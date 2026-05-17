import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  install(): void;
  notifyJsException(message: string, stack: string): void;
  triggerTestNativeCrash(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('CrashScreenshot');
