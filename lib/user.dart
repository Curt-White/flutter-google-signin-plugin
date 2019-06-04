import 'dart:ui' show hashValues;

/// A signed in user account
class GoogleAccount {
  String idToken;
  String firstName;
  String lastName;
  String fullName;
  String email;
  String serverAuthCode;
  String googleId;
  String photoUrl;

  GoogleAccount(
    String idToken,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String serverAuthCode,
    String googleId,
    String photoUrl,
  ) {
    this.idToken = idToken;
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = fullName;
    this.email = email;
    this.serverAuthCode = serverAuthCode;
    this.googleId = googleId;
    this.photoUrl = photoUrl;
  }


  @override
  int get hashCode => hashValues(this.fullName, this.email, this.googleId, 
                                 this.idToken, this.serverAuthCode, this.firstName, this.lastName);

  @override
  bool operator == (dynamic other) {
    if (identical(this, other)) return true;
    if (other is! GoogleAccount) return false;
    final GoogleAccount otherAccount = other;
    return this.idToken == otherAccount.idToken &&
        this.email== otherAccount.email &&
        this.googleId == otherAccount.googleId &&
        this.photoUrl == otherAccount.photoUrl &&
        this.fullName == otherAccount.fullName &&
        this.firstName == otherAccount.firstName &&
        this.lastName == otherAccount.lastName &&
        this.serverAuthCode == otherAccount.serverAuthCode &&
        this.photoUrl == otherAccount.photoUrl;
  }
}

/// The authentication results for the getTokens process
class Auth {
  String accessToken;
  String idToken;

  Auth(String accessToken, String idToken) {
    this.accessToken = accessToken;
    this.idToken = idToken;
  }
}