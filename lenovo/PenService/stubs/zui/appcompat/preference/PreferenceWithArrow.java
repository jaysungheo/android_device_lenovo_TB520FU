/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package zui.appcompat.preference;

import android.content.Context;

/** Compile-time stand-in; the real class comes from the PenService dex. */
public class PreferenceWithArrow extends Preference {
    public static int LOCATION_BOTTOM = 1;

    public PreferenceWithArrow(Context context) {
        super(context);
    }

    public void setSummaryLocation(int location) {}
}
