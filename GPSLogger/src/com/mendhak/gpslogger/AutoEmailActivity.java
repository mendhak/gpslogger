package com.mendhak.gpslogger;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;
import android.widget.TimePicker;

import com.mendhak.gpslogger.helpers.AutoEmailSetupHelper;
import com.mendhak.gpslogger.interfaces.IMessageBoxCallback;
import com.mendhak.gpslogger.R;

public class AutoEmailActivity extends PreferenceActivity implements OnPreferenceChangeListener,
		IMessageBoxCallback
{

	String initialEmailAddress;
	public final Handler handler = new Handler();


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.autoemailsettings);

		CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");
		ToggleElements(chkEnabled.isChecked());

		chkEnabled.setOnPreferenceChangeListener(this);

		EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");
		initialEmailAddress = txtTarget.getText();
	}
	
	

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");
			EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");

			if (chkEnabled.isChecked()
					&& (initialEmailAddress == null 
							|| !initialEmailAddress.equalsIgnoreCase(txtTarget.getText())))
			{
				RegisterEmailSettings();
			}
			else
			{
				finish();
			}
			return false;
		}
		else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	public final Runnable updateResultsSettingsRegistered = new Runnable()
	{
		public void run()
		{
			TargetEmailSaved();
		}
	};
	public final Runnable updateResultsConnectionFail = new Runnable()
	{
		public void run()
		{
			CouldNotSaveDetails();
		}
	};

	private void CouldNotSaveDetails()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("autoemail_target", initialEmailAddress);
		editor.commit();
		Utilities.MsgBox(getString(R.string.sorry), getString(R.string.error_connection), this, this);
	}

	public void MessageBoxResult(int which)
	{
		finish();
	}

	private void TargetEmailSaved()
	{
		Utilities.MsgBox(getString(R.string.success), getString(R.string.autoemail_success), this, this);
	}

	private void RegisterEmailSettings()
	{
		AutoEmailSetupHelper helper = new AutoEmailSetupHelper(this);
		helper.RegisterEmailSettings();
	}

	private void ToggleElements(boolean enabled)
	{
		EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");
		txtTarget.setEnabled(enabled);

		Preference lstFrequency = (Preference) findPreference("autoemail_frequency");
		lstFrequency.setEnabled(enabled);
	}

	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		if (preference.getKey().equalsIgnoreCase("autoemail_enabled"))
		{
			boolean enabled = new Boolean(newValue.toString());
			ToggleElements(enabled);
		}
		return true;
	}
}
