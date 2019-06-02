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
  Config config = new Config(
    ['https://www.googleapis.com/auth/userinfo.email'],
    'your ios client id',
    'your web client id',
    offlineAccess: true 
  );

  @override
  void initState() {
    super.initState();
    GoogleSignin.configure(config);
  }


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
              MaterialButton(
                onPressed: GoogleSignin.signIn,
                child: Text("Login",style: TextStyle(color: Colors.white),),color: Colors.blue,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
