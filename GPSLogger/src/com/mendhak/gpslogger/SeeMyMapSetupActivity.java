package com.mendhak.gpslogger;

import com.mendhak.gpslogger.helpers.SeeMyMapSetupHelper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SeeMyMapSetupActivity extends Activity implements OnClickListener
{

	public String guid;
	public final Handler handler = new Handler();

	public final Runnable updateResultsConnectionFailure = new Runnable()
	{
		public void run()
		{
			ThereWasAnError();
		}
	};

	public final Runnable updateResultsNotAvailable = new Runnable()
	{
		public void run()
		{
			NotAvailable();
		}
	};

	public final Runnable updateResultsUrlRequest = new Runnable()
	{
		public void run()
		{
			SaveSubdomainInfo();
		}

	};

	private void NotAvailable()
	{
		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		txtRequestUrl.setText("");

		Utilities.MsgBox("Sorry", "That name isn't available, try another.", this);
	}

	private void ThereWasAnError()
	{
		Utilities.MsgBox("Can't connect", "Couldn't connect to the server. Try again later.", this);
	}

	private void SaveSubdomainInfo()
	{
		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);

		Utilities.MsgBox("Yay!", "It's yours.", this);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("seemymap_GUID", guid);
		editor.putString("seemymap_URL", txtRequestUrl.getText().toString());
		editor.putString("seemymap_Password", txtPassword.getText().toString());
		editor.commit();

		ShowSummary();

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.seemymapsetup);

		Button btnCheck = (Button) findViewById(R.id.btnCheck);
		btnCheck.setOnClickListener(this);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);

		String seeMyMapUrl = prefs.getString("seemymap_URL", "");

		txtRequestUrl.setText(seeMyMapUrl);
		txtPassword.setText(prefs.getString("seemymap_Password", ""));
		guid = prefs.getString("seemymap_GUID", "");

		ShowSummary();

	}

	public void ShowSummary()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String seeMyMapUrl = prefs.getString("seemymap_URL", "");
		TextView txtSummary = (TextView) findViewById(R.id.txtSeeMyMapSummary);

		if (seeMyMapUrl.length() > 0)
		{

			txtSummary.setText("You've currently registered " + seeMyMapUrl + ".seemymap.com");
		}
		else
		{
			txtSummary.setText("Use the textboxes above to register a SeeMyMap URL. You can always change your mind and register another.  Don't forget the password, as you'll need it if you want to retrieve it some day.");
		}
	}

	public void onClick(View v)
	{

		SeeMyMapSetupHelper helper = new SeeMyMapSetupHelper(this);
		helper.RequestUrl();

	}

}
