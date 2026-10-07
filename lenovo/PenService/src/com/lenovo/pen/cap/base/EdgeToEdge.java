/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.cap.base;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.AbsListView;
import android.widget.ScrollView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lets a pen settings page scroll under the navigation bar like the Settings
 * pages: the window draws behind the system bars on the page background (so
 * the status bar has the page colour too), the page content keeps the status
 * bar and side insets, and the scrolling lists get the navigation bar as
 * bottom padding (not clipped), so their last rows scroll up from under the
 * bar instead of stopping at a band of page background above it.
 *
 * Applied to the Settings style pages only (the ones with the ZUI toolbar),
 * see PageStyler; once per window.
 */
final class EdgeToEdge implements View.OnApplyWindowInsetsListener {
    // Windows already set up, by their content view. Not a View tag: the key
    // of setTag(int, Object) has to be an id of the application's own package
    // ("The key must be an application-specific resource id", View.java),
    // and android.R.id.content, the only id known here, is a framework one.
    private static final Map<View, Boolean> sApplied = new WeakHashMap<>();

    final ArrayList<View> mLists = new ArrayList<>();
    final ArrayList<Integer> mBottomPaddings = new ArrayList<>();

    static void apply(Activity activity) {
        Window window = activity.getWindow();
        View content = window.findViewById(android.R.id.content);
        if (content == null || sApplied.containsKey(content)) {
            return;
        }
        sApplied.put(content, Boolean.TRUE);
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        window.getDecorView().setBackgroundColor(activity.getColor(night
                ? android.R.color.system_surface_container_dark
                : android.R.color.system_surface_container_light));
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        content.setOnApplyWindowInsetsListener(new EdgeToEdge());
        content.requestApplyInsets();
    }

    @Override
    public WindowInsets onApplyWindowInsets(View content, WindowInsets insets) {
        Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                | WindowInsets.Type.displayCutout());
        content.setPadding(bars.left, bars.top, bars.right, 0);
        // Lists can be added later (fragments), so look again every time.
        collect(content);
        for (int i = 0; i < mLists.size(); i++) {
            View list = mLists.get(i);
            if (list instanceof ViewGroup) {
                ((ViewGroup) list).setClipToPadding(false);
            }
            list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                    list.getPaddingRight(), mBottomPaddings.get(i) + bars.bottom);
        }
        return WindowInsets.CONSUMED;
    }

    void collect(View view) {
        if (view instanceof RecyclerView || view instanceof AbsListView
                || view instanceof ScrollView
                || "androidx.core.widget.NestedScrollView".equals(view.getClass().getName())) {
            if (!mLists.contains(view)) {
                mLists.add(view);
                mBottomPaddings.add(view.getPaddingBottom());
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i));
            }
        }
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
