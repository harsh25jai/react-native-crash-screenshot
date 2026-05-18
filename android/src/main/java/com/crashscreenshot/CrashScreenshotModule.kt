package com.crashscreenshot

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactApplicationContext

class CrashScreenshotModule(reactContext: ReactApplicationContext) :
    NativeCrashScreenshotSpec(reactContext) {

  override fun install() {
    val app = reactApplicationContext.applicationContext as? Application ?: return
    CrashScreenshotHandler.install(app, reactApplicationContext.currentActivity)
    scheduleLegacyWritePermissionIfNeeded()
  }

  override fun notifyJsException(message: String, stack: String) {
    CrashScreenshotHandler.notifyJsException(message, stack)
  }

  override fun triggerTestNativeCrash() {
    CrashScreenshotHandler.scheduleTestCrash()
  }

  /**
   * API 33+ ignores broad WRITE_EXTERNAL_STORAGE for normal apps. API 23–32: request once so
   * app-specific external dirs work on OEMs that still gate them without the permission.
   */
  private fun scheduleLegacyWritePermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    if (Build.VERSION.SDK_INT > 32) return
    val ctx = reactApplicationContext
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED) {
      return
    }

    val tryRequest = Runnable {
      val activity = ctx.currentActivity ?: return@Runnable
      if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
          PackageManager.PERMISSION_GRANTED) {
        return@Runnable
      }
      ActivityCompat.requestPermissions(
          activity,
          arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          PERM_REQUEST_WRITE_EXTERNAL,
      )
    }

    val activity = ctx.currentActivity
    if (activity != null) {
      activity.runOnUiThread(tryRequest)
    } else {
      val listener =
          object : LifecycleEventListener {
            override fun onHostResume() {
              ctx.removeLifecycleEventListener(this)
              ctx.currentActivity?.runOnUiThread(tryRequest)
            }

            override fun onHostPause() {}

            override fun onHostDestroy() {
              ctx.removeLifecycleEventListener(this)
            }
          }
      ctx.addLifecycleEventListener(listener)
    }
  }

  companion object {
    const val NAME = NativeCrashScreenshotSpec.NAME
    private const val PERM_REQUEST_WRITE_EXTERNAL = 0x4353_4352 // 'CRCR' crash screenshot
  }
}
