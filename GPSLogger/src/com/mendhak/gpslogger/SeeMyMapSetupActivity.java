package com.mendhak.gpslogger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SeeMyMapSetupActivity extends Activity implements OnClickListener {

	String guid;
	final Handler handler = new Handler();
	final Runnable updateResults = new Runnable() {
		public void run() {
			SaveSubdomainInfo();
		}

	};

	private void SaveSubdomainInfo() {
		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);

		if (guid.equalsIgnoreCase("")) {

			txtRequestUrl.setText("");

			Utilities.MsgBox("Sorry",
					"That name isn't available, try another.", this);

		} else if (guid.equalsIgnoreCase("ERROR")) {
			Utilities.MsgBox("Can't connect",
					"Couldn't connect to the server. Try again later.", this);

		} else {
			Utilities.MsgBox("Yay!", "It's yours.", this);

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("seemymap_GUID", guid);
			editor
					.putString("seemymap_URL", txtRequestUrl.getText()
							.toString());
			editor.putString("seemymap_Password", txtPassword.getText()
					.toString());
			editor.commit();
		}
	}

	private void checkMap(String requestedUrl, String password) {

		String getMapResponse = null;
		
		try {
			getMapResponse = Utilities
					.GetUrl(Utilities.GetSeeMyMapRequestUrl(requestedUrl, password));
		} catch (Exception e) {

			guid = "ERROR";
		}

		if (getMapResponse == null || getMapResponse.length() == 0) {
			guid = "ERROR";
		} else if (getMapResponse.endsWith("/>")) {
			// Already taken or connection timed out
			guid = "";
		} else {
			guid = getMapResponse.substring(getMapResponse.indexOf('>') + 1,
					getMapResponse.lastIndexOf('<'));
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.seemymapsetup);

		Button btnCheck = (Button) findViewById(R.id.btnCheck);
		btnCheck.setOnClickListener(this);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);

		txtRequestUrl.setText(prefs.getString("seemymap_URL", ""));
		txtPassword.setText(prefs.getString("seemymap_Password", ""));
		guid = prefs.getString("seemymap_GUID", "");

	}

	public void onClick(View v) {

		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		final String requestedUrl = txtRequestUrl.getText().toString();

		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);
		final String password = txtPassword.getText().toString();

		if (ValidUrlAndPassword(requestedUrl, password)) {
			final ProgressDialog pd = ProgressDialog.show(
					SeeMyMapSetupActivity.this, "Checking...",
					"Checking availability of " + requestedUrl
							+ ".seemymap.com", true, true);

			Thread t = new Thread() {
				public void run() {
					checkMap(requestedUrl, password);
					pd.dismiss();
					handler.post(updateResults);
				}
			};
			t.start();
		} else {
			Utilities
					.MsgBox(
							"Cat got your keyboard?",
							"Please limit the website name and password to numbers and letters, nothing else.",
							this);
		}

	}

	private boolean ValidUrlAndPassword(String requestedUrl, String password) {

		requestedUrl = requestedUrl.trim();
		if (requestedUrl != null && requestedUrl.length() > 0
				&& requestedUrl.matches("[a-zA-Z0-9]+") && password != null
				&& password.length() > 0 && password.matches("[a-zA-Z0-9]+")) {
			return true;
		}

		return false;
	}

}
