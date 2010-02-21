package com.mendhak.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.inputmethod.InputMethodManager;

public class GpsSettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		//enableDisableGps
		
		final Preference distPref = (Preference) findPreference("distance_before_logging");
		distPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{


		public boolean onPreferenceClick(Preference preference) {
			  InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			  imm.showSoftInput(distPref.getView(getCurrentFocus(), getListView()), 0);
			  return false;
		}
		}
		
		);
		
		
		Preference enableDisablePref = (Preference) findPreference("enableDisableGps");
		enableDisablePref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
		{

			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
				return true;
			}
		
		}
		);
	}

}
