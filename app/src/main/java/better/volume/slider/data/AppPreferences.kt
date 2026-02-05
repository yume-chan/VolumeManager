package better.volume.slider.data

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    var isPlayer: Boolean = false,
    var volume: Float = 1.0f,
    var hidden: Boolean = false,
    var disableVolumeButtons: Boolean = false
)