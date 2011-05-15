package com.mendhak.gpslogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class OSMSettingsActivity extends PreferenceActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.osmsettings);
		
		Preference resetPref = (Preference)findPreference("osm_resetauth");
		resetPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			
			public boolean onPreferenceClick(Preference preference)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.remove("osm_accesstoken");
				editor.remove("osm_accesstokensecret");
				editor.remove("osm_requesttoken");
				editor.remove("osm_requesttokensecret");
				editor.commit();
				finish();
				return true;
			}
		});

	}

}
