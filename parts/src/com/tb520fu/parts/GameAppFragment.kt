/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.tb520fu.parts

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

/** CPU and GPU level of one app in the game performance list, or remove it. */
class GameAppFragment : SettingsBasePreferenceFragment() {

    private val pkg get() = requireArguments().getString(ARG_PKG)!!
    private val cpuPrefs = mutableListOf<SelectorWithWidgetPreference>()
    private val gpuPrefs = mutableListOf<SelectorWithWidgetPreference>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val ctx = requireContext()
        preferenceScreen = preferenceManager.createPreferenceScreen(ctx)

        addLevels(R.string.game_cpu_category, "cpu", R.array.game_cpu_level_summaries, cpuPrefs) {
            levels, level -> levels.copy(cpu = level)
        }
        addLevels(R.string.game_gpu_category, "gpu", R.array.game_gpu_level_summaries, gpuPrefs) {
            levels, level -> levels.copy(gpu = level)
        }

        val other = PreferenceCategory(ctx).apply { key = "other" }
        preferenceScreen.addPreference(other)
        other.addPreference(Preference(ctx).apply {
            key = "remove"
            title = getString(R.string.game_app_remove)
            setOnPreferenceClickListener {
                GameApps.remove(requireContext(), pkg)
                parentFragmentManager.popBackStack()
                true
            }
        })
    }

    private fun addLevels(
        titleRes: Int,
        prefix: String,
        summariesRes: Int,
        prefs: MutableList<SelectorWithWidgetPreference>,
        change: (GameApps.Levels, Int) -> GameApps.Levels,
    ) {
        val ctx = requireContext()
        val summaries = resources.getStringArray(summariesRes)
        val category = PreferenceCategory(ctx).apply {
            key = prefix
            title = getString(titleRes)
        }
        preferenceScreen.addPreference(category)
        GameApps.LEVELS.forEach { level ->
            val pref = SelectorWithWidgetPreference(ctx).apply {
                key = "${prefix}_$level"
                title = GameApps.levelName(ctx, level)
                summary = summaries[level]
                isPersistent = false
                setOnClickListener {
                    val levels = GameApps.get(requireContext(), pkg) ?: return@setOnClickListener
                    GameApps.set(requireContext(), pkg, change(levels, level))
                    updateChecked()
                }
            }
            category.addPreference(pref)
            prefs += pref
        }
    }

    override fun onResume() {
        super.onResume()
        if (GameApps.get(requireContext(), pkg) == null) {
            parentFragmentManager.popBackStack()
            return
        }
        activity?.title = GameApps.label(requireContext(), pkg)
        updateChecked()
    }

    private fun updateChecked() {
        val levels = GameApps.get(requireContext(), pkg) ?: return
        cpuPrefs.forEachIndexed { level, pref -> pref.isChecked = level == levels.cpu }
        gpuPrefs.forEachIndexed { level, pref -> pref.isChecked = level == levels.gpu }
    }

    companion object {
        private const val ARG_PKG = "package"

        fun newInstance(pkg: String) = GameAppFragment().apply {
            arguments = Bundle().apply { putString(ARG_PKG, pkg) }
        }
    }
}
