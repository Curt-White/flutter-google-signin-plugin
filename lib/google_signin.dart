// import 'dart:async';

import 'package:flutter/services.dart';

class GoogleSignin {
  static const MethodChannel _channel =
      const MethodChannel('google_signin');

  // static Future<String> get platformVersion async {
  //   final String version = await _channel.invokeMethod('getPlatformVersion');
  //   return version;
  // }

  static configure() async {
    var mp = { 
      'scopes': ['https://www.googleapis.com/auth/userinfo.email'],
      'iosClientId': '1069760089509-uf228dkrqancv7kqe7oi28qeunqltkad.apps.googleusercontent.com',
      'webClientId': '1069760089509-36242fb9lgju2m6hkdf3mrn6msqur08s.apps.googleusercontent.com',
      'offlineAccess': true,
      'forceConsentPrompt': false 
    };
    print("here1");
    await _channel.invokeMethod('configure', mp);
    print("here");
    signIn();
  }

  static signIn() async {
    await _channel.invokeMethod('signIn');
  }
}
