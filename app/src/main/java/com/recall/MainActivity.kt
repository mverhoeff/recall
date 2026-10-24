package com.recall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import com.recall.debug.KilnGallery
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.KilnTheme
import com.recall.ui.theme.TypePairing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KilnTheme(ClayBody.SLATE, isSystemInDarkTheme(), TypePairing.B, reducedMotion = false) {
                if (BuildConfig.DEBUG) KilnGallery() else BasicText("Recall")
            }
        }
    }
}
