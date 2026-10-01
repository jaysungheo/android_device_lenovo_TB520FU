/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.tb520fu.input;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

/**
 * Folio case mode, like the stock ZUI "Folio case mode" switch: closing the
 * folio cover turns the screen off, opening it turns the screen back on.
 *
 * The hall sensor ("hall_irq") is no SW_LID switch here: it reports the stock
 * ZUI scan codes 750 (cover away) and 751 (cover close), which the stock
 * ZuiPhoneWindowManager turned into notifyLidSwitchChanged(). Without a key
 * layout they arrive as KEYCODE_UNKNOWN with that scan code, so they are
 * matched by device name and scan code and consumed.
 *
 * Stock setting: Settings.System zui_lid_enable, unset means on.
 */
final class FolioCover {
    private static final String TAG = "TB520FUFolio";

    static final String SETTING = "zui_lid_enable";
    private static final String DEVICE = "hall_irq";
    private static final int SCAN_OPEN = 750;
    private static final int SCAN_CLOSE = 751;

    private final Context mContext;
    private final Handler mHandler;

    FolioCover(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    /** Called on the input policy thread; must stay cheap. */
    boolean handle(KeyEvent event) {
        int scan = event.getScanCode();
        if (scan != SCAN_OPEN && scan != SCAN_CLOSE) return false;
        InputDevice device = event.getDevice();
        if (device == null || !DEVICE.equals(device.getName())) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            boolean closed = scan == SCAN_CLOSE;
            Safe.post(mHandler, "folio cover", () -> onCover(closed));
        }
        return true;
    }

    private void onCover(boolean closed) {
        if (Settings.System.getIntForUser(mContext.getContentResolver(), SETTING, 1,
                UserHandle.USER_CURRENT) == 0) {
            Log.d(TAG, "folio case mode off, ignoring cover " + (closed ? "close" : "open"));
            return;
        }
        PowerManager pm = mContext.getSystemService(PowerManager.class);
        long now = SystemClock.uptimeMillis();
        if (closed) {
            if (pm.isInteractive()) {
                pm.goToSleep(now, PowerManager.GO_TO_SLEEP_REASON_LID_SWITCH, 0);
                // Stock: "turn the screen off and lock automatically".
                try {
                    WindowManagerGlobal.getWindowManagerService().lockNow(null);
                } catch (RemoteException e) {
                    Log.w(TAG, "lockNow failed", e);
                }
            }
        } else if (!pm.isInteractive()) {
            pm.wakeUp(now, PowerManager.WAKE_REASON_LID, "tb520fu:folio");
        }
    }
}
