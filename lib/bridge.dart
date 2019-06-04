import 'dart:async';

import 'package:flutter/services.dart';

import "./config.dart";
import './user.dart';

class GoogleSignin {
  static const MethodChannel _channel = MethodChannel('google_signin');

  static GoogleAccount _account; // current signed in account
  static bool _configured = false; // true if config has been called
  static Config _configuration; // configuration of gsignin

  static bool get isConfigured => _configured;
  static Config get configuration => _configuration;
  static GoogleAccount get currentUser => _account;

  static _MethodCompleter _lastMethodCompleter; // async method blocking

  /// Configure the google sign in process
  /// takes a config object and applies the config to the sign in process
  static configure(Config config) async {
    if (_configured) {
      return;
    }
    await _channel.invokeMethod('configure', config.toMap());
    _configuration = config;
    _configured = true;
  }

  /// Returns true if the user is signed in else false
  static Future<bool> isSignedIn() async {
    final bool result = await _channel.invokeMethod('isSignedIn');
    return result;
  }

  /// Returns a google account on sign in 
  /// Throws state error if the user is already signed in
  static Future<GoogleAccount> signIn() async {
    if (_account != null) {
      throw StateError("User already signed in, Must sign out current user first");
    }

    final Future<GoogleAccount> result = _addMethodCall('signIn');
    // Handle process if cancelled
    bool isCanceled(dynamic error) => error is PlatformException && error.code == 'sign_in_cancelled';
    return result.catchError((dynamic _) => null, test: isCanceled);
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
    await _channel.invokeMethod("clearTokenCache", {"token": token});
  }

  static Future<void> signOut() async => _addMethodCall("signOut");
  static Future<void> disconnect() async => _addMethodCall("disconnect");

  static Future<GoogleAccount> signInSilently() {
    final Future<GoogleAccount> res = _addMethodCall("signInSilently");
    return res.catchError((dynamic error) => print(error));
  }

  // ALL OF THE BELOW CODE WAS COPIED FROM FLUTTERGOOGLESIGNIN PLUGIN
  // BUT MODIFIED SLIGHTLY TO WORK WITH THE STRUCTURE OF THIS PROJECT

  static StreamController<GoogleAccount> _currentUserController =
      StreamController<GoogleAccount>.broadcast();

  /// Subscribe to this stream to be notified when the current user changes.
  static Stream<GoogleAccount> get onCurrentUserChanged =>
      _currentUserController.stream;
  
  static GoogleAccount _setCurrentUser(GoogleAccount currentUser) {
    if (currentUser != _account) {
      _account = currentUser;
      _currentUserController.add(_account);
    }
    return _account;
  }

  static Future<GoogleAccount> _callMethod(String method) async {
    final Map<dynamic, dynamic> info = await _channel.invokeMethod(method);
    return _setCurrentUser(info != null && info.isNotEmpty
        ? new GoogleAccount(info["idToken"], info["user"]["firstName"], 
                            info["user"]["lastName"], info["user"]["fullName"], 
                            info["user"]["email"], info["authCode"], info["user"]["id"],
                            info["user"]["photo"])
        : null);
  }
 
  static Future<GoogleAccount> _addMethodCall(String method) {
    if (_lastMethodCompleter == null) {
      _lastMethodCompleter = _MethodCompleter(method)
        ..complete(_callMethod(method));
      return _lastMethodCompleter.future;
    }

    final _MethodCompleter completer = _MethodCompleter(method);
    _lastMethodCompleter.future.whenComplete(() {
      // If after the last completed call currentUser is not null and requested
      // method is a sign in method, re-use the same authenticated user
      // instead of making extra call to the native side.
      const List<String> kSignInMethods = <String>['signIn', 'signInSilently'];
      if (kSignInMethods.contains(method) && _account != null) {
        completer.complete(_account);
      } else {
        completer.complete(_callMethod(method));
      }
    }).catchError((dynamic _) {
      // Ignore if previous call completed with an error.
    });
    _lastMethodCompleter = completer;
    return _lastMethodCompleter.future;
  }
}

class _MethodCompleter {
  _MethodCompleter(this.method);

  final String method;
  final Completer<GoogleAccount> _completer =
      Completer<GoogleAccount>();

  void complete(FutureOr<GoogleAccount> value) {
    if (value is Future<GoogleAccount>) {
      value.then(_completer.complete, onError: _completer.completeError);
    } else {
      _completer.complete(value);
    }
  }

  bool get isCompleted => _completer.isCompleted;
  Future<GoogleAccount> get future => _completer.future;
}
