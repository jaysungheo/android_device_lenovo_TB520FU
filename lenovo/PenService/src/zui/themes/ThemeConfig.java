/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package zui.themes;

/**
 * The ZUI theme configuration the stock floating pen window asks for the
 * theme font when the configuration changes. PixelOS has no ZUI theme fonts,
 * so there is never one to follow.
 */
public final class ThemeConfig {
    private ThemeConfig() {}

    public static String getThemeFontName() {
        return "";
    }
}
