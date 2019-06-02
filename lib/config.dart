class Config {
  List<String> scopes;
  String iosClientId;
  String webClientId;
  bool offlineAccess;
  bool forceConsentPrompt;
  String hostedDomain;
  String accountName;

  Config(List<String> scopes, String iosClientId, String webClientId,
         {bool offlineAccess = false, bool forceConsentPrompt = false, String hostedDomain,
         String accountName}) {
    this.scopes = scopes;
    this.iosClientId = iosClientId;
    this.webClientId = webClientId;
    this.offlineAccess = offlineAccess;
    this.forceConsentPrompt = forceConsentPrompt;
    this.hostedDomain = hostedDomain;
    this.accountName = accountName;
  }

  Map<String, Object> toMap() {
    return {
      'scopes': this.scopes,
      'iosClientId': this.iosClientId,
      'webClientId': this.webClientId,
      'offlineAccess': this.offlineAccess,
      'forceConsentPrompt': this.forceConsentPrompt,
      'hostedDomain': this.hostedDomain,
      'accountName': this.accountName
    };
  }
}
