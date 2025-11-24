package moe.chensi.volume.volumemanager

import android.content.Context

object PreferenceHelper {
    const val PREF_SHOW_VOLUME_OVERLAY = "pref_show_volume_overlay"

    fun isVolumeOverlayEnabled(context: Context): Boolean {
    // Default shared prefs name used by PreferenceFragmentCompat is usually: // "${context.packageName}_preferences"
    val prefsName = "${context.packageName}_preferences"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SHOW_VOLUME_OVERLAY, true)
    }
}