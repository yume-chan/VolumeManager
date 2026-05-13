package moe.chensi.volume.system

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import org.joor.Reflect
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("PropertyName")
@SuppressLint("ResourceType")
class BrightnessUtils(context: Context) {
    var R: Float = 0.5f
    var A: Float = 0.17883277f
    var B: Float = 0.28466892f
    var C: Float = 0.5599107f

    init {
        Log.d("BrightnessUtils", "MANUFACTURER: ${Build.MANUFACTURER}")

        if (Build.MANUFACTURER == "Xiaomi") {
            val resources = context.resources

            val rId = resources.getIdentifier(
                "config_GammaLinearConvertRValue", "dimen", "android.miui"
            )
            val aId = resources.getIdentifier(
                "config_GammaLinearConvertAValue", "dimen", "android.miui"
            )
            val bId = resources.getIdentifier(
                "config_GammaLinearConvertBValue", "dimen", "android.miui"
            )
            val cId = resources.getIdentifier(
                "config_GammaLinearConvertCValue", "dimen", "android.miui"
            )

            if (rId != 0 && aId != 0 && bId != 0 && cId != 0) {
                R = resources.getFloat(rId)
                A = resources.getFloat(aId)
                B = resources.getFloat(bId)
                C = resources.getFloat(cId)
            }
        }

        Log.d("BrightnessUtils", "R: $R")
        Log.d("BrightnessUtils", "A: $A")
        Log.d("BrightnessUtils", "B: $B")
        Log.d("BrightnessUtils", "C: $C")
    }

    fun convertGammaPercentageToLinear(v: Float, min: Float, max: Float): Float {
        val ret: Float = if (v <= R) {
            (v / R).pow(2)
        } else {
            exp((v - C) / A) + B
        }

        // HLG is normalized to the range [0, 12], ensure that value is within that range,
        // it shouldn't be out of bounds.
        val normalizedRet = ret.coerceIn(0f, 12f)

        // Re-normalize to the range [0, 1]
        // in order to derive the correct setting value.
        return lerp(min, max, normalizedRet / 12)
    }

    fun convertLinearToGammaPercentage(v: Float, min: Float, max: Float): Float {
        // For some reason, HLG normalizes to the range [0, 12] rather than [0, 1]
        val normalizedVal: Float = norm(min, max, v) * 12
        return if (normalizedVal <= 1f) {
            sqrt(normalizedVal) * R
        } else {
            A * ln(normalizedVal - B) + C
        }
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    private fun norm(start: Float, stop: Float, value: Float): Float {
        val range = stop - start
        if (range == 0f) {
            return 0f
        }
        return (value - start) / range
    }
}
