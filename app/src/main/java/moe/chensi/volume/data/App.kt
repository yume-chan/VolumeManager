package moe.chensi.volume.data

import android.content.pm.PackageInfo
import android.icu.text.Collator
import android.icu.text.Transliterator
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.chensi.volume.system.AudioPlaybackConfigurationProxy
import moe.chensi.volume.system.PackageManagerProxy
import java.util.Locale

data class App(
    val packageManager: PackageManagerProxy,
    val packageInfo: PackageInfo,
    val name: String,
    private var preferences: AppPreferences,
    private val savePreferences: () -> Unit
) {
    companion object {
        val collator: Collator by lazy {
            Collator.getInstance().apply {
                strength = Collator.PRIMARY
                decomposition = Collator.CANONICAL_DECOMPOSITION
            }
        }

        val transliterator: Transliterator by lazy { Transliterator.getInstance("Han-Latin") }

        val defaultComparator: Comparator<App> by lazy {
            compareBy(collator) { it.name }
        }

        val chineseComparator: Comparator<App> by lazy {
            compareBy(Comparator { a, b ->
                for ((aChar, bChar) in a.zip(b)) {
                    if (aChar == bChar) {
                        continue
                    }

                    val aIsChinese =
                        Character.UnicodeScript.of(aChar.code) == Character.UnicodeScript.HAN
                    val bIsChinese =
                        Character.UnicodeScript.of(bChar.code) == Character.UnicodeScript.HAN

                    // Both are Chinese or both are not Chinese
                    if (aIsChinese == bIsChinese) {
                        val result = collator.compare(aChar.toString(), bChar.toString())
                        if (result != 0) {
                            return@Comparator result
                        }
                    }

                    if (aIsChinese) {
                        val transliteration = transliterator.transliterate(aChar.toString())
                        val result = collator.compare(transliteration, bChar.toString())
                        if (result != 0) {
                            return@Comparator result
                        }
                    } else {
                        val transliteration = transliterator.transliterate(bChar.toString())
                        val result = collator.compare(aChar.toString(), transliteration)
                        if (result != 0) {
                            return@Comparator result
                        }
                    }
                }

                return@Comparator a.length - b.length
            }) { it.name }
        }

        val comparator: Comparator<App>
            get() {
                if (Locale.getDefault().language == "zh") {
                    return chineseComparator
                }
                return defaultComparator
            }

        val scope = CoroutineScope(Dispatchers.IO)
    }

    val packageName: String
        get() = packageInfo.packageName

    val hasAnyActivity: Boolean
        get() = packageInfo.activities?.isNotEmpty() == true

    val applicationInfo
        get() = packageInfo.applicationInfo!!

    private var _icon by mutableStateOf<ImageBitmap?>(null)
    private var _iconLoading = false
    val icon: ImageBitmap?
        get() {
            if (!_iconLoading) {
                _iconLoading = true
                scope.launch {
                    _icon = packageManager.getDrawable(
                        packageName,
                        applicationInfo.icon,
                        applicationInfo
                    )?.toBitmap(128, 128)?.asImageBitmap()
                        ?: packageManager.defaultActivityIconImageBitmap
                }
            }

            return _icon
        }

    fun setPreferences(value: AppPreferences) {
        preferences = value

        _volume = preferences.volume
        _hidden = preferences.hidden
        _disableVolumeButtons = preferences.disableVolumeButtons
    }

    private val _players: MutableList<AudioPlaybackConfigurationProxy> = mutableStateListOf()
    val players: List<AudioPlaybackConfigurationProxy> = _players

    fun clearPlayers() {
        _players.clear()
        isPlaying = false
    }

    fun addPlayer(config: AudioPlaybackConfigurationProxy) {
        // Set app as player even if the player is released
        isPlayer = true

        if (!config.hasPlayer) {
            return
        }

        Log.d(
            "AppVolManager",
            "add player $packageName ${config.clientPid} ${config.playerTypeName} ${config.playerStateName}"
        )

        // Apply volume to potentially new player
        if (!config.setVolume(_volume)) {
            // Player is dead, don't add it
            return
        }

        _players.add(config)

        if (config.isPlaying) {
            isPlaying = true
        }
    }

    fun applyVolume(value: Float) {
        val deadPlayers = mutableListOf<AudioPlaybackConfigurationProxy>()
        for (player in players) {
            if (!player.setVolume(value)) {
                deadPlayers.add(player)
            }
        }
        // Remove dead players
        _players.removeAll(deadPlayers)
    }

    private var _isPlayer by mutableStateOf(preferences.isPlayer)
    var isPlayer: Boolean
        get() = _isPlayer
        private set(value) {
            if (value == _isPlayer) {
                return
            }

            _isPlayer = value
            preferences.isPlayer = value
            savePreferences()
        }

    var isPlaying by mutableStateOf(false)
        private set

    private var _volume by mutableFloatStateOf(preferences.volume)
    var volume
        get() = _volume
        set(value) {
            if (value == _volume) {
                return
            }

            applyVolume(value)

            _volume = value
            preferences.volume = value
            savePreferences()
        }

    private var _hidden by mutableStateOf(preferences.hidden)
    var hidden: Boolean
        get() = _hidden
        set(value) {
            if (value == _hidden) {
                return
            }

            _hidden = value
            preferences.hidden = value
            savePreferences()
        }

    private var _disableVolumeButtons by mutableStateOf(preferences.disableVolumeButtons)
    var disableVolumeButtons: Boolean
        get() = _disableVolumeButtons
        set(value) {
            if (value == _disableVolumeButtons) {
                return
            }

            _disableVolumeButtons = value
            preferences.disableVolumeButtons = value
            savePreferences()
        }
}
