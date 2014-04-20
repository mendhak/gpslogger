/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.senders.osm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import org.slf4j.LoggerFactory;

public class OSMAuthorizationActivity extends PreferenceActivity {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OSMAuthorizationActivity.class.getSimpleName());
    private static OAuthProvider provider;
    private static OAuthConsumer consumer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.osmsettings);

        final Intent intent = getIntent();
        final Uri myURI = intent.getData();

        if (myURI != null && myURI.getQuery() != null
                && myURI.getQuery().length() > 0) {
            //User has returned! Read the verifier info from querystring
            String oAuthVerifier = myURI.getQueryParameter("oauth_verifier");

            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                if (provider == null) {
                    provider = OSMHelper.GetOSMAuthProvider(getApplicationContext());
                }

                if (consumer == null) {
                    //In case consumer is null, re-initialize from stored values.
                    consumer = OSMHelper.GetOSMAuthConsumer(getApplicationContext());
                }

                //Ask OpenStreetMap for the access token. This is the main event.
                provider.retrieveAccessToken(consumer, oAuthVerifier);

                String osmAccessToken = consumer.getToken();
                String osmAccessTokenSecret = consumer.getTokenSecret();

                //Save for use later.
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("osm_accesstoken", osmAccessToken);
                editor.putString("osm_accesstokensecret", osmAccessTokenSecret);
                editor.commit();

                //Now go away
                startActivity(new Intent(getApplicationContext(), GpsMainActivity.class));
                finish();

            } catch (Exception e) {
                tracer.error("OSMAuthorizationActivity.onCreate - user has returned", e);
                Utilities.MsgBox(getString(R.string.sorry), getString(R.string.osm_auth_error), this);
            }
        }


        Preference visibilityPref = findPreference("osm_visibility");
        Preference descriptionPref = findPreference("osm_description");
        Preference tagsPref = findPreference("osm_tags");
        Preference resetPref = findPreference("osm_resetauth");

        if (!OSMHelper.IsOsmAuthorized(getApplicationContext())) {
            resetPref.setTitle(R.string.osm_lbl_authorize);
            resetPref.setSummary(R.string.osm_lbl_authorize_description);
            visibilityPref.setEnabled(false);
            descriptionPref.setEnabled(false);
            tagsPref.setEnabled(false);
        } else {
            visibilityPref.setEnabled(true);
            descriptionPref.setEnabled(true);
            tagsPref.setEnabled(true);

        }


        resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {

                if (OSMHelper.IsOsmAuthorized(getApplicationContext())) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("osm_accesstoken");
                    editor.remove("osm_accesstokensecret");
                    editor.remove("osm_requesttoken");
                    editor.remove("osm_requesttokensecret");
                    editor.commit();
                    startActivity(new Intent(getApplicationContext(), GpsMainActivity.class));
                    finish();

                } else {
                    try {
                        StrictMode.enableDefaults();

                        //User clicks. Set the consumer and provider up.
                        consumer = OSMHelper.GetOSMAuthConsumer(getApplicationContext());
                        provider = OSMHelper.GetOSMAuthProvider(getApplicationContext());

                        String authUrl;

                        //Get the request token and request token secret
                        authUrl = provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);

                        //Save for later
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("osm_requesttoken", consumer.getToken());
                        editor.putString("osm_requesttokensecret", consumer.getTokenSecret());
                        editor.commit();

                        //Open browser, send user to OpenStreetMap.org
                        Uri uri = Uri.parse(authUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);

                    } catch (Exception e) {
                        tracer.error("OSMAuthorizationActivity.onClick", e);
                        Utilities.MsgBox(getString(R.string.sorry), getString(R.string.osm_auth_error),
                                OSMAuthorizationActivity.this);
                    }
                }

                return true;


            }
        });

//        Button authButton = (Button) findViewById(R.id.btnAuthorizeOSM);
//        authButton.setOnClickListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
