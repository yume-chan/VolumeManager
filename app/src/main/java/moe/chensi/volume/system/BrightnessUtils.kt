package moe.chensi.volume.system

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.os.Build
import android.util.Log
import android.util.MathUtils

@SuppressLint("ResourceType")
object BrightnessUtils {
    var R: Float = 0.5f
    var A: Float = 0.17883277f
    var B: Float = 0.28466892f
    var C: Float = 0.5599107f

    init {
        Log.d("BrightnessUtils", "MANUFACTURER: ${Build.MANUFACTURER}")

        if (Build.MANUFACTURER == "Xiaomi") {
            val app = ActivityThread.currentApplication()
            val resources = app.resources

            val rId =
                resources.getIdentifier("config_GammaLinearConvertRValue", "dimen", "android.miui")
            val aId =
                resources.getIdentifier("config_GammaLinearConvertAValue", "dimen", "android.miui")
            val bId =
                resources.getIdentifier("config_GammaLinearConvertBValue", "dimen", "android.miui")
            val cId =
                resources.getIdentifier("config_GammaLinearConvertCValue", "dimen", "android.miui")

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
            MathUtils.sq(v / R)
        } else {
            MathUtils.exp((v - C) / A) + B
        }

        // HLG is normalized to the range [0, 12], ensure that value is within that range,
        // it shouldn't be out of bounds.
        val normalizedRet = MathUtils.constrain(ret, 0f, 12f)

        // Re-normalize to the range [0, 1]
        // in order to derive the correct setting value.
        return MathUtils.lerp(min, max, normalizedRet / 12)
    }

    fun convertLinearToGammaPercentage(v: Float, min: Float, max: Float): Float {
        // For some reason, HLG normalizes to the range [0, 12] rather than [0, 1]
        val normalizedVal: Float = MathUtils.norm(min, max, v) * 12
        return if (normalizedVal <= 1f) {
            MathUtils.sqrt(normalizedVal) * R
        } else {
            A * MathUtils.log(normalizedVal - B) + C
        }
    }
}