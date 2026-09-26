/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.tb520fu.parts

import android.content.Intent
import android.hardware.input.InputManager
import android.hardware.input.InputSettings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment

/**
 * Physical keyboard page like the stock Lenovo one (ZuiSettings
 * PhysicalKeyboardFragment). Lenovo features use the stock settings keys
 * (LenovoKeyboard); the rest are the AOSP input settings.
 */
class KeyboardFragment : SettingsBasePreferenceFragment(), InputManager.InputDeviceListener {

    private lateinit var firmwareCategory: PreferenceCategory
    private lateinit var firmwarePref: Preference
    private lateinit var app1Pref: Preference
    private lateinit var app2Pref: Preference
    private lateinit var switches: List<Pair<SwitchPreferenceCompat, () -> Boolean>>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.keyboard_settings, rootKey)
        val ctx = requireContext()

        findPreference<Preference>(KEY_SHORTCUTS_HELPER)!!.setOnPreferenceClickListener {
            ctx.sendBroadcastAsUser(
                Intent(Intent.ACTION_SHOW_KEYBOARD_SHORTCUTS).setPackage(SYSTEMUI),
                UserHandle.CURRENT,
            )
            true
        }
        // Modifier keys, repeat keys and keyboard accessibility live on the
        // AOSP physical keyboard page.
        val aospKeyboard = Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS)
        listOf(KEY_MODIFIER_KEYS, KEY_REPEAT_KEYS, KEY_A11Y).forEach {
            findPreference<Preference>(it)!!.intent = aospKeyboard
        }

        switches = listOf(
            systemSwitch(KEY_AUTO_BACKLIGHT, LenovoKeyboard.AUTO_BACKLIGHT, 0),
            systemSwitch(KEY_SINGLE_FINGER_TAP, LenovoKeyboard.SINGLE_FINGER_TAP, 1),
            systemSwitch(KEY_TAP_WAKE, LenovoKeyboard.TAP_WAKE, 1),
            switch(KEY_STICKY_KEYS, { InputSettings.isAccessibilityStickyKeysEnabled(ctx) }) {
                InputSettings.setAccessibilityStickyKeysEnabled(ctx, it)
            },
            switch(KEY_BOUNCE_KEYS, { InputSettings.isAccessibilityBounceKeysEnabled(ctx) }) {
                InputSettings.setAccessibilityBounceKeysThreshold(ctx, if (it) KEYS_THRESHOLD_MS else 0)
            },
            switch(KEY_SLOW_KEYS, { InputSettings.isAccessibilitySlowKeysEnabled(ctx) }) {
                InputSettings.setAccessibilitySlowKeysThreshold(ctx, if (it) KEYS_THRESHOLD_MS else 0)
            },
            switch(KEY_MOUSE_KEYS, { InputSettings.isAccessibilityMouseKeysEnabled(ctx) }) {
                InputSettings.setAccessibilityMouseKeysEnabled(ctx, it)
            },
            switch(KEY_RIGHT_CLICK_ZONE, { InputSettings.useTouchpadRightClickZone(ctx) }) {
                InputSettings.setTouchpadRightClickZone(ctx, it)
            },
            switch(KEY_REVERSE_SCROLLING, { InputSettings.useTouchpadNaturalScrolling(ctx) }) {
                InputSettings.setTouchpadNaturalScrolling(ctx, it)
            },
        )

        findPreference<SeekBarPreference>(KEY_POINTER_SPEED)!!.apply {
            min = InputSettings.MIN_POINTER_SPEED
            max = InputSettings.MAX_POINTER_SPEED
            value = InputSettings.getTouchpadPointerSpeed(ctx)
            setOnPreferenceChangeListener { _, value ->
                InputSettings.setTouchpadPointerSpeed(requireContext(), value as Int)
                true
            }
        }

        app1Pref = appKey(KEY_APP1, 1)
        app2Pref = appKey(KEY_APP2, 2)

        firmwareCategory = findPreference(KEY_FIRMWARE_CATEGORY)!!
        firmwarePref = findPreference(KEY_FIRMWARE)!!
    }

    private fun switch(
        key: String,
        get: () -> Boolean,
        set: (Boolean) -> Unit,
    ): Pair<SwitchPreferenceCompat, () -> Boolean> {
        val pref: SwitchPreferenceCompat = findPreference(key)!!
        pref.setOnPreferenceChangeListener { _, value ->
            set(value as Boolean)
            true
        }
        return pref to get
    }

    /** A switch backed by a stock Lenovo Settings.System int. */
    private fun systemSwitch(key: String, setting: String, def: Int) =
        switch(key, { LenovoKeyboard.getInt(requireContext(), setting, def) != 0 }) {
            LenovoKeyboard.putInt(requireContext(), setting, if (it) 1 else 0)
        }

    private fun appKey(key: String, index: Int): Preference =
        findPreference<Preference>(key)!!.apply {
            setOnPreferenceClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(
                        com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                        KeyboardAppKeyFragment.newInstance(index),
                    )
                    .addToBackStack(null)
                    .commit()
                true
            }
        }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.lkb_physical_keyboard_title)
        val ctx = requireContext()
        switches.forEach { (pref, get) -> pref.isChecked = get() }
        app1Pref.summary = KeyboardAppKeyFragment.describe(ctx, 1)
        app2Pref.summary = KeyboardAppKeyFragment.describe(ctx, 2)
        ctx.getSystemService(InputManager::class.java)
            .registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
        updateFirmwareUpdate()
    }

    override fun onPause() {
        super.onPause()
        requireContext().getSystemService(InputManager::class.java)
            .unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) = updateFirmwareUpdate()

    override fun onInputDeviceRemoved(deviceId: Int) = updateFirmwareUpdate()

    override fun onInputDeviceChanged(deviceId: Int) = updateFirmwareUpdate()

    /** Stock: the firmware update entry is shown only with a Lenovo keyboard connected. */
    private fun updateFirmwareUpdate() {
        val intent = LenovoKeyboard.firmwareUpdateIntent(requireContext())
        firmwarePref.intent = intent
        firmwareCategory.isVisible = intent != null
    }

    private companion object {
        const val SYSTEMUI = "com.android.systemui"
        const val KEYS_THRESHOLD_MS = 500

        const val KEY_SHORTCUTS_HELPER = "keyboard_shortcuts_helper"
        const val KEY_MODIFIER_KEYS = "modifier_keys_settings"
        const val KEY_AUTO_BACKLIGHT = "keyboard_auto_backlight"
        const val KEY_REPEAT_KEYS = "physical_keyboard_repeat_keys"
        const val KEY_A11Y = "physical_keyboard_a11y"
        const val KEY_STICKY_KEYS = "accessibility_sticky_keys"
        const val KEY_BOUNCE_KEYS = "accessibility_bounce_keys"
        const val KEY_SLOW_KEYS = "accessibility_slow_keys"
        const val KEY_MOUSE_KEYS = "accessibility_mouse_keys"
        const val KEY_SINGLE_FINGER_TAP = "physical_keyboard_single_finger_touch"
        const val KEY_TAP_WAKE = "keyboard_touch_bright_screen"
        const val KEY_RIGHT_CLICK_ZONE = "trackpad_bottom_right_tap"
        const val KEY_REVERSE_SCROLLING = "trackpad_reverse_scrolling"
        const val KEY_POINTER_SPEED = "touchpad_pointer_speed"
        const val KEY_APP1 = "key_shortcut_app1"
        const val KEY_APP2 = "key_shortcut_app2"
        const val KEY_FIRMWARE_CATEGORY = "firmware_update_category"
        const val KEY_FIRMWARE = "keyboard_firmware_update"
    }
}
