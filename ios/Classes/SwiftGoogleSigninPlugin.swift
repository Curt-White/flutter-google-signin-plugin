import Flutter
import UIKit
import GoogleSignIn

@available(iOS 9.0, *)
public class SwiftGoogleSigninPlugin: NSObject, FlutterPlugin, GIDSignInDelegate, GIDSignInUIDelegate {
  var _accountRequest: FlutterResult? = nil;
  
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "google_signin", binaryMessenger: registrar.messenger());
    let instance = SwiftGoogleSigninPlugin();
    registrar.addMethodCallDelegate(instance, channel: channel);
  }
  
  // Initialize the google sign in instance
  public override init() {
    super.init();
    
    GIDSignIn.sharedInstance().delegate = self;
    GIDSignIn.sharedInstance().uiDelegate = self;
    GIDSignIn.sharedInstance().shouldFetchBasicProfile = true;

    signal(SIGPIPE, SIG_IGN);
  }
  
  // Return proper
  public func getFlutterError(error: Error) -> FlutterError {
    let code: Int = (error as NSError).code;
    let message: String = (error as NSError).domain;
    let description: String = (error as NSError).localizedDescription;

    var errorCode: String;

    if (code == GIDSignInErrorCode.hasNoAuthInKeychain.rawValue) {
      errorCode = "not_signed_in";
    } else if (code == GIDSignInErrorCode.canceled.rawValue) {
      errorCode = "sign_in_cancelled";
    } else {
      errorCode = "sign_in_failed";
    }
    
    return FlutterError(code: errorCode, message: message, details: description);
  }

  // Flutter handle the function calls
  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch (call.method) {
    case "configure":
      guard let args = (call.arguments as? [String : Any]) else {
        result("No Arguments Found");
        return
      };
      
      configure(args: args, result: result);
      result(nil);
    case "signIn":
      if !setAccountRequest(result: result) {
        return;
      }
      GIDSignIn.sharedInstance()?.signIn();
    case "isSignedIn":
      result(GIDSignIn.sharedInstance()?.hasAuthInKeychain());
    case "signInSilently":
      if !setAccountRequest(result: result) {
        return;
      }
      GIDSignIn.sharedInstance()?.signInSilently();
    case "getTokens":
      let user: GIDGoogleUser? = GIDSignIn.sharedInstance()?.currentUser;
      let auth: GIDAuthentication = user!.authentication;
      auth.getTokensWithHandler { (auth, error) in
        result(error != nil ? error : [
          "idToken": auth?.idToken,
          "accessToken": auth?.accessToken
        ]);
      }
    case "signOut":
      GIDSignIn.sharedInstance()?.signOut();
      result(nil);
    case "disconnect":
      if !setAccountRequest(result: result) {
        return;
      }
      GIDSignIn.sharedInstance()?.disconnect();
    case "clearTokenCache":
      result(nil);
    default:
      result("Not Implemented");
    }
  }

  // Configure the google sign in client
  public func configure(args: [String : Any], result: FlutterResult) {
    guard let iosClientId = (args["iosClientId"] as? String) else {
      result("Must Provide an iosClientId")
      return;
    };
    GIDSignIn.sharedInstance()?.clientID = iosClientId;
  
    if args["loginHint"] != nil {
      GIDSignIn.sharedInstance()?.loginHint = (args["loginHint"] as? String);
    }
    
    if args["scopes"] != nil {
      GIDSignIn.sharedInstance()?.scopes = (args["scopes"] as? [String]);
    }

    if args["hostedDomain"] != nil {
      GIDSignIn.sharedInstance()?.hostedDomain = (args["hostedDomain"] as? String);
    }
    
    if args["webClientId"] != nil {
      GIDSignIn.sharedInstance()?.serverClientID = (args["webClientId"] as? String);
    }
  }
  
  // set the current transaction and return an error if there are multiple concurrent transactions
  private func setAccountRequest(result: @escaping FlutterResult) -> Bool {
    if (self._accountRequest != nil) {
      result(FlutterError(code: "", message: "Concurrent Requests", details: "There is more than one concurrent request"));
      return false;
    }
    
    self._accountRequest = result;
    return true;
  }
  
  private func application(_ app: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool {
    return GIDSignIn.sharedInstance().handle(url as URL?,
                                             sourceApplication: options[UIApplicationOpenURLOptionsKey.sourceApplication] as? String,
                                             annotation: options[UIApplicationOpenURLOptionsKey.annotation])
  }

  // show the view controller
  public func sign(_ signIn: GIDSignIn!, present viewController: UIViewController!) {
    viewController.present(viewController, animated: true, completion: nil)
  }
  
  // dismiss the view controller
  public func sign(_ signIn: GIDSignIn!, dismiss viewController: UIViewController!) {
    viewController.dismiss(animated: true, completion: nil);
  }
  
  // after sign in directed to this method and send the account informatin as a result
  public func sign(_ signIn: GIDSignIn!, didSignInFor user: GIDGoogleUser!, withError error: Error!) {
    if (error != nil) {
      responseWithSigninResult(account: nil, error: error);
      return
    }
    
    var userInfo: [String: Any] = [:];
    userInfo["id"] = user.userID;
    userInfo["name"] = user.profile.name;
    userInfo["firstName"] = user.profile.givenName;
    userInfo["lastName"] = user.profile.familyName;
    userInfo["email"] = user.profile.email;
    userInfo["photo"] = user.profile.imageURL(withDimension: 1200)?.absoluteString;
    
    var params: [String : Any] = [:];
    params["user"] = userInfo;
    params["authCode"] = user.serverAuthCode;
    params["idToken"] = user.authentication.idToken;

    responseWithSigninResult(account: params, error: nil)
  }
  
  // call back here when the client is disconnected
  public func sign(_ signIn: GIDSignIn!, didDisconnectWith user: GIDGoogleUser!, withError error: Error!) {
    responseWithSigninResult(account: [:], error: nil)
  }
  
  
  public func responseWithSigninResult(account: [String : Any]?, error: Error? = nil) {
    print(self._accountRequest == nil);
    guard let result: FlutterResult = self._accountRequest else {
      print("Error on account request, not null");
      return;
    }

    self._accountRequest = nil;
    result(error != nil ? getFlutterError(error: error!) : account);
  }
}
