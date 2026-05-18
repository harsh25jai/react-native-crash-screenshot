import { NativeModules, TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  install(): void;
  notifyJsException(message: string, stack: string): void;
  triggerTestNativeCrash(): void;
}

function getNativeModule(): Spec {
  const fromTurbo = TurboModuleRegistry.get<Spec>('CrashScreenshot');
  if (fromTurbo != null) {
    return fromTurbo;
  }
  const fromBridge = NativeModules.CrashScreenshot as Spec | undefined;
  if (fromBridge != null) {
    return fromBridge;
  }
  throw new Error(
    "[react-native-crash-screenshot] Native module 'CrashScreenshot' is not registered. " +
      'Rebuild the iOS app after `pod install` (clean DerivedData if needed). ' +
      'The iOS target must link the CrashScreenshot pod (autolinking usually handles this).',
  );
}

export default getNativeModule();
