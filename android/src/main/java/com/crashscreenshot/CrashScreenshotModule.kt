package com.crashscreenshot

import android.app.Application
import com.facebook.react.bridge.ReactApplicationContext

class CrashScreenshotModule(reactContext: ReactApplicationContext) :
    NativeCrashScreenshotSpec(reactContext) {

  override fun install() {
    val app = reactApplicationContext.applicationContext as? Application ?: return
    CrashScreenshotHandler.install(app)
  }

  override fun notifyJsException(message: String, stack: String) {
    CrashScreenshotHandler.notifyJsException(message, stack)
  }

  override fun triggerTestNativeCrash() {
    CrashScreenshotHandler.scheduleTestCrash()
  }

  companion object {
    const val NAME = NativeCrashScreenshotSpec.NAME
  }
}
