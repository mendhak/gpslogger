package com.mendhak.gpslogger;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GpsSettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
	}

}
