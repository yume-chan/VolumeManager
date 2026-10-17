package moe.chensi.volume

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.roundToInt

internal class FloatingButton(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onClick: () -> Unit,
    sizeDp: Int
) {
    companion object {
        private const val MIN_ICON_SIZE_DP = 24
        private const val ICON_PADDING_DP = 16
        private const val COLLAPSED_ALPHA = 0.5f
    }

    private enum class Edge {
        LEFT,
        RIGHT
    }

    private var view: FrameLayout? = null
    private var icon: ImageView? = null
    private var background: GradientDrawable? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var collapsed = false
    private var collapsedOnRight = true
    private var edge: Edge? = Edge.RIGHT
    private var sizeDp = sizeDp.coerceIn(
        MyApplication.MIN_FLOATING_BUTTON_SIZE_DP,
        MyApplication.MAX_FLOATING_BUTTON_SIZE_DP
    )
    private var downRawX = 0f
    private var downRawY = 0f
    private var downX = 0
    private var downY = 0
    private var dragged = false

    fun show() {
        if (view != null) {
            return
        }

        val size = dp(sizeDp)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = screenWidth - size
            y = (screenHeight - size) / 2
        }

        val image = ImageView(context).apply {
            setImageDrawable(context.applicationInfo.loadIcon(context.packageManager))
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val shape = GradientDrawable().apply {
            setColor(0x66000000)
        }
        val button = FrameLayout(context).apply {
            background = shape
            contentDescription = context.getString(R.string.floating_button_open)
            isClickable = true
            addView(
                image,
                FrameLayout.LayoutParams(
                    dp(iconSizeDp()),
                    dp(iconSizeDp()),
                    Gravity.CENTER
                )
            )
            setOnTouchListener { _, event -> onTouch(event) }
        }

        windowManager.addView(button, params)
        view = button
        icon = image
        background = shape
        layoutParams = params
        applyCornerRadii()
    }

    fun hide() {
        val button = view ?: return
        windowManager.removeView(button)
        view = null
        icon = null
        background = null
        layoutParams = null
        collapsed = false
        collapsedOnRight = true
        edge = Edge.RIGHT
    }

    fun setSize(sizeDp: Int) {
        val newSizeDp = sizeDp.coerceIn(
            MyApplication.MIN_FLOATING_BUTTON_SIZE_DP,
            MyApplication.MAX_FLOATING_BUTTON_SIZE_DP
        )
        if (newSizeDp == this.sizeDp) {
            return
        }

        this.sizeDp = newSizeDp
        val params = layoutParams ?: return
        val size = dp(this.sizeDp)
        val screenWidth = context.resources.displayMetrics.widthPixels

        params.width = size
        params.height = size
        params.x = when {
            collapsed && collapsedOnRight -> screenWidth - size / 2
            collapsed -> -size / 2
            edge == Edge.RIGHT -> screenWidth - size
            edge == Edge.LEFT -> 0
            else -> params.x.coerceIn(0, screenWidth - size)
        }
        params.y = params.y.coerceIn(
            0,
            context.resources.displayMetrics.heightPixels - size
        )
        icon?.layoutParams = FrameLayout.LayoutParams(
            dp(iconSizeDp()),
            dp(iconSizeDp()),
            Gravity.CENTER
        )
        updateLayout()
    }

    private fun onTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downX = params.x
                downY = params.y
                dragged = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - downRawX).roundToInt()
                val deltaY = (event.rawY - downRawY).roundToInt()
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

                if (!dragged && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                    dragged = true
                }

                if (dragged) {
                    val size = dp(sizeDp)
                    val screenWidth = context.resources.displayMetrics.widthPixels
                    val targetX = downX + deltaX

                    when {
                        targetX < -dp(8) -> collapse(right = false)
                        targetX > screenWidth - size + dp(8) -> collapse(right = true)
                        else -> {
                            collapsed = false
                            edge = null
                            params.width = size
                            params.x = targetX.coerceIn(0, screenWidth - size)
                            view?.alpha = 1f
                            view?.contentDescription =
                                context.getString(R.string.floating_button_open)
                            applyCornerRadii()
                        }
                    }

                    params.y = (downY + deltaY).coerceIn(
                        0,
                        context.resources.displayMetrics.heightPixels - size
                    )
                    updateLayout()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!dragged) {
                    if (collapsed) {
                        expand()
                    } else {
                        onClick()
                    }
                } else if (!collapsed) {
                    val size = dp(sizeDp)
                    val screenWidth = context.resources.displayMetrics.widthPixels
                    collapsedOnRight = params.x + size / 2 >= screenWidth / 2
                    edge = if (collapsedOnRight) Edge.RIGHT else Edge.LEFT
                    params.x = if (collapsedOnRight) screenWidth - size else 0
                    applyCornerRadii()
                    updateLayout()
                }
                return true
            }
        }

        return true
    }

    private fun collapse(right: Boolean) {
        val params = layoutParams ?: return
        val size = dp(sizeDp)
        val screenWidth = context.resources.displayMetrics.widthPixels

        collapsed = true
        collapsedOnRight = right
        edge = if (right) Edge.RIGHT else Edge.LEFT
        params.width = size
        params.x = if (right) screenWidth - size / 2 else -size / 2
        view?.alpha = COLLAPSED_ALPHA
        view?.contentDescription = context.getString(R.string.floating_button_expand)
        applyCornerRadii()
    }

    private fun expand() {
        val params = layoutParams ?: return
        val size = dp(sizeDp)

        collapsed = false
        params.width = size
        params.x = if (collapsedOnRight) {
            context.resources.displayMetrics.widthPixels - size
        } else {
            0
        }
        edge = if (collapsedOnRight) Edge.RIGHT else Edge.LEFT
        view?.alpha = 1f
        view?.contentDescription = context.getString(R.string.floating_button_open)
        applyCornerRadii()
        updateLayout()
    }

    private fun applyCornerRadii() {
        val radius = dp(16).toFloat()
        background?.cornerRadii = when (edge) {
            Edge.LEFT -> floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
            Edge.RIGHT -> floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
            null -> FloatArray(8) { radius }
        }
    }

    private fun updateLayout() {
        val button = view ?: return
        val params = layoutParams ?: return
        windowManager.updateViewLayout(button, params)
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun iconSizeDp() =
        (sizeDp - ICON_PADDING_DP).coerceAtLeast(MIN_ICON_SIZE_DP)
}
