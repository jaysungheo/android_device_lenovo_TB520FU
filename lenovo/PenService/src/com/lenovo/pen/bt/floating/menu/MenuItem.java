/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.bt.floating.menu;

import android.content.Context;
import android.view.View;

import com.lenovo.pen.bt.floating.widget.FloatingExpandView2;
import com.lenovo.pen.bt.util.NotesRoleManager;
import com.lenovo.pen.cap.util.Features;

import kotlin.jvm.internal.DefaultConstructorMarker;

/**
 * Replacement of the stock com.lenovo.pen.bt.floating.menu.MenuItem (MenuItem.kt) with
 * the same members. Only updateVisibility() differs: the base implementation is a
 * no-op in stock and MenuSmartDocument / MenuNewNote do not override it, so the
 * toolbox (FloatingMenuRepository.getVisibleActionMenus, the layout handlers) and the
 * toolbox edit page (FloatingMenuViewModel.updateFunctionItems) see them as always
 * shown. Other items hide themselves by overriding it; these two get the same
 * treatment here:
 *  - IDX_SMART_DOCUMENT: its action is a PEN_OPERATION broadcast (type 1) nobody
 *    handles on this ROM, so it is always hidden.
 *  - IDX_NEW_NOTE: hidden unless ZUI Notes is installed or an app holds the Notes
 *    role (the same two conditions MenuNewNote.clickNewNote / updateEnabled use).
 *
 * Nested classes must not touch private members of other classes (d8 --no-desugaring),
 * hence the public access$ accessor used by Companion.
 */
public abstract class MenuItem {
    public static final Companion Companion = new Companion((DefaultConstructorMarker) null);
    public static final String IDX_AI_STYLUS = "IDX_AI_STYLUS";
    private static final String[] IDX_ARRAY = {
        "IDX_INK_MODE", "IDX_EASY_JOT", "IDX_SMART_HUNTER", "IDX_CIRCLE_TO_SEARCH",
        "IDX_AI_STYLUS", "IDX_SKETCH_TO_IMAGE", "IDX_SCREENSHOT", "IDX_NEW_NOTE",
        "IDX_SMART_DOCUMENT", "IDX_DEMO_PEN", "IDX_MAGNIFIER", "IDX_REMOTE_CONTROL",
        "IDX_SETTINGS", "IDX_SWITCH", "IDX_EMPTY", "IDX_EMPTY_DEFAULT",
    };
    public static final String IDX_CIRCLE_TO_SEARCH = "IDX_CIRCLE_TO_SEARCH";
    public static final String IDX_DEMO_PEN = "IDX_DEMO_PEN";
    public static final String IDX_EASY_JOT = "IDX_EASY_JOT";
    public static final String IDX_EMPTY = "IDX_EMPTY";
    public static final String IDX_EMPTY_DEFAULT = "IDX_EMPTY_DEFAULT";
    public static final String IDX_EMPTY_TARGET = "IDX_EMPTY_TARGET";
    public static final String IDX_INK_MODE = "IDX_INK_MODE";
    public static final String IDX_MAGNIFIER = "IDX_MAGNIFIER";
    public static final String IDX_NEW_NOTE = "IDX_NEW_NOTE";
    public static final String IDX_REMOTE_CONTROL = "IDX_REMOTE_CONTROL";
    public static final String IDX_SCREENSHOT = "IDX_SCREENSHOT";
    public static final String IDX_SETTINGS = "IDX_SETTINGS";
    public static final String IDX_SKETCH_TO_IMAGE = "IDX_SKETCH_TO_IMAGE";
    public static final String IDX_SMART_DOCUMENT = "IDX_SMART_DOCUMENT";
    public static final String IDX_SMART_HUNTER = "IDX_SMART_HUNTER";
    public static final String IDX_SWITCH = "IDX_SWITCH";
    public static final int TYPE_EXPAND_APP = 3;
    public static final int TYPE_EXPAND_CHECK = 2;
    public static final int TYPE_EXPAND_ITEM = 1;

    private boolean enabled;
    private final String idx;
    private final int type;
    private int visibility;

    public MenuItem(String idx, int type, int visibility, boolean enabled) {
        this.idx = java.util.Objects.requireNonNull(idx);
        this.type = type;
        this.visibility = visibility;
        this.enabled = enabled;
    }

    /** Kotlin default-arguments constructor. */
    public MenuItem(String idx, int type, int visibility, boolean enabled, int mask,
            DefaultConstructorMarker marker) {
        this(idx,
                (mask & 2) != 0 ? 3 : type,
                (mask & 4) != 0 ? 0 : visibility,
                (mask & 8) != 0 ? true : enabled);
    }

    public static final String[] access$getIDX_ARRAY$cp() {
        return IDX_ARRAY;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public final String getIdx() {
        return idx;
    }

    public final int getType() {
        return type;
    }

    public int getVisibility() {
        return visibility;
    }

    public void onClick(View view, FloatingExpandView2 expandView) {
        java.util.Objects.requireNonNull(view);
        java.util.Objects.requireNonNull(expandView);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public abstract View toMenuItemArcView(Context context, FloatingExpandView2 expandView,
            View.OnHoverListener hoverListener);

    public abstract View toMenuItemView(Context context, FloatingExpandView2 expandView,
            View.OnHoverListener hoverListener);

    public void updateEnabled(Context context) {
        java.util.Objects.requireNonNull(context);
    }

    public void updateVisibility(Context context) {
        java.util.Objects.requireNonNull(context);
        if (IDX_SMART_DOCUMENT.equals(idx)) {
            visibility = View.GONE;
        } else if (IDX_NEW_NOTE.equals(idx)) {
            visibility = hasNotesApp(context) ? View.VISIBLE : View.GONE;
        }
    }

    private static boolean hasNotesApp(Context context) {
        if (!Features.isNotSupportZuiNotes(context)) {
            return true;
        }
        String pkg = NotesRoleManager.getDefaultNotesPackageName(context);
        return pkg != null && !pkg.isEmpty();
    }

    public static final class Companion {
        public Companion(DefaultConstructorMarker marker) {}

        public final String[] getIDX_ARRAY() {
            return MenuItem.access$getIDX_ARRAY$cp();
        }
    }
}
