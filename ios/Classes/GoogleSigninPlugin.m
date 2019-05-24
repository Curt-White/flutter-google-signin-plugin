#import "GoogleSigninPlugin.h"
#import <google_signin/google_signin-Swift.h>

@implementation GoogleSigninPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftGoogleSigninPlugin registerWithRegistrar:registrar];
}
@end
