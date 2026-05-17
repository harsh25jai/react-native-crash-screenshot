import ErrorUtils from 'react-native/Libraries/vendor/core/ErrorUtils';

import NativeCrashScreenshot from './NativeCrashScreenshot';

export { default as NativeCrashScreenshot } from './NativeCrashScreenshot';

let initialized = false;

/**
 * Initializes native crash handlers and forwards JS fatal errors to native so a
 * screenshot can be captured before the default RN handler runs.
 */
export function initializeCrashScreenshot(): void {
  if (initialized) {
    return;
  }

  NativeCrashScreenshot.install();

  const previousHandler = ErrorUtils.getGlobalHandler();
  ErrorUtils.setGlobalHandler((error: unknown, isFatal: boolean) => {
    try {
      if (isFatal) {
        const message = error instanceof Error ? error.message : String(error);
        const stack = error instanceof Error ? (error.stack ?? '') : '';
        NativeCrashScreenshot.notifyJsException(message, stack);
      }
    } finally {
      previousHandler(error, isFatal);
    }
  });

  initialized = true;
}
