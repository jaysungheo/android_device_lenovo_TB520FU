/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.handwriting;

import android.app.ActivityThread;

import com.zui.input.handwriting.ZuiHandWritingManager;

import kotlin.jvm.functions.Function0;

/**
 * Replaces the stock D8 synthetic lazy initializer of EngineHelper.mZuiHandWritingManager,
 * which did EngineHelper.app.getSystemService("zui_hand_writing"); see
 * InkHandle$$ExternalSyntheticLambda0. EngineHelper.app is the process Application.
 */
public final class EngineHelper$$ExternalSyntheticLambda3 implements Function0<Object> {
    public EngineHelper$$ExternalSyntheticLambda3() {}

    @Override
    public Object invoke() {
        return ZuiHandWritingManager.getInstance(ActivityThread.currentApplication());
    }
}
