package com.crashscreenshot

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/** Minimal activity so {@link CrashScreenshotHandler} has a resumed window for PixelCopy. */
class TestActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val frame = FrameLayout(this)
    frame.layoutParams =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    setContentView(frame)
  }
}
