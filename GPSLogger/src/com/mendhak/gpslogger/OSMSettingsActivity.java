package com.mendhak.gpslogger;

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
