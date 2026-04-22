package com.mendhak.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import com.google.android.gms.common.SignInButton;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String TAG = "LoginActivity";

    private CredentialManager credentialManager;
    private static final String CLIENT_ID = "906480199699-q332hspmvkbk6f70i5mu4dnaigb75h8f.apps.googleusercontent.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "LoginActivity onCreate");

        credentialManager = CredentialManager.create(this);

//        Button signInButton = findViewById(R.id.signInButton);
        SignInButton signInButton = findViewById(R.id.signInButton);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setColorScheme(SignInButton.COLOR_DARK);
        signInButton.setOnClickListener(v -> {
            Log.d(TAG, "Sign in button clicked");
            signIn(new SignInCallback() {
                @Override
                public void onSuccess(String name, String email, String idToken) {
                    Log.d(TAG, "Sign in success");
                    navigateToMain(name, email);
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Sign in error: " + message);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public interface SignInCallback {
        void onSuccess(String name, String email, String idToken);
        void onError(String message);
    }


    /**
     * Start Google sign-in with Credential Manager.
     */
    public void signIn(SignInCallback callback) {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CancellationSignal cancellationSignal = new CancellationSignal();

        credentialManager.getCredentialAsync(
                this,
                request,
                cancellationSignal,
                Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse response) {
                        handleResponse(response, callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Sign-in failed", e);
                        runOnUiThread(() -> callback.onError("Sign-in failed: " + e.getMessage()));
                    }
                }
        );
    }

    private void handleResponse(GetCredentialResponse response, SignInCallback callback) {
        try {
            Credential credential = response.getCredential();
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());

            String name = googleCredential.getDisplayName();
            String email = googleCredential.getId();
            String idToken = googleCredential.getIdToken();

            saveCredentials(name, email, idToken);
            runOnUiThread(() -> callback.onSuccess(name, email, idToken));

        } catch (Exception e) {
            Log.e(TAG, "Sign-in error", e);
            runOnUiThread(() -> callback.onError("Sign-in error: " + e.getMessage()));
        }
    }

    public void saveCredentials(String name, String email, String idToken) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("id_token", idToken)
                .apply();
    }

    public String getIdToken() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("id_token", null);
    }

    public boolean isTokenValid(String idToken) {
        try {
            // Split JWT (header.payload.signature)
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) return false;

            // Decode payload (Base64URL)
            String payloadJson = new String(Base64.decode(parts[1], Base64.URL_SAFE), StandardCharsets.UTF_8);
            JSONObject payload = new JSONObject(payloadJson);

            // Extract expiration time (in seconds)
            long exp = payload.optLong("exp", 0);
            long currentTimeSec = System.currentTimeMillis() / 1000L;

            return exp > currentTimeSec;

        } catch (Exception e) {
            Log.e(TAG, "Token validation error", e);
            return false;
        }
    }

    public interface TokenRefreshCallback {
        void onSuccess(String newToken);
        void onFailure(String error);
    }
    public void refreshIdToken(TokenRefreshCallback callback) {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse response) {
                        try {
                            Credential credential = response.getCredential();
                            GoogleIdTokenCredential googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.getData());

                            String idToken = googleCredential.getIdToken();
                            LoginActivity.this.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("id_token", idToken)
                                    .apply();

                            Log.d("Auth", "Token refreshed successfully");

                            callback.onSuccess(idToken);
                        } catch (Exception e) {
                            Log.e("Auth", "Failed to refresh token", e);
                            callback.onFailure("Parse error");
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e("Auth", "Token refresh error: " + e.getMessage());
                        callback.onFailure("REAUTH_REQUIRED");
                    }
                }
        );
    }

    public boolean isUserSignedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String idToken = prefs.getString("id_token", null);
        return idToken != null && !idToken.isEmpty();
    }

    public String getUserName() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("user_name", "User");
    }

    public String getUserEmail() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("user_email", "unknown@example.com");
    }

    public void signOut() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
    @Override
    protected void onStart() {
        super.onStart();

        if (isUserSignedIn()) {
            navigateToMain(getUserName(), getUserEmail());
        }
    }

    /**
     * Navigates to MainActivity after successful sign-in.
     */
    private void navigateToMain(String name, String email) {
        Intent intent = new Intent(this, GpsMainActivity.class);
        intent.putExtra("user_name", name);
        intent.putExtra("user_email", email);
        startActivity(intent);
        finish();
    }
}
