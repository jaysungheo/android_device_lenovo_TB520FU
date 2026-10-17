/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.window;

import android.content.Context;

import com.zui.input.handwriting.ZuiHandWritingManager;

import kotlin.jvm.functions.Function0;

/**
 * Replaces the stock D8 synthetic lazy initializer of InkHandle.mZuiHandWritingManager,
 * which did context.getSystemService("zui_hand_writing") as ZuiHandWritingManager.
 * Nothing registers that service on this ROM, so the cast threw/returned null and
 * InkHandle$createView$1.onViewDetachedFromWindow dereferenced it. Same members
 * as stock (f$0, Context constructor, invoke()).
 */
public final class InkHandle$$ExternalSyntheticLambda0 implements Function0<Object> {
    public final Context f$0;

    public InkHandle$$ExternalSyntheticLambda0(Context context) {
        this.f$0 = context;
    }

    @Override
    public Object invoke() {
        return ZuiHandWritingManager.getInstance(this.f$0);
    }
}
