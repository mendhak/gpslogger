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

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.osm.OpenStreetMapManager;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import org.slf4j.Logger;

public class OSMAuthorizationFragment extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener {

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

        addPreferencesFromResource(R.xml.osmsettings);

        manager = new OpenStreetMapManager(preferenceHelper);

        final Intent intent = getActivity().getIntent();
        final Uri myURI = intent.getData();

        if (myURI != null && myURI.getQuery() != null
                && myURI.getQuery().length() > 0) {
            //User has returned! Read the verifier info from querystring

            Dialogs.progress(getActivity(), getString(R.string.please_wait), getString(R.string.please_wait));

            LOG.debug("OAuth user has returned!");
            String oAuthVerifier = myURI.getQueryParameter("oauth_verifier");

            new Thread(new OsmAuthorizationEndWorkflow(oAuthVerifier)).start();

        }


        setPreferencesState();

    }

    private void setPreferencesState() {
        Preference visibilityPref = findPreference("osm_visibility");
        Preference descriptionPref = findPreference("osm_description");
        Preference tagsPref = findPreference("osm_tags");
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
    @AskPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public boolean onPreferenceClick(Preference preference) {
        if (manager.isOsmAuthorized()) {
            preferenceHelper.setOSMAccessToken("");
            preferenceHelper.setOSMAccessTokenSecret("");
            preferenceHelper.setOSMRequestToken("");
            preferenceHelper.setOSMRequestTokenSecret("");

            startActivity(new Intent(getActivity(), GpsMainActivity.class));
            getActivity().finish();

        } else {


            //User clicks. Set the consumer and provider up.
            consumer = manager.getOSMAuthConsumer();
            provider = manager.getOSMAuthProvider();

            new Thread(new OsmAuthorizationBeginWorkflow()).start();


        }

        return true;
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
                        Dialogs.error(getString(R.string.sorry), getString(R.string.osm_auth_error), e.getMessage(), e, getActivity());
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
                        Dialogs.error(getString(R.string.sorry), getString(R.string.osm_auth_error), e.getMessage(), e,
                                getActivity());
                    }
                });
            }
        }
    }
}
