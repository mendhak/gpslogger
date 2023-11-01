/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.ui.fragments.settings;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.osm.OpenStreetMapManager;
import com.mendhak.gpslogger.ui.Dialogs;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.ClientSecretPost;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.slf4j.Logger;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;


public class OSMAuthorizationFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, SimpleDialog.OnDialogResultListener {

    private static final Logger LOG = Logs.of(OSMAuthorizationFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    private Handler osmHandler = new Handler();

    //Must be static - when user returns from OSM, this needs to be set already
    private AuthorizationService authorizationService;
    private AuthState authState = new AuthState();


    OpenStreetMapManager manager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = new OpenStreetMapManager(preferenceHelper);

        final Intent intent = getActivity().getIntent();
        final Uri myURI = intent.getData();

        if (myURI != null && myURI.getQuery() != null
                && myURI.getQuery().length() > 0) {
            //User has returned! Read the verifier info from querystring

            Dialogs.progress((FragmentActivity) getActivity(), getString(R.string.please_wait));

            LOG.debug("OAuth user has returned!");
            String oAuthVerifier = myURI.getQueryParameter("oauth_verifier");


        }

        setPreferencesState();

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.osmsettings, rootKey);
    }

    private void setPreferencesState() {
        Preference visibilityPref = findPreference(PreferenceNames.OPENSTREETMAP_VISIBILITY);
        visibilityPref.setOnPreferenceClickListener(this);
        visibilityPref.setSummary(preferenceHelper.getOSMVisibility());


        Preference descriptionPref = findPreference(PreferenceNames.OPENSTREETMAP_DESCRIPTION);
        descriptionPref.setOnPreferenceClickListener(this);
        descriptionPref.setSummary(preferenceHelper.getOSMDescription());

        Preference tagsPref = findPreference(PreferenceNames.OPENSTREETMAP_TAGS);
        tagsPref.setOnPreferenceClickListener(this);
        tagsPref.setSummary(preferenceHelper.getOSMTags());

        Preference resetPref = findPreference("osm_resetauth");

        if (!manager.isOsmAuthorized()) {
            resetPref.setTitle(R.string.osm_lbl_authorize);
            resetPref.setSummary(R.string.osm_lbl_authorize_description);
            visibilityPref.setEnabled(false);
            descriptionPref.setEnabled(false);
            tagsPref.setEnabled(false);
        } else {
            resetPref.setTitle(R.string.osm_resetauth);
            resetPref.setSummary("");
            visibilityPref.setEnabled(true);
            descriptionPref.setEnabled(true);
            tagsPref.setEnabled(true);

        }

        resetPref.setOnPreferenceClickListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENSTREETMAP_VISIBILITY)){

            String[] choices =  getResources().getStringArray(R.array.osm_visibility_choices);
            ArrayList<String> choicesArray = new ArrayList<>(Arrays.asList(choices));
            int chosenIndex = choicesArray.indexOf(preferenceHelper.getOSMVisibility());

            SimpleListDialog.build()
                    .title(R.string.osm_visibility)
                    .msg(R.string.osm_visibility_summary)
                    .pos(R.string.ok)
                    .items(getActivity(), R.array.osm_visibility_choices)
                    .choiceMode(SimpleListDialog.SINGLE_CHOICE_DIRECT)
                    .choicePreset(chosenIndex)
                    .show(this, PreferenceNames.OPENSTREETMAP_VISIBILITY);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENSTREETMAP_DESCRIPTION)){
            SimpleFormDialog.build()
                    .title(R.string.osm_description)
                    .msg(R.string.osm_description_summary)
                    .fields(
                            Input.plain(PreferenceNames.OPENSTREETMAP_DESCRIPTION)
                                    .text(preferenceHelper.getOSMDescription())
                    )
                    .show(this, PreferenceNames.OPENSTREETMAP_DESCRIPTION);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase(PreferenceNames.OPENSTREETMAP_TAGS)){
            SimpleFormDialog.build()
                    .title(R.string.osm_tags)
                    .msg(R.string.osm_tags_summary)
                    .fields(
                            Input.plain(PreferenceNames.OPENSTREETMAP_TAGS)
                                    .text(preferenceHelper.getOSMTags())
                    )
                    .show(this, PreferenceNames.OPENSTREETMAP_TAGS);
            return true;
        }

        if(preference.getKey().equalsIgnoreCase("osm_resetauth")){
            if (manager.isOsmAuthorized()) {
                preferenceHelper.setOSMAccessToken("");
                preferenceHelper.setOSMAccessTokenSecret("");
                preferenceHelper.setOSMRequestToken("");
                preferenceHelper.setOSMRequestTokenSecret("");

                setPreferencesState();

            } else {

                authorizationService = OpenStreetMapManager.getAuthorizationService(getActivity());

                SecureRandom sr = new SecureRandom();
                byte[] ba = new byte[64];
                sr.nextBytes(ba);
                String codeVerifier = android.util.Base64.encodeToString(ba, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(codeVerifier.getBytes());
                    String codeChallenge = android.util.Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

                    AuthorizationRequest.Builder requestBuilder = new AuthorizationRequest.Builder(
                            OpenStreetMapManager.getAuthorizationServiceConfiguration(),
                            OpenStreetMapManager.getOpenStreetMapClientID(),
                            ResponseTypeValues.CODE,
                            Uri.parse(OpenStreetMapManager.getOpenStreetMapRedirect())
                    ).setCodeVerifier(codeVerifier, codeChallenge, "S256");

                    requestBuilder.setScopes(OpenStreetMapManager.getOpenStreetMapClientScopes());
                    AuthorizationRequest authRequest = requestBuilder.build();
                    Intent authIntent = authorizationService.getAuthorizationRequestIntent(authRequest);
                    openStreetMapAuthenticationWorkflow.launch(new IntentSenderRequest.Builder(
                            PendingIntent.getActivity(getActivity(), 0, authIntent, 0))
                            .setFillInIntent(authIntent)
                            .build());

                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }


            }
            return true;
        }


        return true;
    }

    ActivityResultLauncher<IntentSenderRequest> openStreetMapAuthenticationWorkflow = registerForActivityResult(
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
//                            ClientAuthentication clientAuth = new ClientSecretPost(OpenStreetMapManager.getOpenStreetMapClientSecret());
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

                                        }
                                    }
//                                    saveOSMAuthState();
                                    // save the auth state to preferences now
                                    LOG.info(authState.jsonSerializeString());
                                    setPreferencesState();


                                }
                            });
                        }

                    }

                }
            });

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(which != BUTTON_POSITIVE){
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENSTREETMAP_VISIBILITY)){
            String visibility = extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL);
            preferenceHelper.setOSMVisibility(visibility);
            findPreference(PreferenceNames.OPENSTREETMAP_VISIBILITY).setSummary(visibility);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENSTREETMAP_DESCRIPTION)){
            String description = extras.getString(PreferenceNames.OPENSTREETMAP_DESCRIPTION);
            preferenceHelper.setOSMDescription(description);
            findPreference(PreferenceNames.OPENSTREETMAP_DESCRIPTION).setSummary(description);
            return true;
        }

        if(dialogTag.equalsIgnoreCase(PreferenceNames.OPENSTREETMAP_TAGS)){
            String tags = extras.getString(PreferenceNames.OPENSTREETMAP_TAGS);
            preferenceHelper.setOSMTags(tags);
            findPreference(PreferenceNames.OPENSTREETMAP_TAGS).setSummary(tags);
            return true;
        }

        return false;
    }



}
