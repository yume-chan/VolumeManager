package moe.chensi.volume.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val HookOff: ImageVector
    get() {
        if (_HookOff != null) return _HookOff!!
        _HookOff = ImageVector.Builder(
            name = "HookOff",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = null,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
            ) {
                moveTo(13.0f, 9.86f)
                verticalLineToRelative(1.32f)
                lineToRelative(2.0f, 2.0f)
                verticalLineTo(9.86f)
                curveToRelative(2.14f, -0.55f, 3.43f, -2.73f, 2.87f, -4.86f)
                arcTo(4.0f, 4.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 13.0f, 2.11f)
                arcTo(
                    4.01f,
                    4.01f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    10.13f,
                    7.0f
                )
                curveToRelative(0.37f, 1.4f, 1.46f, 2.5f, 2.87f, 2.86f)
                moveTo(14.0f, 4.0f)
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    2.0f,
                    2.0f
                )
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -2.0f,
                    2.0f
                )
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -2.0f,
                    -2.0f
                )
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    2.0f,
                    -2.0f
                )
                moveToRelative(4.73f, 18.0f)
                lineToRelative(-3.87f, -3.87f)
                arcToRelative(
                    5.015f,
                    5.015f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -6.03f,
                    3.69f
                )
                curveTo(6.6f, 21.28f, 5.0f, 19.29f, 5.0f, 17.0f)
                verticalLineToRelative(-5.0f)
                lineToRelative(5.0f, 5.0f)
                horizontalLineTo(7.0f)
                arcToRelative(
                    3.0f,
                    3.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    3.0f,
                    3.0f
                )
                arcToRelative(
                    3.0f,
                    3.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    3.0f,
                    -3.0f
                )
                verticalLineToRelative(-0.73f)
                lineToRelative(-11.0f, -11.0f)
                lineTo(3.28f, 4.0f)
                lineTo(13.0f, 13.72f)
                lineToRelative(2.0f, 2.0f)
                lineToRelative(5.0f, 5.0f)
                close()
            }
        }.build()
        return _HookOff!!
    }

private var _HookOff: ImageVector? = null