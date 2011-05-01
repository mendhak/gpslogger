package com.mendhak.gpslogger;


import android.os.Bundle;
import android.preference.PreferenceActivity;

public class OSMSettingsActivity extends PreferenceActivity
{
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//addPreferencesFromResource(R.xml.autoemailsettings);
//		/**** PURE TEST CODE ***/
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.osmauth);
//
//		final Intent intent = getIntent();
//		final String myScheme = intent.getScheme();
//		final Bundle myBundle = intent.getExtras();
//		final boolean inContestKey;
//
//		if (myBundle != null)
//		{
//			inContestKey = myBundle.containsKey("a");
//		}
//
//		final Uri myURI = intent.getData();
//
//		final String value;
//		if (myURI != null)
//		{
//			String queryString = myURI.getQuery();
//			value = myURI.getQueryParameter("a");
//		}
	}
	
}
