import 'package:flutter/services.dart';

import "./config.dart";
import './user.dart';

class GoogleSignin {
  static const MethodChannel _channel =
      const MethodChannel('google_signin');
  static GoogleAccount _account;

  static configure(Config config) async {
    await _channel.invokeMethod('configure', config.toMap());
  }

  static Future<GoogleAccount> signIn() async {
    Map<String, dynamic> info = await _channel.invokeMethod('signIn');
    _account = new GoogleAccount(info["idToken"], info["user"]["firstName"], 
                               info["user"]["lastName"], info["user"]["fullName"], 
                               info["user"]["email"], info["authCode"], info["user"]["id"]);
    print(_account);
    return _account;
  }

  /// Returns an authentication object containing access and id tokens
  /// Throws a StateError if the user has not yet been signed in
  static Future<Auth> get auth async {
    if (_account == null) {
      throw StateError("User is not signed in");
    }

    var resp = await _channel.invokeMethod("getTokens", <String, dynamic>{
      'email': _account.email,
      'shouldRecoverAuth': true,
    });

    if (resp["idToken"] == null) {
      resp["idToken"] = _account.idToken;
    }

    return new Auth(resp["accessToken"], resp["idToken"]);
  }

  /// Can clear the auth cache of possibly invalid tokens
  /// This method has no function on ios devices
  static Future<void> clearTokenCache() async {
    String token = (await auth).accessToken;
    await _channel.invokeMethod("clearTokenCache", token);
  }

  
}
