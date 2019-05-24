import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_signin/google_signin.dart';

void main() {
  const MethodChannel channel = MethodChannel('google_signin');

  // setUp(() {
  //   channel.setMockMethodCallHandler((MethodCall methodCall) async {
  //     return '42';
  //   });
  // });

  // tearDown(() {
  //   channel.setMockMethodCallHandler(null);
  // });

  test('getPlatformVersion', () async {
    await GoogleSignin.configure();
  });
}
