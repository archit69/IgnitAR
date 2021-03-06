package com.vizy.ignitar.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.accountkit.AccessToken;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.vizy.ignitar.R;
import com.vizy.ignitar.constants.IgnitarConstants;
import com.vizy.ignitar.preferences.IgnitarStore;
import com.vizy.ignitar.utils.StringUtils;
import com.vizy.ignitar.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

public class SignIn extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private GoogleApiClient mGoogleApiClient;
    private IgnitarStore ignitarStore;
    private ProgressDialog progressDialog;
    private static final int RC_SIGN_IN = 9001;
    private final String TAG = this.getClass().getSimpleName();
    private GoogleSignInAccount acct;
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private Button mobileNumLogin;
    public static int APP_REQUEST_CODE = 99;

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        ignitarStore = new IgnitarStore(SignIn.this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();

      /*  mGoogleApiClient=new GoogleApiClient.Builder(this).enableAutoManage(this,this).addApi(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .addScope(Scopes.PLUS_LOGIN).addScope(Scopes.PLUS_ME).build();*/
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.COLOR_LIGHT);
        signInButton.setScopes(gso.getScopeArray());
        //  signInButton.set
        //  setGooglePlusButtonText(signInButton,"Log in with Google");
        // Fb login starts from here

        mobileNumLogin = (Button) findViewById(R.id.mobile_number_login);
        AccountKit.initialize(getApplicationContext());
        FacebookSdk.sdkInitialize(getApplicationContext());
        mobileNumLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoginPhone(mobileNumLogin);
            }
        });

        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");
        loginButton.setReadPermissions("public_profile");
        // If using in a fragment
        //  loginButton.setActivi(this);
        // Other app specific specialization
        AccessToken accessToken = AccountKit.getCurrentAccessToken();

        if (accessToken != null) {
            //Handle Returning User
        } else {
            //Handle new or logged out user
        }
        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            ignitarStore.saveTourTaken(true);
                            if ((object != null) && (!object.isNull(IgnitarConstants.FacebookConstants.FIRST_NAME))) {
                                ignitarStore.saveUserEmail(object.getString(IgnitarConstants.FacebookConstants.FIRST_NAME));
                            }
                            if ((object != null) && (!object.isNull(IgnitarConstants.FacebookConstants.FIRST_NAME))) {
                                ignitarStore.saveDeviceName(object.getString(IgnitarConstants.FacebookConstants.EMAIL));
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, StringUtils.isNullOrEmpty(e.getMessage()) ?
                                    IgnitarConstants.Exceptions.JSON_EXCEPTION : e.getMessage());
                        }
                    }
                });
                startActivity(new Intent(SignIn.this, HomeActivity.class));
                finish();
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.d(TAG, StringUtils.isNullOrEmpty(exception.getMessage()) ?
                        IgnitarConstants.Exceptions.FACEBOOK_EXCEPTION : exception.getMessage());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN && data != null) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) { // confirm that this response matches your request
            AccountKitLoginResult loginResult = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            String toastMessage;
            if (loginResult.getError() != null) {
                toastMessage = loginResult.getError().getErrorType().getMessage();
                // showErrorActivity(loginResult.getError());
            } else if (loginResult.wasCancelled()) {
                //toastMessage = "Login Cancelled";
            } else {
                if (loginResult.getAccessToken() != null) {
                    // toastMessage = "Success:" + loginResult.getAccessToken().getAccountId();
                } else {
                    toastMessage = String.format(
                            "Success:%s...",
                            loginResult.getAuthorizationCode().substring(0, 10));
                }
                // If you have an authorization code, retrieve it from
                // loginResult.getAuthorizationCode()
                // and pass it to your server and exchange it for an access token.
                // Success! Start your next activity...
                startActivity(new Intent(SignIn.this, HomeActivity.class));
                finish();
            }
            // Surface the result to your user in an appropriate way.
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {

        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            ignitarStore.saveTourTaken(true);
            if ((acct != null) && (!StringUtils.isNullOrEmpty(acct.getDisplayName()))) {
                ignitarStore.saveUserName(acct.getDisplayName());
            }
            if ((acct != null) && (!StringUtils.isNullOrEmpty(acct.getEmail()))) {
                ignitarStore.saveUserEmail(acct.getEmail());
            }
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void updateUI(boolean b) {
        if (b) {
            Log.d(TAG, "Login");
            startActivity(new Intent(SignIn.this, HomeActivity.class));
            finish();
        } else {
            Log.d(TAG, "error");
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void onLoginPhone(@NonNull final View view) {
        final Intent intent = new Intent(SignIn.this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                        AccountKitActivity.ResponseType.CODE);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, configurationBuilder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);
    }

    protected void setGooglePlusButtonText(@NonNull SignInButton signInButton, @NonNull String buttonText) {
        // Find the TextView that is inside of the SignInButton and set its text
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(buttonText);
                return;
            }
        }
    }
}
