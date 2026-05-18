package com.crashscreenshot

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
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

  private val pixelCopyThread: HandlerThread =
      HandlerThread("CrashScreenshot-PixelCopy").apply { start() }
  private val pixelCopyHandler = Handler(pixelCopyThread.looper)

  @Volatile private var currentActivity: WeakReference<Activity>? = null

  @Volatile private var chainHandler: Thread.UncaughtExceptionHandler? = null

  @Volatile private var initialized = false

  fun install(application: Application, initialActivity: Activity? = null) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
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

            override fun onActivityDestroyed(activity: Activity) {
              if (currentActivity?.get() === activity) {
                currentActivity = null
              }
            }
          })

      initialActivity?.let { act -> currentActivity = WeakReference(act) }

      chainHandler = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
        try {
          saveScreenshotSync("uncaught_${throwable.javaClass.simpleName}")
        } catch (_: Throwable) {}
        chainHandler?.uncaughtException(thread, throwable)
      }
      initialized = true
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
    val captureWork = Runnable { runMainThreadPixelCopy(label, latch) }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      captureWork.run()
    } else {
      mainHandler.post(captureWork)
    }
    latch.await(3, TimeUnit.SECONDS)
  }

  private fun runMainThreadPixelCopy(label: String, latch: CountDownLatch) {
    try {
      val activity = currentActivity?.get()
      val window = activity?.window
      val view = window?.decorView
      if (activity == null || window == null || view == null || view.width <= 0 || view.height <= 0) {
        latch.countDown()
        return
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            pixelCopyHandler)
      } else {
        try {
          val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(bitmap)
          view.draw(canvas)
          persistBitmap(activity, bitmap, label)
          bitmap.recycle()
        } catch (e: Throwable) {
          Log.e(TAG, "Canvas screenshot failed", e)
        } finally {
          latch.countDown()
        }
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Screenshot capture failed", e)
      latch.countDown()
    }
  }

  private fun persistBitmap(activity: Activity, bitmap: Bitmap, label: String) {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val safeLabel = label.replace(Regex("[^a-zA-Z0-9_-]+"), "_").take(80)
    val fileName = "${stamp}_${safeLabel}.jpg"
    val dirs = ArrayList<File>(2)
    activity.getExternalFilesDir("crash_screenshots")?.let { dirs.add(it) }
    dirs.add(File(activity.filesDir, "crash_screenshots"))

    var lastError: Throwable? = null
    for (baseDir in dirs) {
      try {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
          continue
        }
        val file = File(baseDir, fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
        return
      } catch (e: Throwable) {
        lastError = e
        Log.w(TAG, "Could not persist screenshot under ${baseDir.path}", e)
      }
    }
    Log.e(TAG, "Failed to persist crash screenshot for $label", lastError)
  }
}
