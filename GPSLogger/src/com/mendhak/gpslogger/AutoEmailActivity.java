package com.mendhak.gpslogger;

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

import com.mendhak.gpslogger.helpers.AutoEmailHelper;
import com.mendhak.gpslogger.interfaces.IAutoSendHelper;
import com.mendhak.gpslogger.interfaces.IMessageBoxCallback;
import com.mendhak.gpslogger.R;

public class AutoEmailActivity extends PreferenceActivity implements
		OnPreferenceChangeListener, IMessageBoxCallback, IAutoSendHelper,
		OnPreferenceClickListener
{

	String					initialEmailAddress;
	public final Handler	handler	= new Handler();


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.autoemailsettings);

		CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");

		chkEnabled.setOnPreferenceChangeListener(this);

		ListPreference lstPresets = (ListPreference) findPreference("autoemail_preset");
		lstPresets.setOnPreferenceChangeListener(this);

		EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
		EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
		txtSmtpServer.setOnPreferenceChangeListener(this);
		txtSmtpPort.setOnPreferenceChangeListener(this);

		EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");
		initialEmailAddress = txtTarget.getText();

		Preference testEmailPref = (Preference) findPreference("smtp_testemail");

		testEmailPref.setOnPreferenceClickListener(this);

	}

	public boolean onPreferenceClick(Preference preference)
	{

		if (!IsFormValid())
		{
			Utilities.MsgBox(getString(R.string.autoemail_invalid_form),
					getString(R.string.autoemail_invalid_form_message),
					AutoEmailActivity.this);
			return false;
		}

		Utilities.ShowProgress(this, getString(R.string.autoemail_sendingtest),
					getString(R.string.please_wait));

		CheckBoxPreference chkUseSsl = (CheckBoxPreference) findPreference("smtp_ssl");
		EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
		EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
		EditTextPreference txtUsername = (EditTextPreference) findPreference("smtp_username");
		EditTextPreference txtPassword = (EditTextPreference) findPreference("smtp_password");
		EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");

		AutoEmailHelper aeh = new AutoEmailHelper(null);
		aeh.SendTestEmail(txtSmtpServer.getText(), txtSmtpPort.getText(),
				txtUsername.getText(), txtPassword.getText(),
				chkUseSsl.isChecked(), txtTarget.getText(),
				AutoEmailActivity.this, AutoEmailActivity.this);
		
		return true;
	}

	private boolean IsFormValid()
	{

		CheckBoxPreference chkEnabled = (CheckBoxPreference) findPreference("autoemail_enabled");
		EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
		EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
		EditTextPreference txtUsername = (EditTextPreference) findPreference("smtp_username");
		EditTextPreference txtPassword = (EditTextPreference) findPreference("smtp_password");
		EditTextPreference txtTarget = (EditTextPreference) findPreference("autoemail_target");

		if (chkEnabled.isChecked())
		{
			if (txtSmtpServer.getText() != null
					&& txtSmtpServer.getText().length() > 0
					&& txtSmtpPort.getText() != null
					&& txtSmtpPort.getText().length() > 0
					&& txtUsername.getText() != null
					&& txtUsername.getText().length() > 0
					&& txtPassword.getText() != null
					&& txtPassword.getText().length() > 0
					&& txtTarget.getText() != null
					&& txtTarget.getText().length() > 0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}

		return true;

	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (!IsFormValid())
			{
				Utilities.MsgBox(getString(R.string.autoemail_invalid_form),
						getString(R.string.autoemail_invalid_form_message),
						this);
				return false;
			}
			else
			{
				return super.onKeyDown(keyCode, event);
			}
		}
		else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	public final Runnable	updateResultsSettingsRegistered	= new Runnable()
															{
																public void run()
																{
																	TargetEmailSaved();
																}
															};
	public final Runnable	updateResultsConnectionFail		= new Runnable()
															{
																public void run()
																{
																	CouldNotSaveDetails();
																}
															};

	private void CouldNotSaveDetails()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("autoemail_target", initialEmailAddress);
		editor.commit();
		Utilities.MsgBox(getString(R.string.sorry),
				getString(R.string.error_connection), this, this);
	}

	public void MessageBoxResult(int which)
	{
		finish();
	}

	private void TargetEmailSaved()
	{
		Utilities.MsgBox(getString(R.string.success),
				getString(R.string.autoemail_success), this, this);
	}

	public boolean onPreferenceChange(Preference preference, Object newValue)
	{

		if (preference.getKey().equals("autoemail_preset"))
		{
			int newPreset = Integer.valueOf(newValue.toString());

			switch (newPreset)
			{
				case 0:
					// Gmail
					SetSmtpValues("smtp.gmail.com", "465", true);
					break;
				case 1:
					// Windows live mail
					SetSmtpValues("smtp.live.com", "587", false);
					break;
				case 2:
					// Yahoo
					SetSmtpValues("smtp.mail.yahoo.com", "465", true);
					break;
				case 99:
					// manual
					break;
			}

		}

		return true;
	}

	private void SetSmtpValues(String server, String port, boolean useSsl)
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = prefs.edit();

		EditTextPreference txtSmtpServer = (EditTextPreference) findPreference("smtp_server");
		EditTextPreference txtSmtpPort = (EditTextPreference) findPreference("smtp_port");
		CheckBoxPreference chkUseSsl = (CheckBoxPreference) findPreference("smtp_ssl");

		// Yahoo
		txtSmtpServer.setText(server);
		editor.putString("smtp_server", server);
		txtSmtpPort.setText(port);
		editor.putString("smtp_port", port);
		chkUseSsl.setChecked(useSsl);
		editor.putBoolean("smtp_ssl", useSsl);

		editor.commit();

	}

	String	testResults;

	public void OnRelay(boolean connectionSuccess, String message)
	{

		testResults = message;

		handler.post(showTestResults);
	}

	public final Runnable	showTestResults	= new Runnable()
											{
												public void run()
												{
													TestEmailResults();
												}
											};

	public void TestEmailResults()
	{
		Utilities.HideProgress();
		Utilities.MsgBox(getString(R.string.autoemail_testresult_title),
				testResults, this);
	}

}
