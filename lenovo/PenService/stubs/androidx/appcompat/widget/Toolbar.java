/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.appcompat.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

/** Compile-time stand-in; the real class comes from the PenService dex. */
public class Toolbar extends ViewGroup {
    public Toolbar(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {}

    public Drawable getNavigationIcon() {
        throw new RuntimeException("stub");
    }

    public void setNavigationIcon(Drawable icon) {}

    public void setTitleMarginStart(int margin) {}

    public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {}
}
