package moe.chensi.volume

import MaterialMusicNote
import MaterialNotifications
import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.joor.Reflect
import rikka.shizuku.ShizukuProvider
import java.util.Objects

@SuppressLint("AccessibilityPolicy")
class Service : AccessibilityService() {
    companion object {
        private const val TAG = "VolumeManager.Service"

        private const val ANIMATION_DURATION = 300L
        private const val IDLE_TIMEOUT = 5000L
    }

    private val windowManager: WindowManager by lazy {
        Objects.requireNonNull(
            getSystemService(
                WindowManager::class.java
            )!!
        )
    }
    private lateinit var manager: Manager

    private var idleTimer: CountDownTimer? = null

    private fun startIdleTimer() {
        idleTimer?.cancel()
        idleTimer = object : CountDownTimer(IDLE_TIMEOUT, IDLE_TIMEOUT) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                hideView()
            }
        }.start()
    }

    private var lifecycle: LifecycleRegistry? = null

    private fun createView(): View {
        return object : AbstractComposeView(this) {
            init {
                val owner = object : SavedStateRegistryOwner {
                    private val lifecycleRegistry = LifecycleRegistry(this)

                    private val savedStateRegistryController =
                        SavedStateRegistryController.create(this)

                    init {
                        savedStateRegistryController.performRestore(null)
                        lifecycleRegistry.currentState = Lifecycle.State.STARTED
                        this@Service.lifecycle = lifecycleRegistry
                    }

                    override val lifecycle: Lifecycle
                        get() = lifecycleRegistry

                    override val savedStateRegistry: SavedStateRegistry
                        get() = savedStateRegistryController.savedStateRegistry
                }

                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()

                Log.i(TAG, "onAttachedToWindow")

                if (windowManager.isCrossWindowBlurEnabled && isHardwareAccelerated) {
                    background =
                        Reflect.on(rootSurfaceControl).call("createBackgroundBlurDrawable").apply {
                            call("setBlurRadius", 200)
                            call("setCornerRadius", 40f)
                        }.get()
                }

                startIdleTimer()
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                Log.i(TAG, "onTouchEvent ${event.actionMasked}")

                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    hideView()
                    return true
                }

                return super.onTouchEvent(event)
            }

            @Composable
            override fun Content() {
                var volumeChanged by remember { mutableIntStateOf(0) }

                DisposableEffect(manager.audioManager) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            volumeChanged++
                            startIdleTimer()
                        }
                    }

                    registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

                    onDispose {
                        unregisterReceiver(receiver)
                    }
                }

                return MaterialTheme {
                    Surface(
                        color = Color.Transparent,
                        contentColor = Color.White,
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Color(1f, 1f, 1f, 0.3f), RoundedCornerShape(40f)
                                )
                                .padding(20.dp, 16.dp)
                        ) {
                            AppVolumeList(
                                manager.apps.values,
                                showAll = false,
                                onChange = { startIdleTimer() }) {
                                item(AudioManager.STREAM_MUSIC) {
                                    StreamVolumeSlider(
                                        AudioManager.STREAM_MUSIC,
                                        volumeChanged,
                                        MaterialMusicNote,
                                        "Music",
                                        onChange = { startIdleTimer() })
                                }

                                item(AudioManager.STREAM_NOTIFICATION) {
                                    StreamVolumeSlider(
                                        AudioManager.STREAM_NOTIFICATION,
                                        volumeChanged,
                                        MaterialNotifications,
                                        "Notifications",
                                        onChange = { startIdleTimer() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val layoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // Width
            WindowManager.LayoutParams.WRAP_CONTENT, // Height
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT // Make the background translucent
        ).apply {
            gravity = Gravity.CENTER // Center the view
        }
    }

    private var view: View? = null
    private var viewVisible = false

    private fun showView() {
        if (view == null) {
            Log.i(TAG, "add view")
            // The view doesn't respond to input events if reused
            view = createView()
            layoutParams.alpha = 0f
            windowManager.addView(view, layoutParams)
        }

        if (!viewVisible) {
            Log.i(TAG, "animate in")
            animateAlpha(layoutParams.alpha, 1f, ANIMATION_DURATION)
            startIdleTimer()
            viewVisible = true
        }
    }

    private fun hideView() {
        if (viewVisible) {
            Log.i(TAG, "animate out")
            animateAlpha(layoutParams.alpha, 0f, ANIMATION_DURATION) {
                if (!viewVisible) {
                    Log.i(TAG, "remove view")
                    view!!.background = null
                    lifecycle?.currentState = Lifecycle.State.DESTROYED
                    windowManager.removeView(view)
                    view = null
                }
            }
            viewVisible = false
        }
    }

    private var currentAnimator: ValueAnimator? = null

    private fun animateAlpha(from: Float, to: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        currentAnimator?.cancel()

        val animator = ValueAnimator.ofFloat(from, to)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            if (view != null) {
                layoutParams.alpha = animation.animatedValue as Float
                windowManager.updateViewLayout(view, layoutParams)
            }
        }

        animator.addListener(object : Animator.AnimatorListener {
            var canceled = false

            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                if (canceled) {
                    return
                }

                layoutParams.alpha = to
                windowManager.updateViewLayout(view, layoutParams)

                onEnd?.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {
                canceled = true
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })

        animator.start()
        currentAnimator = animator
    }

    override fun onServiceConnected() {
        Log.i(TAG, "onServiceConnected")

        ShizukuProvider.enableMultiProcessSupport(false)
        manager = Manager(this, (application as MyApplication).dataStore)

        accessibilityButtonController.registerAccessibilityButtonCallback(object :
            AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController?) {
                if (manager.shizukuPermission) {
                    showView()
                }
            }
        })

        Log.i(TAG, "onServiceConnected done ${serviceInfo.capabilities.toString(2)}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.i(TAG, "onKeyEvent ${event.action} ${event.keyCode}")

        if (!manager.shizukuPermission) {
            return false
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (view != null) {
                        manager.audioManager.adjustSuggestedStreamVolume(
                            AudioManager.ADJUST_RAISE, AudioManager.USE_DEFAULT_STREAM_TYPE, 0
                        )
                    }
                    showView()
                }
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (view != null) {
                        manager.audioManager.adjustSuggestedStreamVolume(
                            AudioManager.ADJUST_LOWER, AudioManager.USE_DEFAULT_STREAM_TYPE, 0
                        )
                    }
                    showView()
                }
                return true
            }
        }

        return false
    }

    @Composable
    fun StreamVolumeSlider(
        streamType: Int,
        triggerChange: Int,
        icon: ImageVector,
        name: String,
        onChange: (() -> Unit)? = null
    ) {
        var volume by remember { mutableIntStateOf(manager.audioManager.getStreamVolume(streamType)) }

        LaunchedEffect(triggerChange) {
            volume = manager.audioManager.getStreamVolume(streamType)
        }

        TrackSlider(
            cornerRadius = 20.dp,
            value = volume.toFloat(),
            valueRange = 0f..manager.audioManager.getStreamMaxVolume(streamType).toFloat(),
            onValueChange = { value ->
                manager.audioManager.setStreamVolume(streamType, value.toInt(), 0)
                onChange?.invoke()
            },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp, 8.dp)
            ) {
                Image(
                    imageVector = icon,
                    contentDescription = name,
                    modifier = Modifier.width(32.dp),
                    contentScale = ContentScale.FillWidth
                )

                Text(text = name, color = Color.White)
            }
        }
    }
}
