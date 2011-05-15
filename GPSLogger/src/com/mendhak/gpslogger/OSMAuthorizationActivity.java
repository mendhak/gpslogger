package com.mendhak.gpslogger;

import com.mendhak.gpslogger.helpers.SimpleCrypto;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class OSMAuthorizationActivity extends Activity implements
		OnClickListener
{

	private static OAuthProvider	provider;
	private static OAuthConsumer	consumer;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.osmauth);

		final Intent intent = getIntent();
		final String myScheme = intent.getScheme();
		final Bundle myBundle = intent.getExtras();
		final boolean inContestKey;

		if (myBundle != null)
		{
			inContestKey = myBundle.containsKey("oauth_token");
		}

		final Uri myURI = intent.getData();

		if (myURI != null && myURI.getQuery() != null
				&& myURI.getQuery().length() > 0)
		{
			//User has returned! Read the verifier info from querystring
			String oAuthVerifier = myURI.getQueryParameter("oauth_verifier");

			try
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				
				if (provider == null)
				{
					provider = Utilities.GetOSMAuthProvider(getBaseContext());
				}

				if (consumer == null)
				{
					//In case consumer is null, re-initialize from stored values.
					consumer = Utilities.GetOSMAuthConsumer(getBaseContext());
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
				startActivity(new Intent(getBaseContext(), GpsMainActivity.class));
				finish();

			}
			catch (Exception e)
			{
				Utilities.LogError("OSMAuthorizationActivity.onCreate - user has returned", e);
				Utilities.MsgBox(getString(R.string.sorry), getString(R.string.osm_auth_error), this);
			}
		}

		Button authButton = (Button) findViewById(R.id.btnAuthorizeOSM);
		authButton.setOnClickListener(this);

	}

	public void onClick(View v)
	{
		try
		{
			//User clicks. Set the consumer and provider up.
			consumer = Utilities.GetOSMAuthConsumer(getBaseContext());
			provider = Utilities.GetOSMAuthProvider(getBaseContext());

			String authUrl;

			//Get the request token and request token secret
			authUrl = provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);

			//Save for later
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("osm_requesttoken", consumer.getToken());
			editor.putString("osm_requesttokensecret",consumer.getTokenSecret());
			editor.commit();

			//Open browser, send user to OpenStreetMap.org
			Uri uri = Uri.parse(authUrl);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);

		}
		catch (Exception e)
		{
			Utilities.LogError("OSMAuthorizationActivity.onClick", e);
			Utilities.MsgBox(getString(R.string.sorry), getString(R.string.osm_auth_error), this);
		}

	}

}
