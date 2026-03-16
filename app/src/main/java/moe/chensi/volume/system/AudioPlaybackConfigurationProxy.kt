package moe.chensi.volume.system

import android.media.AudioPlaybackConfiguration
import android.os.DeadObjectException
import org.joor.Reflect
import org.joor.ReflectException
import java.lang.reflect.InvocationTargetException

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
        } catch (e: ReflectException) {
            val cause = e.cause
            if (cause is InvocationTargetException && cause.cause is DeadObjectException) {
                false
            } else {
                throw e
            }
        }
    }
}
