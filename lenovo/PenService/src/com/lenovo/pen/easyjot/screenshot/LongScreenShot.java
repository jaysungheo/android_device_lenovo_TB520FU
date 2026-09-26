/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.easyjot.screenshot;

import android.content.Context;
import android.util.Log;

/**
 * Replaces PenService's long (scrolling) screenshot helper, which is built on
 * the ZUI-only android.app.ILongScreenshotListener. Reports the feature as
 * unavailable so EasyJot hides it instead of crashing.
 */
public final class LongScreenShot {
    private static final String TAG = "PenServiceCompat";

    public static final Companion Companion = new Companion();

    private final Context mContext;
    private final OnLongScreenshotListener mOnLongScreenshotListener;

    public LongScreenShot(Context context, OnLongScreenshotListener listener) {
        mContext = context;
        mOnLongScreenshotListener = listener;
    }

    public static OnLongScreenshotListener access$getMOnLongScreenshotListener$p(LongScreenShot s) {
        return s.mOnLongScreenshotListener;
    }

    public boolean checkLongScreenshotAbility() {
        if (mOnLongScreenshotListener != null) mOnLongScreenshotListener.onLongScreenshotAbility(0);
        return false;
    }

    public boolean startLongScreenShot() {
        Log.i(TAG, "long screenshot not supported");
        if (mOnLongScreenshotListener != null) mOnLongScreenshotListener.onScreenShotFail();
        return false;
    }

    public void continueLongScreenShot() {
        if (mOnLongScreenshotListener != null) mOnLongScreenshotListener.onScreenShotFail();
    }

    public void stopLongScreenShot() {}

    public interface OnLongScreenshotListener {
        void onLongScreenshotAbility(int ability);

        void onScreenShotFail();

        void onScreenShotUpdate(boolean isEnd, int topLoc, int bottomLoc, int viewTop, int viewBottom);
    }

    public static final class Companion {
        private Companion() {}
    }
}
