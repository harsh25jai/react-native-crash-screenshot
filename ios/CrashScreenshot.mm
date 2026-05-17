#import "CrashScreenshot.h"

#import <UIKit/UIKit.h>

#import <CrashScreenshotSpec/CrashScreenshotSpec.h>

#import <memory>
#import <signal.h>
#import <string.h>

static constexpr int kSigCount = 4;
static const int kSignals[kSigCount] = {SIGABRT, SIGSEGV, SIGBUS, SIGILL};
static struct sigaction gPrevSigaction[kSigCount];

static NSUncaughtExceptionHandler *gPreviousNSHandler = nullptr;

static UIWindow *RNCSSKeyWindow(void) {
  for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
    if (scene.activationState != UISceneActivationStateForegroundActive) {
      continue;
    }
    if (![scene isKindOfClass:[UIWindowScene class]]) {
      continue;
    }
    UIWindowScene *windowScene = (UIWindowScene *)scene;
    for (UIWindow *window in windowScene.windows) {
      if (window.isKeyWindow) {
        return window;
      }
    }
    return windowScene.windows.firstObject;
  }
  return nil;
}

static UIImage *RNCSSSnapshot(void) {
  UIWindow *window = RNCSSKeyWindow();
  if (window == nil || window.bounds.size.width < 1 || window.bounds.size.height < 1) {
    return nil;
  }
  UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
  format.opaque = YES;
  format.scale = UIScreen.mainScreen.scale;
  UIGraphicsImageRenderer *renderer =
      [[UIGraphicsImageRenderer alloc] initWithSize:window.bounds.size format:format];
  UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext *_Nonnull context) {
    [window drawViewHierarchyInRect:window.bounds afterScreenUpdates:NO];
  }];
  return image;
}

static void RNCSSPersist(UIImage *image, NSString *label) {
  if (image == nil) {
    return;
  }
  NSData *data = UIImageJPEGRepresentation(image, 0.85);
  if (data == nil) {
    return;
  }
  NSFileManager *fm = NSFileManager.defaultManager;
  NSURL *docs =
      [fm URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask].firstObject;
  if (docs == nil) {
    return;
  }
  NSURL *dir = [docs URLByAppendingPathComponent:@"crash_screenshots" isDirectory:YES];
  [fm createDirectoryAtURL:dir withIntermediateDirectories:YES attributes:nil error:nil];
  NSDateFormatter *fmt = [[NSDateFormatter alloc] init];
  fmt.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
  fmt.dateFormat = @"yyyyMMdd_HHmmss";
  NSString *stamp = [fmt stringFromDate:[NSDate date]];
  NSCharacterSet *allowed = [NSCharacterSet alphanumericCharacterSet];
  NSMutableString *safe = [NSMutableString string];
  for (NSUInteger i = 0; i < label.length; i++) {
    unichar c = [label characterAtIndex:i];
    if ([allowed characterIsMember:c] || c == '_' || c == '-') {
      [safe appendFormat:@"%C", c];
    } else {
      [safe appendString:@"_"];
    }
  }
  if (safe.length > 80) {
    safe = [[safe substringToIndex:80] mutableCopy];
  }
  NSString *name = [NSString stringWithFormat:@"%@_%@.jpg", stamp, safe.length > 0 ? safe : @"crash"];
  NSURL *fileURL = [dir URLByAppendingPathComponent:name];
  [data writeToURL:fileURL atomically:YES];
}

static void RNCSSCapture(NSString *label) {
  dispatch_semaphore_t sem = dispatch_semaphore_create(0);
  dispatch_async(dispatch_get_main_queue(), ^{
    @autoreleasepool {
      UIImage *img = RNCSSSnapshot();
      if (img != nil) {
        RNCSSPersist(img, label);
      }
    }
    dispatch_semaphore_signal(sem);
  });
  dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.8 * NSEC_PER_SEC)));
}

static void RNCSSHandleNSException(NSException *exception) {
  RNCSSCapture(@"objc_exception");
  if (gPreviousNSHandler != nullptr) {
    gPreviousNSHandler(exception);
  }
}

extern "C" void rncss_signal_trampoline(int sig) {
  NSString *label = [NSString stringWithFormat:@"signal_%d", sig];
  RNCSSCapture(label);
  for (int i = 0; i < kSigCount; i++) {
    if (kSignals[i] == sig) {
      sigaction(sig, &gPrevSigaction[i], nullptr);
      raise(sig);
      return;
    }
  }
  raise(sig);
}

static void RNCSSInstallOnce(void) {
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    gPreviousNSHandler = NSGetUncaughtExceptionHandler();
    NSSetUncaughtExceptionHandler(&RNCSSHandleNSException);

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = rncss_signal_trampoline;
    sigemptyset(&sa.sa_mask);
    for (int i = 0; i < kSigCount; i++) {
      int signum = kSignals[i];
      sigaction(signum, &sa, &gPrevSigaction[i]);
    }
  });
}

@implementation CrashScreenshot

- (void)install {
  RNCSSInstallOnce();
}

- (void)notifyJsException:(NSString *)message stack:(NSString *)stack {
  (void)message;
  (void)stack;
  RNCSSCapture(@"js");
}

- (void)triggerTestNativeCrash {
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.2 * NSEC_PER_SEC)),
                 dispatch_get_main_queue(),
                 ^{
                   @throw [NSException exceptionWithName:@"RNCrashScreenshotTest"
                                                    reason:@"test"
                                                  userInfo:nil];
                 });
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeCrashScreenshotSpecJSI>(params);
}

+ (NSString *)moduleName {
  return @"CrashScreenshot";
}

@end
