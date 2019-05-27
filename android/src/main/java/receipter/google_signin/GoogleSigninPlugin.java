package receipter.google_signin;

import android.content.ContextWrapper;
import android.content.Intent;

import android.accounts.Account;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


import android.app.Activity;
import android.app.Dialog;

import android.util.Log;
import android.net.Uri;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import com.google.common.base.Joiner;

import receipter.google_signin.FutureHandler;
import receipter.google_signin.FutureHandler.Callback;


/** GoogleSigninPlugin */
public class GoogleSigninPlugin implements MethodCallHandler {
  private GoogleSignInClient _apiClient;
  private final Registrar registrar;
  private MethodChannel methodChannel;
  private List<String> requestedScopes;

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
        break;
      case("signInSilently"):
        signInSilently(result);
        break;
      case("getCurrentUser"):
        getCurrentUser(result);
        break;
      case("getTokens"):
        String email = call.argument("email");
        boolean shouldRecoverAuth = call.argument("shouldRecoverAuth");
        getTokens(result, email, shouldRecoverAuth);
        break;
      case("clearTokenCache"):
        String token = call.argument("token");
        clearTokenCache(result, token);
        break;
      case("signOut"):
        signOut(result);
        break;
      case("disconnect"):
        disconnect(result);
        break;
      default:
        result.notImplemented();
    }
  }

  public void isSignedIn(Result result) {
    boolean isSignedIn = GoogleSignIn.getLastSignedInAccount(registrar.context()) != null;
    result.success(isSignedIn);
  }

  public void getCurrentUser(Result result) {
      GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(registrar.context());
      result.success(account == null ? null : getUserInfo(account));
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

  /**
   * Construct the api client from the parameters
   * @param scopes - The requested scopes
   * @param webClientId - web client id from cloud console
   * @param offlineAccess - allow offline access for requests from 3rd party server
   * @param forceConsentPrompt - force to consent on each login
   * @param accountName - specifies account on device to be used
   * @param hostedDomain - domain restriction
   * @return google
   */
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

  /**
   * Configure the OAuth2 client
   * @param result - Flutter result object
   * @param config - An object containing the config options
   */
  public void configure(final Result result, final MethodCall config) {
    final ArrayList<String> scopes = config.argument("scopes");
    requestedScopes = scopes;
    final String webClientId = config.argument("webClientId");
    final boolean offlineAccess = config.hasArgument("offlineAccess") ? (boolean)config.argument("offlineAccess") : false;
    final boolean forceConsentPrompt = config.hasArgument("forceConsentPrompt") ? (boolean)config.argument("forceConsentPrompt") : false;
    final String accountName = config.argument("accountName");
    final String hostedDomain = config.argument("hostedDomain");

    final String[] scopeArray = scopes.toArray(new String[scopes.size()]);
    final GoogleSignInOptions options = getSignInOptions(createScopesArray(scopeArray), 
        webClientId, offlineAccess, forceConsentPrompt, accountName, hostedDomain);
    _apiClient = GoogleSignIn.getClient(new ContextWrapper(registrar.context()), options);

    result.success(null);
  }

  /**
   * Attempts to sign in the user without the user interface
   * @param result - Flutter result object
   */
  public void signInSilently(Result result) {
    if (_apiClient == null) {
      throw new IllegalStateException("The API client is not initiallized");
    }

    this.handler.setOperation(result, "signInSilently", null);
    Task<GoogleSignInAccount> task = _apiClient.silentSignIn();

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

  /**
   * Handle the results after the signin method is called
   * @param task
   */
  private void handleSignInResult(Task<GoogleSignInAccount> task) {
    try {
      GoogleSignInAccount account = task.getResult(ApiException.class);

      if (account == null) {
        this.handler.finishOperationWithError("handleSignInResult", "Could not handle google sign in, account object is null");
      } else {
        this.handler.finishOperationWithSuccess(getUserInfo(account));
      }
    } catch (ApiException e) {
      handler.finishOperationWithError(Integer.toString(e.getStatusCode()), "Failed to sign in:" + e.getMessage());
    }
  }


  /**
   * Create a map from the user info in the account object
   * @param account - Thwe user account returned after signing in
   * @return a map containing all of the data received from the signin request
   */
  private Map<String, Object> getUserInfo(GoogleSignInAccount account) {
      Map<String, Object> user = new HashMap<>();
      user.put("id", account.getId());
      user.put("name", account.getDisplayName());
      user.put("firstName", account.getGivenName());
      user.put("lastName", account.getFamilyName());
      user.put("email", account.getEmail());
      Uri photo = account.getPhotoUrl();
      user.put("photo", photo != null ? photo.toString() : null);

      Map<String, Object> params = new HashMap<>();
      params.put("user", user);
      params.put("idToken", account.getIdToken());
      params.put("authCode", account.getServerAuthCode());

      ArrayList<String> scopes = new ArrayList<>();
      for(Scope scope : account.getGrantedScopes()){
        String scopeVal = scope.toString();
        if(scopeVal.startsWith("http")) {
          scopes.add(scopeVal);
        }
      }

      params.put("scopes", scopes);
      return params;
  }

  /**
   * Sign in the user
   * @param result - Flutter result object
   */
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

  /**
   * Signs out the user and invalidates their credentials
   * @param result - Flutter result object
   */
  public void disconnect(Result result) {
    handler.setOperation(result, "disconnect", null);

    _apiClient.revokeAccess().addOnCompleteListener(
      new OnCompleteListener<Void>() {
        @Override
        public void onComplete(Task<Void> task) {
          if(task.isSuccessful()) {
            handler.finishOperationWithSuccess(null);
          }
        }
      }
    );
  }

  /**
   * Signs the user out but credentials will stay valid
   * @param result - Flutter result object
   */
  public void signOut(Result result) {
    handler.setOperation(result, "signOut", null);

    _apiClient.signOut().addOnCompleteListener(
      new OnCompleteListener<Void>() {
        @Override
        public void onComplete(Task<Void> task) {
          if(task.isSuccessful()) {
            handler.finishOperationWithSuccess(null);
          }
        }
      }
    );
  }

  /**
   * Clears the token from the auth cache so a new token can be retrieved
   * @param result - Flutter result object
   * @param token - token to remove from cache
   */
  public void clearTokenCache(final Result result, final String token) {
    Callable<Void> clearTokenTask = new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        GoogleAuthUtil.clearToken(registrar.context(), token);
        return null;
      }
    };

    // Async call to clearTokenTask and handle with callback upon completion
    handler.handleAsync(
      clearTokenTask,
      new Callback<Void>() {
        @Override
        public void run(Future<Void> clearTokenFuture) {
          try {
            result.success(clearTokenFuture.get());
          } catch (ExecutionException e) {
            result.error("exception", e.getCause().getMessage(), null);
          } catch (InterruptedException e) {
            result.error("exception", e.getMessage(), null);
            Thread.currentThread().interrupt();
          }
        }
      }
    );
  }

  /**
   * Gets OAuth2 token for the specified scopes for the configured client
   * @param result - Flutter result object
   * @param email - email of the account to request token for
   * @param shouldRecoverAuth - If set will attempt to rerun to authenticate
   */
  public void getTokens(final Result result, final String email, final boolean shouldRecoverAuth) {
    if (email == null) {
      result.error("Exception", "Email is null", null);
      return;
    }

    // Function to retrieve token, will be called asynchronously
    Callable<String> getToken = new Callable<String>() {
      @Override
      public String call() throws Exception{
        Account account = new Account(email, "com.google");
        String scopesStr = "oauth2:" + Joiner.on(' ').join(requestedScopes);
        return GoogleAuthUtil.getToken(registrar.context(), account, scopesStr);
      }
    };

    // The callback function which received the result of the previously defined getToken function
    Callback<String> handleTokenResponse = new Callback<String>() {
      @Override 
      public void run(Future<String> tokenResponse) {
        try {
          // Attempt to return token in a hashmap
          String token = tokenResponse.get();
          HashMap<String, String> tokenResult = new HashMap<>();
          tokenResult.put("accessToken", token);
          result.success(tokenResult);
        } catch (ExecutionException e) {
          if (e.getCause() instanceof UserRecoverableAuthException) {
            // If conditions then attemp to recover the auth process
            if (shouldRecoverAuth && handler.getOperation() == null) {
              Activity activity = registrar.activity();
              if (activity == null) {
                result.error(
                    "User Recoverable Auth",
                    "Cant Recover Cause App Not In Foreground" + e.getLocalizedMessage(), null);
              } else {
                handler.setOperation(result, "getTokens", email);
                Intent recoveryIntent = ((UserRecoverableAuthException) e.getCause()).getIntent();
                activity.startActivityForResult(recoveryIntent, REQUEST_CODE_RECOVER_AUTH);
              }
            } else {
              result.error("User Recoverable Auth", e.getLocalizedMessage(), null);
            }
          } else {
            result.error("Exception", e.getCause().getMessage(), null);
          }
        } catch (InterruptedException e) {
          result.error("Exception", e.getMessage(), null);
          Thread.currentThread().interrupt();
        }
      }
    };

    handler.handleAsync(
      getToken,
      handleTokenResponse
    );
  }

  /* 
    Listen for activity events on after the sign in event is accomplished or on 
    recovery of auth request
   */
  private class GoogleSigninActivityListener implements ActivityResultListener {
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
      if (handler.getOperation() == null) {
        return false;
      }

      // On sign in event
      if(requestCode == RC_SIGN_IN) {
        if (data != null) {
          Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
          handleSignInResult(task);
        } else {
          handler.finishOperationWithError("Signin Failed", "null intent in listener");
        }
      // On request for auth request recovery
      } else if (requestCode == REQUEST_CODE_RECOVER_AUTH) {
        if (resultCode == Activity.RESULT_OK) {
          Result result = handler.getOperation().result;
          String email = (String) handler.getOperation().data;
          handler.clearOperation();
          getTokens(result, email, false);
        } else {
          handler.finishOperationWithError("Signin Failed", "Failed to sign in the user");
        }
      }
      return false;
    }
  }
}
