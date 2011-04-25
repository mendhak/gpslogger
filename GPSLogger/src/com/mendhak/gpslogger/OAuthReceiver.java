package com.mendhak.gpslogger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class OAuthReceiver extends Activity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		/**** PURE TEST CODE ***/
		super.onCreate(savedInstanceState);
		setContentView(R.layout.osmauth);

		final Intent intent = getIntent();
		final String myScheme = intent.getScheme();
		final Bundle myBundle = intent.getExtras();
		final boolean inContestKey;

		if (myBundle != null)
		{
			inContestKey = myBundle.containsKey("a");
		}

		final Uri myURI = intent.getData();

		final String value;
		if (myURI != null)
		{
			String queryString = myURI.getQuery();
			value = myURI.getQueryParameter("a");
		}
	}

}
