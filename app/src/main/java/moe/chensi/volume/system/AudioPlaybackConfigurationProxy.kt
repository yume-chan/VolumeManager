package moe.chensi.volume.system

import android.media.AudioPlaybackConfiguration
import org.joor.Reflect

class AudioPlaybackConfigurationProxy(raw: AudioPlaybackConfiguration) {
    enum class PlayerState(val value: Int) {
        Unknown(-1), Released(0), Idle(1), Started(2), Paused(3), Stopped(4);
    }

    fun Int.toPlayerState(): PlayerState {
        for (state in PlayerState.entries) {
            if (state.value == this) {
                return state
            }
        }
        return PlayerState.Unknown
    }

    companion object {
        val classReflect: Reflect = Reflect.onClass(AudioPlaybackConfiguration::class.java)
    }

    private val reflect = Reflect.on(raw)

    private val player = reflect.call("getIPlayer")

    val hasPlayer
        get() = player.get<Any>() != null

    val clientPid: Int = reflect.get("mClientPid")

    val playerType: Int = reflect.get("mPlayerType")

    val playerTypeName: String by lazy {
        classReflect.call("toLogFriendlyPlayerType", playerType).get()
    }

    val playerState
        get() = reflect.get<Int>("mPlayerState").toPlayerState()

    val playerStateName: String by lazy {
        classReflect.call("playerStateToString", playerState.value).get()
    }

    val isPlaying: Boolean
        get() {
            if (playerType == 3) {
                return true
            }

            return playerState == PlayerState.Started
        }

    fun setVolume(value: Float): Boolean {
        return try {
            player.call("setVolume", value)
            true
        } catch (e: java.lang.reflect.InvocationTargetException) {
            if (e.cause is android.os.DeadObjectException) {
                false
            } else {
                throw e
            }
        }
    }
}
