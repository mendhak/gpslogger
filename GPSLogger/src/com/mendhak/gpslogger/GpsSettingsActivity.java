package com.mendhak.gpslogger;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class GpsSettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		// enableDisableGps

	
		if(!Utilities.Flag())
		{
			Preference seemymapSetup = (Preference)findPreference("seemymap_setup");
			seemymapSetup.setSummary("Available in the Pro version, coming soon");
			seemymapSetup.setEnabled(false);
		}
		
		Preference enableDisablePref = (Preference) findPreference("enableDisableGps");
		
		enableDisablePref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						startActivity(new Intent(
								"android.settings.LOCATION_SOURCE_SETTINGS"));
						return true;
					}

				});
		
		
	}

}
