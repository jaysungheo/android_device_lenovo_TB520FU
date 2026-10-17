/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.app;

/**
 * Inert stand-in for the ZUI framework class that does not exist in this ROM.
 * Stock com.lenovo.pen.bt.util.ZuiModeHelper constructs it and registers a
 * GameAppStateListener (PenSettingsBt.initCore); without the class the persistent
 * process dies with NoClassDefFoundError. There is no game mode here, so the
 * listener is never called.
 */
public class ZuiModeManager {
    public ZuiModeManager() {}

    public void registerGameAppStateListener(GameAppStateListener listener) {}

    public void unregisterGameAppStateListener(GameAppStateListener listener) {}

    public interface GameAppStateListener {
        void onGameAppExit(String packageName, String activityName);

        void onGameAppStart(String packageName, String activityName);
    }
}
