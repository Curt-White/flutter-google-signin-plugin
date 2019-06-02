class GoogleAccount {
  String idToken;
  String firstName;
  String lastName;
  String fullName;
  String email;
  String serverAuthCode;
  String googleId;

  GoogleAccount(
    String idToken,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String serverAuthCode,
    String googleId
  ) {
    this.idToken = idToken;
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = fullName;
    this.email = email;
    this.serverAuthCode = serverAuthCode;
    this.googleId = googleId;
  }
}

class Auth {
  String accessToken;
  String idToken;

  Auth(String accessToken, String idToken) {
    this.accessToken = accessToken;
    this.idToken = idToken;
  }
}