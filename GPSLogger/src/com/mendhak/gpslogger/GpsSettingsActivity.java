package com.mendhak.gpslogger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class GpsSettingsActivity extends PreferenceActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean useImperial = prefs.getBoolean("useImperial", false);

		if (!Utilities.Flag())
		{
			Preference seemymapSetup = (Preference) findPreference("seemymap_setup");
			seemymapSetup.setSummary("Available in the Pro version, coming soon");
			seemymapSetup.setEnabled(false);
		}

		final EditTextPreference distanceBeforeLogging = (EditTextPreference) findPreference("distance_before_logging");

		if (useImperial)
		{
			distanceBeforeLogging.setDialogTitle("Distance in feet");
			distanceBeforeLogging.getEditText().setHint("Enter feet (max 9999)");
		}
		else
		{
			distanceBeforeLogging.setDialogTitle("Distance in meters");
			distanceBeforeLogging.getEditText().setHint("Enter meters (max 9999)");
		}

		CheckBoxPreference imperialCheckBox = (CheckBoxPreference) findPreference("useImperial");
		imperialCheckBox.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			public boolean onPreferenceChange(Preference preference, final Object newValue)
			{

				final ProgressDialog pd = ProgressDialog.show(
						GpsSettingsActivity.this,
						"Converting...",
						"Converting minimum distance units. Values above 9999 are reset to 9999. The settings screen will now reload.",
						true, true);

				new Thread()
				{

					public void run()
					{

						try
						{
							sleep(3000); // Give user time to read the progressdialog
						}
						catch (InterruptedException e)
						{

							Log.e("Settings", e.getMessage());

						}
						boolean useImp = new Boolean(newValue.toString());

						String minimumDistanceString = prefs.getString("distance_before_logging", "0");

						int minimumDistance;

						if (minimumDistanceString != null && minimumDistanceString.length() > 0)
						{
							minimumDistance = Integer.valueOf(minimumDistanceString);
						}
						else
						{
							minimumDistance = 0;
						}

						SharedPreferences.Editor editor = prefs.edit();

						if (useImp == true)
						{
							distanceBeforeLogging.setDialogTitle("Distance in feet");
							distanceBeforeLogging.getEditText().setHint("Enter feet (max 9999)");

							minimumDistance = Utilities.MetersToFeet(minimumDistance);

						}
						else
						{
							minimumDistance = Utilities.FeetToMeters(minimumDistance);
							distanceBeforeLogging.setDialogTitle("Distance in meters");
							distanceBeforeLogging.getEditText().setHint("Enter meters (max 9999)");

						}

						if (minimumDistance >= 9999)
						{
							minimumDistance = 9999;
						}

						editor.putString("distance_before_logging", String.valueOf(minimumDistance));

						editor.commit();

						handler.post(updateResults);
					};
				}.start();

				return true;
			}

		});

		Preference enableDisablePref = (Preference) findPreference("enableDisableGps");

		enableDisablePref.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{

			public boolean onPreferenceClick(Preference preference)
			{
				startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
				return true;
			}

		});

	}

	final Handler handler = new Handler();
	final Runnable updateResults = new Runnable()
	{
		public void run()
		{
			finish();

			startActivity(getIntent());
		}

	};

}
