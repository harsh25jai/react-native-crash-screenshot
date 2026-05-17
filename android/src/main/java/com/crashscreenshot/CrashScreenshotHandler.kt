package com.crashscreenshot

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object CrashScreenshotHandler {
  private const val TAG = "CrashScreenshot"

  private val mainHandler = Handler(Looper.getMainLooper())

  @Volatile private var currentActivity: WeakReference<Activity>? = null

  @Volatile private var chainHandler: Thread.UncaughtExceptionHandler? = null

  @Volatile private var initialized = false

  fun install(application: Application) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      initialized = true
      application.registerActivityLifecycleCallbacks(
          object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
              currentActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
          })

      chainHandler = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
        try {
          saveScreenshotSync("uncaught_${throwable.javaClass.simpleName}")
        } catch (_: Throwable) {}
        chainHandler?.uncaughtException(thread, throwable)
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun notifyJsException(message: String, stack: String) {
    try {
      val suffix = Integer.toHexString(message.hashCode())
      saveScreenshotSync("js_$suffix")
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to capture JS exception screenshot", e)
    }
  }

  fun scheduleTestCrash() {
    mainHandler.postDelayed(
        {
          throw RuntimeException("react-native-crash-screenshot: test native crash")
        },
        250L)
  }

  private fun saveScreenshotSync(label: String) {
    val latch = CountDownLatch(1)
    mainHandler.post {
      try {
        val activity = currentActivity?.get()
        val window = activity?.window
        val view = window?.decorView
        if (activity == null || window == null || view == null || view.width <= 0 || view.height <= 0) {
          latch.countDown()
          return@post
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, view.width, view.height)
        PixelCopy.request(
            window,
            rect,
            bitmap,
            { copyResult: Int ->
              try {
                if (copyResult == PixelCopy.SUCCESS) {
                  persistBitmap(activity, bitmap, label)
                  Log.i(TAG, "Saved crash screenshot ($label)")
                }
              } catch (e: Throwable) {
                Log.e(TAG, "Failed persisting screenshot", e)
              } finally {
                if (!bitmap.isRecycled) {
                  bitmap.recycle()
                }
                latch.countDown()
              }
            },
            mainHandler)
      } catch (e: Throwable) {
        Log.e(TAG, "PixelCopy request failed", e)
        latch.countDown()
      }
    }
    latch.await(3, TimeUnit.SECONDS)
  }

  private fun persistBitmap(activity: Activity, bitmap: Bitmap, label: String) {
    val baseDir =
        activity.getExternalFilesDir("crash_screenshots")
            ?: File(activity.filesDir, "crash_screenshots").also { it.mkdirs() }
    if (!baseDir.exists()) {
      baseDir.mkdirs()
    }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val safeLabel = label.replace(Regex("[^a-zA-Z0-9_-]+"), "_").take(80)
    val file = File(baseDir, "${stamp}_${safeLabel}.jpg")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
  }
}
