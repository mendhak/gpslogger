package com.mendhak.gpslogger.senders.googledrive;

import android.content.Context;
import android.net.Uri;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.FileSender;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.json.JSONException;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GoogleDriveManager extends FileSender {

    private static final Logger LOG = Logs.of(GoogleDriveManager.class);
    private final PreferenceHelper preferenceHelper;

    public GoogleDriveManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
    }

    public static String getGoogleDriveApplicationClientID() {
        //OAuth Client for F-Droid release key
        return "889382808911-scco623dhspjbf5guflmg68f61jl1na3.apps.googleusercontent.com";
        // The Client ID doesn't matter too much, it needs to exist, but for verification what Android
        // does is match by SHA1 signing key + package name.
    }

    public static String getGoogleDriveApplicationOauth2Redirect() {
        //Needs to match in androidmanifest.xml
        return "com.mendhak.gpslogger:/oauth2googledrive";
    }

    public static String[] getGoogleDriveApplicationScopes() {
        return new String[]{"https://www.googleapis.com/auth/drive.file"};
    }

    public static AuthorizationService getAuthorizationService(Context context) {
        return new AuthorizationService(context, new AppAuthConfiguration.Builder().build());
    }

    public static AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        return new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://www.googleapis.com/oauth2/v4/token"),
                null,
                Uri.parse("https://accounts.google.com/o/oauth2/revoke?token=")
        );
    }

    public static AuthState getAuthState() {
        AuthState authState = new AuthState();
        String google_drive_auth_state = PreferenceHelper.getInstance().getGoogleDriveAuthState();

        if (!Strings.isNullOrEmpty(google_drive_auth_state)) {
            try {
                authState = AuthState.jsonDeserialize(google_drive_auth_state);

            } catch (JSONException e) {
                LOG.debug(e.getMessage(), e);
            }
        }

        return authState;
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            LOG.debug(f.getName());
            uploadFile(f);
        }
    }

    public void uploadFile(File fileToUpload) {

        String tag = String.valueOf(Objects.hashCode(fileToUpload));
        HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
           put("filePath", fileToUpload.getAbsolutePath());
        }};

        Systems.startWorkManagerRequest(GoogleDriveWorker.class, dataMap, tag);

    }

    @Override
    public boolean isAvailable() {
        return getAuthState().isAuthorized();
    }

    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isGoogleDriveAutoSendEnabled();
    }

    @Override
    public String getName() {
        return SenderNames.GOOGLEDRIVE;
    }

    @Override
    public boolean accept(File file, String s) {
        return true;
    }
}
