/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.recyclerview.widget;

import android.view.View;

/** Compile-time stand-in; the real class comes from the PenService dex. */
public class RecyclerView {
    public static class ViewHolder {
        public final View itemView;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }
    }
}
