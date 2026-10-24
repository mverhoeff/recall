package com.recall.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

val ClayEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
val SettleEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Immutable
data class KilnMotion(val reduced: Boolean) {
    fun <T> fast(easing: Easing = SettleEasing, delay: Int = 0): TweenSpec<T> = spec(140, easing, delay)
    fun <T> base(easing: Easing = SettleEasing, delay: Int = 0): TweenSpec<T> = spec(260, easing, delay)
    fun <T> slow(easing: Easing = SettleEasing, delay: Int = 0): TweenSpec<T> = spec(520, easing, delay)

    private fun <T> spec(duration: Int, easing: Easing, delay: Int): TweenSpec<T> =
        if (reduced) tween(0) else tween(duration, delay, easing)
}

fun systemAnimationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
