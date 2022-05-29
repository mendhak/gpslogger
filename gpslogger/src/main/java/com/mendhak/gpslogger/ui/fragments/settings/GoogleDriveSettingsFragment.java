package com.mendhak.gpslogger.ui.fragments.settings;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.auth0.android.jwt.JWT;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.googledrive.GoogleDriveManager;
import com.mendhak.gpslogger.ui.Dialogs;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.slf4j.Logger;

import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;

import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class GoogleDriveSettingsFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(GoogleDriveSettingsFragment.class);

    GoogleDriveManager manager;

    private AuthState authState = new AuthState();
    private JWT jwt = null;
    private AuthorizationService authorizationService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = new GoogleDriveManager(PreferenceHelper.getInstance());

        findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setOnPreferenceClickListener(this);
        findPreference("google_drive_test").setOnPreferenceClickListener(this);
        setPreferencesState();

        registerEventBus();
    }

    @Override
    public void onDestroy() {
        unregisterEventBus();
        super.onDestroy();
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void setPreferencesState() {
        authState = GoogleDriveManager.getAuthState();
        if (authState.isAuthorized()) {
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setTitle(R.string.osm_resetauth);
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setSummary(R.string.google_drive_clearauthorization_summary);
        } else {
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setTitle(R.string.osm_lbl_authorize);
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setSummary("");
        }
        findPreference("google_drive_test").setEnabled(authState.isAuthorized());
        findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setEnabled(authState.isAuthorized());
        findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setSummary(PreferenceHelper.getInstance().getGoogleDriveFolderPath());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH)) {
            SimpleFormDialog.build()
                    .title(R.string.google_drive_folder_path)
                    .neg(R.string.cancel)
                    .pos(R.string.ok)
                    .msgHtml(getString(R.string.google_drive_folder_path_summary_1)
                            + "<br /><br />"
                            + getString(R.string.google_drive_folder_path_summary_2))
                    .fields(
                            Input.plain(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH)
                                    .text(PreferenceHelper.getInstance().getGoogleDriveFolderPath())
                    )
                    .show(this, PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH);
            return true;
        }

        if (preference.getKey().equals(PreferenceNames.GOOGLE_DRIVE_RESETAUTH)) {


            if (authState.isAuthorized()) {
                authState = new AuthState();
                saveGoogleDriveAuthState();
                setPreferencesState();
                return true;
            }

            authorizationService = GoogleDriveManager.getAuthorizationService(getActivity());

            SecureRandom sr = new SecureRandom();
            byte[] ba = new byte[64];
            sr.nextBytes(ba);
            String codeVerifier = android.util.Base64.encodeToString(ba, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes());
                String codeChallenge = android.util.Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

                AuthorizationRequest.Builder requestBuilder = new AuthorizationRequest.Builder(
                        GoogleDriveManager.getAuthorizationServiceConfiguration(),
                        GoogleDriveManager.getGoogleDriveApplicationClientID(),
                        ResponseTypeValues.CODE,
                        Uri.parse(GoogleDriveManager.getGoogleDriveApplicationOauth2Redirect())
                ).setCodeVerifier(codeVerifier, codeChallenge, "S256");

                requestBuilder.setScopes(GoogleDriveManager.getGoogleDriveApplicationScopes());
                AuthorizationRequest authRequest = requestBuilder.build();
                Intent authIntent = authorizationService.getAuthorizationRequestIntent(authRequest);
                googleDriveAuthenticationWorkflow.launch(new IntentSenderRequest.Builder(
                        PendingIntent.getActivity(getActivity(), 0, authIntent, 0))
                        .setFillInIntent(authIntent)
                        .build());

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }

            return true;

        }

        if (preference.getKey().equals("google_drive_test")) {
            uploadTestFile();
            return true;
        }

        return false;
    }

    ActivityResultLauncher<IntentSenderRequest> googleDriveAuthenticationWorkflow = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        LOG.debug(String.valueOf(result.getData()));
                        AuthorizationResponse authResponse = AuthorizationResponse.fromIntent(result.getData());
                        AuthorizationException authException = AuthorizationException.fromIntent(result.getData());
                        authState = new AuthState(authResponse, authException);
                        if (authException != null) {
                            LOG.error(authException.toJsonString(), authException);
                        }
                        if (authResponse != null) {
                            TokenRequest tokenRequest = authResponse.createTokenExchangeRequest();
                            authorizationService.performTokenRequest(tokenRequest, new AuthorizationService.TokenResponseCallback() {
                                @Override
                                public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                                    if (ex != null) {
                                        authState = new AuthState();
                                        LOG.error(ex.toJsonString(), ex);
                                    } else {
                                        if (response != null) {
                                            authState.update(response, ex);
                                            if (!Strings.isNullOrEmpty(response.idToken)) {
                                                jwt = new JWT(response.idToken);
                                            }
                                        }
                                    }
                                    saveGoogleDriveAuthState();
                                    setPreferencesState();

                                }
                            });
                        }

                    }

                }
            });


    private void uploadTestFile() {
        Dialogs.progress((FragmentActivity) getActivity(), getString(R.string.please_wait));

        try {
            File testFile = Files.createTestFile();
            manager.uploadFile(testFile.getName());

        } catch (Exception ex) {
            LOG.error("Could not create local test file", ex);
            EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not create local test file", ex));
        }

    }

    void saveGoogleDriveAuthState() {
        PreferenceHelper.getInstance().setGoogleDriveAuthState(authState.jsonSerializeString());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.googledrivesettings, rootKey);
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which != BUTTON_POSITIVE) {
            return true;
        }

        if (dialogTag.equalsIgnoreCase(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH)) {
            PreferenceHelper.getInstance().setGoogleDriveFolderPath(extras.getString(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH));
            findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setSummary(PreferenceHelper.getInstance().getGoogleDriveFolderPath());
            return true;
        }

        return false;
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.GoogleDrive d) {
        LOG.debug("Google Drive Event completed, success: " + d.success);
        Dialogs.hideProgress();
        if (!d.success) {
            Dialogs.showError(getString(R.string.sorry), "Could not upload to Google Drive", d.message, d.throwable, (FragmentActivity) getActivity());
        } else {
            Dialogs.alert(getString(R.string.success), getString(R.string.google_drive_testupload_success), getActivity());
        }
    }
}
