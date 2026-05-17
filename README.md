# react-native-crash-screenshot

Capture screenshots on JS and native crashes

## Installation


```sh
npm install react-native-crash-screenshot
```


## Usage

Call `initializeCrashScreenshot()` once when your app starts (for example inside `useEffect` on the root component). On Android and iOS, uncaught native crashes capture the current window to JPEG files under `crash_screenshots` in the app sandbox. **Fatal** JavaScript errors are forwarded to native before the existing React Native handler runs so a screenshot can be attempted.

```tsx
import { useEffect } from 'react';
import { initializeCrashScreenshot } from 'react-native-crash-screenshot';

export default function App() {
  useEffect(() => {
    initializeCrashScreenshot();
  }, []);

  return null;
}

// Native crash test (optional): import { NativeCrashScreenshot } from 'react-native-crash-screenshot';
// then call NativeCrashScreenshot.triggerTestNativeCrash();
```


## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
