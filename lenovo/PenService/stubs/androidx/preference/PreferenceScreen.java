/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.preference;

import android.content.Context;

/** Compile-time stand-in; the real class comes from the PenService dex. */
public final class PreferenceScreen extends PreferenceGroup {
    public PreferenceScreen(Context context) {
        super(context);
    }
}
