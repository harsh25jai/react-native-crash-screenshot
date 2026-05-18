package com.crashscreenshot

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashScreenshotHandlerInstrumentedTest {

  @Before
  fun install() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    CrashScreenshotHandler.install(app)
    clearScreenshotsDir(app)
  }

  private fun clearScreenshotsDir(app: Application) {
    val dir =
        app.getExternalFilesDir("crash_screenshots")
            ?: File(app.filesDir, "crash_screenshots")
    if (dir.isDirectory) {
      dir.listFiles()?.forEach { f ->
        if (f.isFile && f.name.endsWith(".jpg")) {
          f.delete()
        }
      }
    }
  }

  @Test
  fun notifyJsException_writesJpegUnderCrashScreenshots() {
    ActivityScenario.launch(TestActivity::class.java).use {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      val future =
          Executors.newSingleThreadExecutor()
              .submit { CrashScreenshotHandler.notifyJsException("instrumented_test", "") }
      future.get(20, TimeUnit.SECONDS)
    }

    val app = ApplicationProvider.getApplicationContext<Application>()
    val dir =
        app.getExternalFilesDir("crash_screenshots")
            ?: File(app.filesDir, "crash_screenshots").also { d ->
              if (!d.exists()) {
                d.mkdirs()
              }
            }
    assertTrue("crash_screenshots dir should exist", dir.exists())
    val jpgs = dir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") }
    assertTrue("expected at least one .jpg from notifyJsException", jpgs != null && jpgs.isNotEmpty())
  }
}
