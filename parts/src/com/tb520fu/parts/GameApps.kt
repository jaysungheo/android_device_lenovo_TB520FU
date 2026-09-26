/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.tb520fu.parts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Per-app game performance levels, stored for input/.../GamePerfController.java
 * as Settings.Global tb520fu_game_perf_apps = "pkg:cpu:gpu;pkg:cpu:gpu".
 * Levels: 0 power saving, 1 balanced (sustained), 2 high performance.
 */
object GameApps {
    const val LEVEL_POWER_SAVING = 0
    const val LEVEL_BALANCED = 1
    const val LEVEL_PERFORMANCE = 2
    val LEVELS = intArrayOf(LEVEL_POWER_SAVING, LEVEL_BALANCED, LEVEL_PERFORMANCE)

    data class Levels(val cpu: Int, val gpu: Int)

    /** Package name to levels, in the order the apps were added. */
    fun load(ctx: Context): LinkedHashMap<String, Levels> {
        val map = LinkedHashMap<String, Levels>()
        LenovoSettings.getString(ctx, LenovoSettings.GAME_PERF_APPS, "")
            .split(';')
            .forEach { entry ->
                val f = entry.split(':')
                if (f.size != 3 || f[0].isEmpty()) return@forEach
                val cpu = f[1].toIntOrNull()?.coerceIn(0, 2) ?: return@forEach
                val gpu = f[2].toIntOrNull()?.coerceIn(0, 2) ?: return@forEach
                map[f[0]] = Levels(cpu, gpu)
            }
        return map
    }

    /** Saves the list; an empty list deletes the setting instead of leaving an empty value. */
    fun save(ctx: Context, apps: Map<String, Levels>) {
        if (apps.isEmpty()) {
            ctx.contentResolver.call(
                Settings.Global.CONTENT_URI,
                Settings.CALL_METHOD_DELETE_GLOBAL,
                LenovoSettings.GAME_PERF_APPS,
                null,
            )
        } else {
            LenovoSettings.putString(
                ctx,
                LenovoSettings.GAME_PERF_APPS,
                apps.entries.joinToString(";") { "${it.key}:${it.value.cpu}:${it.value.gpu}" },
            )
        }
    }

    fun get(ctx: Context, pkg: String) = load(ctx)[pkg]

    fun set(ctx: Context, pkg: String, levels: Levels) =
        save(ctx, load(ctx).apply { put(pkg, levels) })

    fun remove(ctx: Context, pkg: String) =
        save(ctx, load(ctx).apply { remove(pkg) })

    /** Drops apps that are no longer installed. */
    fun prune(ctx: Context) {
        val apps = load(ctx)
        val pm = ctx.packageManager
        val installed = apps.filterKeys { pkg ->
            runCatching { pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0)) }
                .isSuccess
        }
        if (installed.size != apps.size) save(ctx, installed)
    }

    fun levelName(ctx: Context, level: Int): String =
        ctx.resources.getStringArray(R.array.game_level_entries)[level.coerceIn(0, 2)]

    /** "CPU Balanced · GPU High performance" */
    fun describe(ctx: Context, levels: Levels): String =
        ctx.getString(
            R.string.game_app_levels,
            levelName(ctx, levels.cpu),
            levelName(ctx, levels.gpu),
        )

    fun label(ctx: Context, pkg: String): CharSequence =
        runCatching { ctx.packageManager.getApplicationInfo(pkg, 0).loadLabel(ctx.packageManager) }
            .getOrDefault(pkg)
}
