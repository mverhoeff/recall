package com.recall.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** Debug-only screen with every Kiln component, for checking the design system on a device. */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The gallery sets up its own theme so it can switch palettes on the fly.
        setContent { KilnGallery() }
    }
}
