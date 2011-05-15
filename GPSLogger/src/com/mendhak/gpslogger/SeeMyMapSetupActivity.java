package com.mendhak.gpslogger;

import com.mendhak.gpslogger.R;

import com.mendhak.gpslogger.helpers.*;

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

	public String personId;
	public String guid;
	public final Handler handler = new Handler();
	
	public final Runnable updateResultsPointDeleted = new Runnable()
	{
		public void run()
		{
			PointDeleted();
		}
	};

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

	private void PointDeleted()
	{
		Utilities.MsgBox(getString(R.string.deleted), getString(R.string.point_deleted), this);
	}
	
	private void NotAvailable()
	{
		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		txtRequestUrl.setText("");

		Utilities.MsgBox(getString(R.string.sorry), getString(R.string.name_unavailable), this);
	}

	private void ThereWasAnError()
	{
		Utilities.MsgBox(getString(R.string.error), getString(R.string.error_connection), this);
	}

	private void SaveSubdomainInfo()
	{
		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);

		Utilities.MsgBox(getString(R.string.success), getString(R.string.name_available), this);

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
		
		Button btnDeleteFirstPoint = (Button)findViewById(R.id.btnClearFirstPoint);
		btnDeleteFirstPoint.setOnClickListener(this);
		
		Button btnDeleteLastPoint = (Button)findViewById(R.id.btnClearLastPoint);
		btnDeleteLastPoint.setOnClickListener(this);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		EditText txtRequestUrl = (EditText) findViewById(R.id.txtRequestUrl);
		EditText txtPassword = (EditText) findViewById(R.id.txtPassword);
		
		personId = Utilities.GetPersonId(getBaseContext());

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

			txtSummary.setText(getString(R.string.seemymap_currenturl, seeMyMapUrl));
			//txtSummary.setText("You've currently registered " + seeMyMapUrl + ".seemymap.com");
		}
		else
		{
			txtSummary.setText(getString(R.string.seemymap_introduction));
		}
	}

	public void onClick(View v)
	{

		int buttonId = v.getId();
		
		if(buttonId == R.id.btnClearFirstPoint)
		{
			SeeMyMapHelper helper = new SeeMyMapHelper(this);
			helper.DeleteFirstPoint();
		}
		else if(buttonId == R.id.btnClearLastPoint)
		{
			SeeMyMapHelper helper = new SeeMyMapHelper(this);
			helper.DeleteLastPoint();
		}
		else
		{
			SeeMyMapSetupHelper helper = new SeeMyMapSetupHelper(this);
			helper.RequestUrl();	
		}

	}

}
