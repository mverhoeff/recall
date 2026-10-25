package com.recall.ui.screens.study

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** A short buzz for a checked answer. Confirm and reject only exist from Android 11 on. */
fun View.answerHaptic(correct: Boolean) {
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (correct) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
    } else {
        HapticFeedbackConstants.CONTEXT_CLICK
    }
    performHapticFeedback(type)
}
