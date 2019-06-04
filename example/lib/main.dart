import 'package:flutter/material.dart';
// import 'dart:async';

// import 'package:flutter/services.dart';
import 'package:google_signin/google_signin.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  // Example config object
  Config config = new Config(
    ['https://www.googleapis.com/auth/userinfo.email'],
    'IosClientId',
    'WebClientId',
    offlineAccess: true 
  );
  
  GoogleAccount acc;
  bool signedIn = false;

  @override
  void initState() {
    super.initState();
    GoogleSignin.configure(config);

    GoogleSignin.onCurrentUserChanged.listen((data) {
      setState(() {
        acc = data;
      });
    });
  }

  /// Testing buttons and text to vizualise the state of user account through different actions
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              Text(acc == null ? " " : acc.email.toString()),
              Text(acc == null ? " " : acc.firstName.toString()),
              Text(acc == null ? " " : acc.lastName.toString()),
              Text(acc == null ? " " : acc.googleId.toString()),
              Text(acc == null ? " " : "serverauth " + (acc.serverAuthCode != null).toString()),
              Text(acc == null ? " " : "idToken " + (acc.idToken != null).toString()),
              MaterialButton(
                onPressed: GoogleSignin.signIn,
                child: Text("Login",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
              MaterialButton(
                onPressed: GoogleSignin.signOut,
                child: Text("Sign Out",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
              MaterialButton(
                onPressed: GoogleSignin.signInSilently,
                child: Text("Signin Silently",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
              MaterialButton(
                onPressed: () async { bool val = await GoogleSignin.isSignedIn(); setState(() {
                  signedIn = this.signedIn = val;
                });},
                child: Text("Is Signed In",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
              Text(signedIn.toString()),
              MaterialButton(
                onPressed: GoogleSignin.disconnect,
                child: Text("Disconnect",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
              MaterialButton(
                onPressed: GoogleSignin.clearTokenCache,
                child: Text("Clear Token Cache",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
