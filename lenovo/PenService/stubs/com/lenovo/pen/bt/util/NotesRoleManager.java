/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.bt.util;

import android.content.Context;

/** Compile-time stand-in; the real class comes from PenService's own dex. */
public final class NotesRoleManager {
    private NotesRoleManager() {}

    public static String getDefaultNotesPackageName(Context context) {
        return null;
    }
}
