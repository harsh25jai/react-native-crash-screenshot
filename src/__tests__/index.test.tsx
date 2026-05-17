import { afterAll, afterEach, beforeAll, describe, expect, it, jest } from '@jest/globals';

jest.mock('react-native/Libraries/vendor/core/ErrorUtils', () => ({
  __esModule: true,
  default: {
    getGlobalHandler: jest.fn(),
    setGlobalHandler: jest.fn(),
  },
}));

jest.mock('../NativeCrashScreenshot', () => ({
  __esModule: true,
  default: {
    install: jest.fn(),
    notifyJsException: jest.fn(),
    triggerTestNativeCrash: jest.fn(),
  },
}));

import ErrorUtils from 'react-native/Libraries/vendor/core/ErrorUtils';
import NativeCrashScreenshot from '../NativeCrashScreenshot';
import { initializeCrashScreenshot, NativeCrashScreenshot as NativeExport } from '../index';

describe('initializeCrashScreenshot', () => {
  const previousHandler = jest.fn();

  let capturedHandler: (error: unknown, isFatal: boolean) => void;

  beforeAll(() => {
    jest.mocked(ErrorUtils.getGlobalHandler).mockReturnValue(previousHandler);
    initializeCrashScreenshot();
    const firstCall = jest.mocked(ErrorUtils.setGlobalHandler).mock.calls[0];
    if (firstCall == null || firstCall[0] == null) {
      throw new Error('expected setGlobalHandler to be called');
    }
    capturedHandler = firstCall[0] as (error: unknown, isFatal: boolean) => void;
  });

  afterEach(() => {
    jest.mocked(NativeCrashScreenshot.notifyJsException).mockClear();
    previousHandler.mockClear();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  it('calls native install once', () => {
    expect(NativeCrashScreenshot.install).toHaveBeenCalledTimes(1);
  });

  it('does not call native install again on second initializeCrashScreenshot', () => {
    initializeCrashScreenshot();
    expect(NativeCrashScreenshot.install).toHaveBeenCalledTimes(1);
  });

  it('registers ErrorUtils.setGlobalHandler once', () => {
    expect(ErrorUtils.setGlobalHandler).toHaveBeenCalledTimes(1);
    expect(capturedHandler).toEqual(expect.any(Function));
  });

  it('forwards JS errors to native then previous handler', () => {
    const err = new Error('boom');
    capturedHandler(err, true);

    expect(NativeCrashScreenshot.notifyJsException).toHaveBeenCalledWith(
      'boom',
      err.stack ?? '',
    );
    expect(previousHandler).toHaveBeenCalledWith(err, true);
  });

  it('handles non-Error values in global handler', () => {
    capturedHandler('plain', false);

    expect(NativeCrashScreenshot.notifyJsException).toHaveBeenCalledWith('plain', '');
    expect(previousHandler).toHaveBeenCalledWith('plain', false);
  });

  it('still invokes previous handler if notifyJsException throws', () => {
    const err = new Error('boom');
    jest.mocked(NativeCrashScreenshot.notifyJsException).mockImplementationOnce(() => {
      throw new Error('notify failed');
    });

    expect(() => capturedHandler(err, true)).toThrow('notify failed');
    expect(previousHandler).toHaveBeenCalledWith(err, true);
  });
});

describe('NativeCrashScreenshot export', () => {
  it('exposes triggerTestNativeCrash', () => {
    expect(NativeExport.triggerTestNativeCrash).toEqual(expect.any(Function));
  });
});
