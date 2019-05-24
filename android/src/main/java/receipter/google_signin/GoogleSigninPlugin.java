package receipter.google_signin;

import android.content.ContextWrapper;
import android.content.Intent;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

import java.util.ArrayList;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.Scopes;

import receipter.google_signin.FutureHandler;


/** GoogleSigninPlugin */
public class GoogleSigninPlugin implements MethodCallHandler {
  private GoogleSignInClient _apiClient;
  private final Registrar registrar;
  private MethodChannel methodChannel;

  private FutureHandler handler;

  public static final int RC_SIGN_IN = 9001;
  public static final int REQUEST_CODE_RECOVER_AUTH = 53294;


  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "google_signin");
    channel.setMethodCallHandler(new GoogleSigninPlugin(registrar, channel));
  }

  public GoogleSigninPlugin(Registrar registrar, MethodChannel methodChannel) {
    this.registrar = registrar;
    registrar.addActivityResultListener(new GoogleSigninActivityListener());

    this.methodChannel = methodChannel;
    this.methodChannel.setMethodCallHandler(this);
    this.handler = new FutureHandler(1);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch(call.method) {
      case("configure"):
        configure(result, call);
        break;
      case("signIn"):
        signIn(result);
      case("signInSilently"):
        signInSilently(result);
      default:
        result.notImplemented();
    }
  }

  static Scope[] createScopesArray(String[] scopes) {
    int size = scopes.length;
    Scope[] _scopes = new Scope[size];

    for (int i = 0; i < size; i++) {
        String scopeName = scopes[i];
        _scopes[i] = new Scope(scopeName);
    }

    return _scopes;
  }

  static GoogleSignInOptions getSignInOptions(
    final Scope[] scopes,
    final String webClientId,
    final boolean offlineAccess,
    final boolean forceConsentPrompt,
    final String accountName,
    final String hostedDomain
  ) {
    GoogleSignInOptions.Builder googleSignInOptionsBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(new Scope(Scopes.EMAIL), scopes);
    if (webClientId != null && !webClientId.isEmpty()) {
        googleSignInOptionsBuilder.requestIdToken(webClientId);
        if (offlineAccess) {
            googleSignInOptionsBuilder.requestServerAuthCode(webClientId, forceConsentPrompt);
        }
    }

    if (accountName != null && !accountName.isEmpty()) {
        googleSignInOptionsBuilder.setAccountName(accountName);
    }

    if (hostedDomain != null && !hostedDomain.isEmpty()) {
        googleSignInOptionsBuilder.setHostedDomain(hostedDomain);
    }

    return googleSignInOptionsBuilder.build();
  }

  public void configure(final Result result, final MethodCall config) {
    final ArrayList<String> scopes = config.argument("scopes");
    final String webClientId = config.argument("id");
    final boolean offlineAccess = config.hasArgument("offlineAccess") ? (boolean)config.argument("offlineAccess") : false;
    final boolean forceConsentPrompt = config.hasArgument("forceConsentPrompt") ? (boolean)config.argument("forceConsentPrompt") : false;
    final String accountName = config.argument("accountName");
    final String hostedDomain = config.argument("hostedDomain");

    final String[] scopeArray = scopes.toArray(new String[scopes.size()]);
    final GoogleSignInOptions options = getSignInOptions(createScopesArray(scopeArray), 
        webClientId, offlineAccess, forceConsentPrompt, accountName, hostedDomain);
    _apiClient = GoogleSignIn.getClient(new ContextWrapper(registrar.activity().getApplicationContext()), options);

    result.success(null);
  }

  public void signInSilently(Result result) {
    if (_apiClient == null) {
      throw new IllegalStateException("The API client is not initiallized");
    }

    this.handler.setOperation(result, "signInSilently", null);
    Task<GoogleSignInAccount> task = signInClient.silentSignIn();

    if (task.isSuccessful()) {
      handleSignInResult(task);
    } else {
      task.addOnCompleteListener(
        new OnCompleteListener<GoogleSignInAccount>() {
          @Override
          public void onComplete(Task<GoogleSignInAccount> task) {
            handleSignInResult(task);
          }
        }
      );
    }
  }

  private void handleSignInResult(Task<GoogleSignInAccount> task) {

  }

  public void signIn(Result result) {
      if (registrar.activity() == null) {
        throw new IllegalStateException("signIn needs a foreground activity");
      }

      if (_apiClient == null) {
        throw new IllegalStateException("Client has not yet been configured");
      }

      this.handler.setOperation(result, "signIn", null);

      Intent signInIntent = _apiClient.getSignInIntent();
      registrar.activity().startActivityForResult(signInIntent, RC_SIGN_IN);
    }


  private class GoogleSigninActivityListener implements ActivityResultListener {
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
      if (handler.getOperation() == null) {
        return false;
      }

      if(requestCode == RC_SIGN_IN) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        this.handleSignInResult(task);
      }

      return false;
    }
  }
    
}
