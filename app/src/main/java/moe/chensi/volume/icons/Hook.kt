package moe.chensi.volume.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Hook: ImageVector
    get() {
        if (_Hook != null) return _Hook!!
        _Hook = ImageVector.Builder(
            name = "Hook",
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
                moveTo(18.0f, 6.0f)
                arcToRelative(
                    3.99f,
                    3.99f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -3.0f,
                    3.86f
                )
                verticalLineTo(17.0f)
                arcToRelative(
                    5.0f,
                    5.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -5.0f,
                    5.0f
                )
                arcToRelative(
                    5.0f,
                    5.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -5.0f,
                    -5.0f
                )
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
                verticalLineTo(9.86f)
                curveToRelative(-1.77f, -0.46f, -3.0f, -2.06f, -3.0f, -3.89f)
                curveTo(10.0f, 3.76f, 11.8f, 2.0f, 14.0f, 2.0f)
                curveToRelative(2.22f, 0.0f, 4.0f, 1.79f, 4.0f, 4.0f)
                moveToRelative(-4.0f, 2.0f)
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    2.0f,
                    -2.0f
                )
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    -2.0f,
                    -2.0f
                )
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    -2.0f,
                    2.0f
                )
                arcToRelative(
                    2.0f,
                    2.0f,
                    0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    2.0f,
                    2.0f
                )
            }
        }.build()
        return _Hook!!
    }

private var _Hook: ImageVector? = null