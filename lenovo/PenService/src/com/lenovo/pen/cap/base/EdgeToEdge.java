/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.cap.base;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Insets;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/**
 * Lets the pen pages scroll under the navigation bar like the Settings pages:
 * the window draws behind the system bars, the page content keeps the status
 * bar and side insets, and only the list gets the navigation bar as bottom
 * padding (not clipped), so its last rows scroll up from under the bar
 * instead of stopping at a band of page background above it.
 */
final class EdgeToEdge implements View.OnApplyWindowInsetsListener {
    final View mList;
    final int mBottomPadding;

    EdgeToEdge(View list) {
        mList = list;
        mBottomPadding = list.getPaddingBottom();
    }

    static void apply(View list) {
        Activity activity = activity(list.getContext());
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        View content = window.findViewById(android.R.id.content);
        if (content == null || content.getTag(android.R.id.content) != null) {
            return;
        }
        content.setTag(android.R.id.content, Boolean.TRUE);
        window.setDecorFitsSystemWindows(false);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        content.setOnApplyWindowInsetsListener(new EdgeToEdge(list));
        content.requestApplyInsets();
    }

    @Override
    public WindowInsets onApplyWindowInsets(View content, WindowInsets insets) {
        Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                | WindowInsets.Type.displayCutout());
        content.setPadding(bars.left, bars.top, bars.right, 0);
        if (mList instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) mList).setClipToPadding(false);
        }
        mList.setPadding(mList.getPaddingLeft(), mList.getPaddingTop(),
                mList.getPaddingRight(), mBottomPadding + bars.bottom);
        return WindowInsets.CONSUMED;
    }

    static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
