/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.cap.base;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

/**
 * Gives the ZUI toolbar of the pen pages the SettingsLib expressive look: the
 * back button as a 40dp circle (surface container highest) 24dp from the
 * edge, the title in variable-title-large-emphasized (the size stays the
 * stock 22sp) 28dp after it.
 *
 * ZuiAppcompatToolbar clears the back button background once on its first
 * layout, so the look is applied after that layout and again whenever the
 * toolbar lays out with another background on the button (cheap check).
 */
final class PixelToolbar implements Runnable, View.OnLayoutChangeListener {
    static final String TITLE_FONT = "variable-title-large-emphasized";

    final Toolbar mToolbar;
    Drawable mArrow;
    Drawable mBackground;

    PixelToolbar(Toolbar toolbar) {
        mToolbar = toolbar;
    }

    /** Styles the page toolbar (id "toolbar") of the window [anchor] is in, once. */
    static void attach(View anchor) {
        View root = anchor.getRootView();
        Context context = anchor.getContext();
        int id = context.getResources().getIdentifier("toolbar", "id", context.getPackageName());
        View view = id == 0 ? null : root.findViewById(id);
        if (!(view instanceof Toolbar) || view.getTag(id) != null) {
            return;
        }
        view.setTag(id, Boolean.TRUE);
        PixelToolbar styler = new PixelToolbar((Toolbar) view);
        view.addOnLayoutChangeListener(styler);
        view.post(styler);
    }

    @Override
    public void run() {
        apply();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {
        ImageButton back = backButton();
        if (back != null && mBackground != null && back.getBackground() != mBackground) {
            v.post(this);
        }
    }

    void apply() {
        Context context = mToolbar.getContext();
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;
        boolean night = (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        if (mArrow == null) {
            int arrowId = Resources.getSystem().getIdentifier("ic_ab_back_material", "drawable",
                    "android");
            if (arrowId == 0 || mToolbar.getNavigationIcon() == null) {
                return;
            }
            mArrow = context.getDrawable(arrowId).mutate();
            mArrow.setTint(context.getColor(night
                    ? android.R.color.system_on_surface_variant_dark
                    : android.R.color.system_on_surface_variant_light));
            mArrow.setAutoMirrored(true);
            mToolbar.setNavigationIcon(mArrow);
            mBackground = circle(context, night);
        }

        ImageButton back = backButton();
        if (back != null) {
            int size = Math.round(40 * density);
            ViewGroup.LayoutParams lp = back.getLayoutParams();
            boolean changed = lp.width != size || lp.height != size;
            lp.width = size;
            lp.height = size;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMarginStart(
                        Math.round(24 * density) - mToolbar.getPaddingStart());
            }
            if (changed) {
                back.setLayoutParams(lp);
            }
            back.setMinimumWidth(0);
            back.setMinimumHeight(0);
            back.setPadding(0, 0, 0, 0);
            back.setScaleType(ImageView.ScaleType.CENTER);
            back.setBackground(mBackground);
        }
        mToolbar.setContentInsetStartWithNavigation(0);
        mToolbar.setTitleMarginStart(Math.round(28 * density));

        Typeface font = Typeface.create(TITLE_FONT, Typeface.NORMAL);
        for (int i = 0; i < mToolbar.getChildCount(); i++) {
            View child = mToolbar.getChildAt(i);
            if (child instanceof TextView) {
                TextView title = (TextView) child;
                if (title.getTypeface() != font) {
                    title.setTypeface(font);
                }
                title.setTextColor(context.getColor(night
                        ? android.R.color.system_on_surface_dark
                        : android.R.color.system_on_surface_light));
            }
        }
    }

    ImageButton backButton() {
        for (int i = 0; i < mToolbar.getChildCount(); i++) {
            View child = mToolbar.getChildAt(i);
            if (child instanceof ImageButton
                    && ((ImageButton) child).getDrawable() == mToolbar.getNavigationIcon()) {
                return (ImageButton) child;
            }
        }
        return null;
    }

    /** SettingsLib expressive back button: a filled circle with a ripple. */
    static Drawable circle(Context context, boolean night) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(context.getColor(night
                ? android.R.color.system_surface_container_highest_dark
                : android.R.color.system_surface_container_highest_light));
        TypedArray a = context.obtainStyledAttributes(
                new int[] { android.R.attr.colorControlHighlight });
        int ripple = a.getColor(0, 0x1f000000);
        a.recycle();
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(ripple), circle, mask);
    }
}
