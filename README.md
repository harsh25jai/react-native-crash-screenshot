# react-native-crash-screenshot

Capture screenshots on JS and native crashes

## Installation

```sh
npm install react-native-crash-screenshot
```

**iOS:** From your app project root:

```sh
cd ios && pod install
# OR
cd ios && bundle exec pod install
```

## Usage

Call `initializeCrashScreenshot()` once when your app starts (for example inside `useEffect` on the root component). **Fatal** JavaScript errors are forwarded to native so a screenshot can be taken before the default handler runs. Uncaught native crashes are also captured when possible.

```tsx
import { useEffect } from 'react';
import { initializeCrashScreenshot } from 'react-native-crash-screenshot';

export default function App() {
  useEffect(() => {
    initializeCrashScreenshot();
  }, []);

  return null;

  // Native crash test (optional): 
  // import { NativeCrashScreenshot } from 'react-native-crash-screenshot';

  // return (
  //   <Button
  //       title="Trigger native crash"
  //       onPress={() => {
  //         NativeCrashScreenshot.triggerTestNativeCrash();
  //       }}
  //     />
  // );

}
```

## Where screenshots are saved
- **Android:** `Android/data/<your.application.id>/files/crash_screenshots/` when available, otherwise app internal storage under `files/crash_screenshots/`.

- **iOS:** `Documents/crash_screenshots/` inside the app sandbox. In Xcode: **Window → Devices and Simulators** → select your app → **Download Container…**, then in the downloaded package open **`AppData/Documents/crash_screenshots`**.

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---
