/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.tb520fu.input;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Per-app performance profiles, similar to the PRC game assistant (ZuiGameHelper)
 * performance modes. While a listed app is in the foreground its profile limits
 * or raises the CPU/GPU clocks; everything is restored when another app comes to
 * the front. Optionally background apps are killed when a listed app starts
 * (the game assistant's memory cleanup).
 *
 * The limits are written straight to cpufreq (system writable) and the kgsl
 * clock limits (made system writable in init.target.rc). Thermal throttling in
 * the kernel keeps working on top of these limits.
 *
 * CPU and GPU each have their own level per app: 0 power saving,
 * 1 balanced (sustained), 2 high performance.
 *
 * Settings.Global (TB520FUParts):
 *   tb520fu_game_perf         1 enable profiles
 *   tb520fu_game_perf_apps    "pkg:cpu:gpu;pkg:cpu:gpu"
 *   tb520fu_game_mem_clean    1 kill background apps when a listed app starts
 *
 * Uninstalled apps are dropped from the list; an empty list deletes the setting.
 */
final class GamePerfController {
    private static final String TAG = "TB520FUGamePerf";

    static final String SETTING_ENABLED = "tb520fu_game_perf";
    static final String SETTING_APPS = "tb520fu_game_perf_apps";
    static final String SETTING_MEM_CLEAN = "tb520fu_game_mem_clean";

    static final int LEVEL_POWER_SAVING = 0;
    static final int LEVEL_BALANCED = 1;
    static final int LEVEL_PERFORMANCE = 2;
    private static final int LEVEL_NONE = -1;

    private static final String CPUFREQ = "/sys/devices/system/cpu/cpufreq/policy";
    private static final String KGSL = "/sys/class/kgsl/kgsl-3d0/";
    // SM8650: policy0 A520 (2.27 GHz), policy2 A720 (3.15), policy5 A720 (2.96), policy7 X4 (3.3)
    private static final int[] POLICIES = {0, 2, 5, 7};

    // kHz caps per policy (0 = no cap), by CPU level. Only the upper limit is
    // changed; the floors set up by the boot scripts stay as they are. Balanced
    // leaves the X4 alone (same single core score) and caps the other clusters
    // to about 85 % of the multi core score; high performance removes every cap.
    private static final int[][] MAX_KHZ = {
        {1804800, 2016000, 2016000, 2169600},   // power saving
        {1804800, 2438400, 2323200, 0},         // balanced / sustained
        {0, 0, 0, 0},                           // high performance
    };
    // GPU clock caps in MHz by GPU level (0 = no cap), applied with max_clock_mhz
    // (max_pwrlevel does not move the devfreq limit). 903 MHz is the top level:
    // 578 MHz is about 64 %, 770 MHz about 85 %.
    private static final int[] GPU_MAX_MHZ = {578, 770, 0};

    private static final long MEM_CLEAN_INTERVAL_MS = 5 * 60 * 1000;

    private final Context mContext;
    private final Handler mHandler;
    /** package -> {cpu level, gpu level} */
    private final Map<String, int[]> mApps = new HashMap<>();
    private final Map<String, Long> mLastClean = new HashMap<>();
    private boolean mEnabled;
    private boolean mMemClean;
    private int mAppliedCpu = LEVEL_NONE;
    private int mAppliedGpu = LEVEL_NONE;
    private String mForeground;
    /** scaling_min/max as set up by the boot scripts, restored when no profile applies. */
    private final int[] mBaseMin = new int[POLICIES.length];
    private final int[] mBaseMax = new int[POLICIES.length];
    private int mGpuBaseMax;

    private final Runnable mCheckForeground = Safe.run("game perf foreground", this::checkForeground);

    GamePerfController(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    void start() {
        ContentResolver cr = mContext.getContentResolver();
        ContentObserver observer = Safe.observer(mHandler, "game perf settings", uri -> {
            readSettings();
            mForeground = null;
            checkForeground();
        });
        for (String key : new String[] {SETTING_ENABLED, SETTING_APPS, SETTING_MEM_CLEAN}) {
            cr.registerContentObserver(Settings.Global.getUriFor(key), false, observer);
        }
        readSettings();
        IntentFilter removed = new IntentFilter(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        removed.addDataScheme("package");
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Safe.run("game perf package removed", () -> {
                    if (intent.getData() != null) forget(intent.getData().getSchemeSpecificPart());
                }).run();
            }
        }, removed, null, mHandler);
        try {
            ActivityTaskManager.getService().registerTaskStackListener(new TaskStackListener() {
                @Override
                public void onTaskStackChanged() {
                    // binder thread: debounce onto our handler
                    mHandler.removeCallbacks(mCheckForeground);
                    mHandler.postDelayed(mCheckForeground, 300);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "registerTaskStackListener", e);
        }
    }

    private void readSettings() {
        ContentResolver cr = mContext.getContentResolver();
        mEnabled = Settings.Global.getInt(cr, SETTING_ENABLED, 0) != 0;
        mMemClean = Settings.Global.getInt(cr, SETTING_MEM_CLEAN, 0) != 0;
        mApps.clear();
        String list = Settings.Global.getString(cr, SETTING_APPS);
        if (!TextUtils.isEmpty(list)) {
            for (String entry : list.split(";")) {
                String[] f = entry.split(":");
                if (f.length != 3 || f[0].isEmpty()) continue;
                try {
                    mApps.put(f[0], new int[] {
                            clampLevel(Integer.parseInt(f[1])), clampLevel(Integer.parseInt(f[2]))});
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (!mEnabled) apply(LEVEL_NONE, LEVEL_NONE);
    }

    /** Drops an uninstalled app from the saved list. */
    private void forget(String pkg) {
        mLastClean.remove(pkg);
        if (mApps.remove(pkg) == null) return;
        StringJoiner list = new StringJoiner(";");
        for (Map.Entry<String, int[]> e : mApps.entrySet()) {
            list.add(e.getKey() + ":" + e.getValue()[0] + ":" + e.getValue()[1]);
        }
        ContentResolver cr = mContext.getContentResolver();
        if (mApps.isEmpty()) {
            cr.call(Settings.Global.CONTENT_URI, Settings.CALL_METHOD_DELETE_GLOBAL,
                    SETTING_APPS, null);
        } else {
            Settings.Global.putString(cr, SETTING_APPS, list.toString());
        }
        Log.i(TAG, "removed uninstalled " + pkg);
    }

    private static int clampLevel(int level) {
        return Math.max(LEVEL_POWER_SAVING, Math.min(LEVEL_PERFORMANCE, level));
    }

    private void checkForeground() {
        String pkg = null;
        try {
            ActivityTaskManager.RootTaskInfo info =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            if (info != null && info.topActivity != null) pkg = info.topActivity.getPackageName();
        } catch (Exception e) {
            Log.w(TAG, "focused task", e);
        }
        if (pkg == null || pkg.equals(mForeground)) return;
        mForeground = pkg;
        int[] levels = mEnabled ? mApps.get(pkg) : null;
        if (levels != null) {
            apply(levels[0], levels[1]);
        } else {
            apply(LEVEL_NONE, LEVEL_NONE);
        }
        if (levels != null && mMemClean) cleanMemory(pkg);
    }

    private void apply(int cpu, int gpu) {
        if (cpu != mAppliedCpu) applyCpu(cpu);
        if (gpu != mAppliedGpu) applyGpu(gpu);
    }

    private void applyCpu(int level) {
        Log.i(TAG, "cpu " + mAppliedCpu + " -> " + level + " for " + mForeground);
        if (mAppliedCpu == LEVEL_NONE) {
            // Leaving the untouched state: remember what the boot scripts set up
            // (read late, so any boot-time boost is long gone).
            for (int i = 0; i < POLICIES.length; i++) {
                mBaseMin[i] = readInt(CPUFREQ + POLICIES[i] + "/scaling_min_freq");
                mBaseMax[i] = readInt(CPUFREQ + POLICIES[i] + "/scaling_max_freq");
            }
        }
        mAppliedCpu = level;
        for (int i = 0; i < POLICIES.length; i++) {
            String base = CPUFREQ + POLICIES[i] + "/";
            int hwMin = readInt(base + "cpuinfo_min_freq");
            int hwMax = mBaseMax[i];
            if (hwMin <= 0 || hwMax <= 0 || mBaseMin[i] <= 0) continue;
            int max = hwMax;
            if (level >= 0 && MAX_KHZ[level][i] > 0) max = Math.min(hwMax, MAX_KHZ[level][i]);
            int min = Math.min(max, mBaseMin[i]);
            // order matters: never let min exceed the current max
            write(base + "scaling_min_freq", hwMin);
            write(base + "scaling_max_freq", max);
            write(base + "scaling_min_freq", min);
        }
    }

    private void applyGpu(int level) {
        Log.i(TAG, "gpu " + mAppliedGpu + " -> " + level + " for " + mForeground);
        if (mAppliedGpu == LEVEL_NONE) {
            // Leaving the untouched state: remember the device limit
            mGpuBaseMax = readInt(KGSL + "max_clock_mhz");
        }
        mAppliedGpu = level;
        if (mGpuBaseMax <= 0) return;
        int max = mGpuBaseMax;
        if (level >= 0 && GPU_MAX_MHZ[level] > 0) max = Math.min(max, GPU_MAX_MHZ[level]);
        write(KGSL + "max_clock_mhz", max);
    }

    /** Game assistant memory cleanup: kill cached/background apps before the game grows. */
    private void cleanMemory(String pkg) {
        long now = SystemClock.elapsedRealtime();
        Long last = mLastClean.get(pkg);
        if (last != null && now - last < MEM_CLEAN_INTERVAL_MS) return;
        mLastClean.put(pkg, now);
        try {
            ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
            ActivityManager am = mContext.getSystemService(ActivityManager.class);
            am.getMemoryInfo(before);
            android.app.ActivityManager.getService().killAllBackgroundProcesses();
            Log.i(TAG, "memory cleanup for " + pkg + ", avail was " + (before.availMem >> 20) + " MB");
        } catch (Exception e) {
            Log.w(TAG, "memory cleanup", e);
        }
    }

    private static int readInt(String path) {
        try {
            return Integer.parseInt(new String(Files.readAllBytes(Paths.get(path)),
                    StandardCharsets.US_ASCII).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private static void write(String path, int value) {
        try (FileWriter w = new FileWriter(path)) {
            w.write(Integer.toString(value));
        } catch (IOException e) {
            Log.w(TAG, "write " + path + " = " + value + ": " + e.getMessage());
        }
    }
}
