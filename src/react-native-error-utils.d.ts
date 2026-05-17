declare module 'react-native/Libraries/vendor/core/ErrorUtils' {
  export type GlobalErrorHandler = (error: unknown, isFatal: boolean) => void;

  const ErrorUtils: {
    getGlobalHandler: () => GlobalErrorHandler;
    setGlobalHandler: (callback: GlobalErrorHandler) => void;
  };

  export default ErrorUtils;
}
