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


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.network.ConscryptProviderInstaller;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.osm.OpenStreetMapManager;
import com.mendhak.gpslogger.ui.Dialogs;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;

import org.slf4j.Logger;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

public class OSMAuthorizationFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, SimpleDialog.OnDialogResultListener {

    private static final Logger LOG = Logs.of(OSMAuthorizationFragment.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    private Handler osmHandler = new Handler();

    //Must be static - when user returns from OSM, this needs to be set already
    private static OAuthProvider provider;
    private static OAuthConsumer consumer;
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

            new Thread(new OsmAuthorizationEndWorkflow(oAuthVerifier)).start();

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

        ConscryptProviderInstaller.addConscryptPreferenceItemIfNeeded(this.getPreferenceScreen());

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


                //User clicks. Set the consumer and provider up.
                consumer = manager.getOSMAuthConsumer();
                provider = manager.getOSMAuthProvider();
                new Thread(new OsmAuthorizationBeginWorkflow()).start();
            }
            return true;
        }


        return true;
    }

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

    private class OsmAuthorizationEndWorkflow implements Runnable {

        String oAuthVerifier;

        OsmAuthorizationEndWorkflow(String oAuthVerifier) {
            this.oAuthVerifier = oAuthVerifier;
        }

        @Override
        public void run() {
            try {
                if (provider == null) {
                    provider = OpenStreetMapManager.getOSMAuthProvider();
                }

                if (consumer == null) {
                    //In case consumer is null, re-initialize from stored values.
                    consumer = OpenStreetMapManager.getOSMAuthConsumer();
                }


                //Ask OpenStreetMap for the access token. This is the main event.
                provider.retrieveAccessToken(consumer, oAuthVerifier);

                String osmAccessToken = consumer.getToken();
                String osmAccessTokenSecret = consumer.getTokenSecret();

                //Save for use later.
                preferenceHelper.setOSMAccessToken(osmAccessToken);
                preferenceHelper.setOSMAccessTokenSecret(osmAccessTokenSecret);

                osmHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Dialogs.hideProgress();
                        setPreferencesState();
                    }
                });


            } catch (final Exception e) {
                LOG.error("OSM authorization error", e);
                osmHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Dialogs.hideProgress();
                        if(getActivity()!=null && isAdded()) {
                            Dialogs.showError(getString(R.string.sorry), getString(R.string.osm_auth_error), e.getMessage(), e, (FragmentActivity) getActivity());
                        }
                    }
                });
            }
        }
    }

    private class OsmAuthorizationBeginWorkflow implements Runnable {

        @Override
        public void run() {
            try {
                String authUrl;
                //Get the request token and request token secret
                authUrl = provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);

                //Save for later
                preferenceHelper.setOSMRequestToken(consumer.getToken());
                preferenceHelper.setOSMRequestTokenSecret(consumer.getTokenSecret());


                //Open browser, send user to OpenStreetMap.org
                Uri uri = Uri.parse(authUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (final Exception e) {
                LOG.error("onClick", e);
                osmHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(getActivity()!=null && isAdded()){
                            Dialogs.showError(getString(R.string.sorry), getString(R.string.osm_auth_error), e.getMessage(), e,
                                    (FragmentActivity) getActivity());
                        }
                    }
                });
            }
        }
    }
}
