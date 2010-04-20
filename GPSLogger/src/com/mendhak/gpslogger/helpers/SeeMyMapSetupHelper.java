package com.mendhak.gpslogger.helpers;

import android.app.ProgressDialog;
import android.widget.EditText;

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.SeeMyMapSetupActivity;
import com.mendhak.gpslogger.Utilities;

public class SeeMyMapSetupHelper implements ISeeMyMapSetupHelper
{
	SeeMyMapSetupActivity setupActivity;

	public SeeMyMapSetupHelper(SeeMyMapSetupActivity activity)
	{
		setupActivity = activity;
	}

	ProgressDialog pd;

	/**
	 * Checks subdomain and password input, then attempts to request the
	 * subdomain from SeeMyMap.
	 */
	public void RequestUrl()
	{

		EditText txtRequestUrl = (EditText) setupActivity.findViewById(R.id.txtRequestUrl);
		final String requestedUrl = txtRequestUrl.getText().toString();

		EditText txtPassword = (EditText) setupActivity.findViewById(R.id.txtPassword);
		final String password = txtPassword.getText().toString();

		if (Utilities.IsValidUrlAndPassword(requestedUrl, password))
		{
			pd = ProgressDialog.show(setupActivity, "Checking...", "Checking availability of "
					+ requestedUrl + ".seemymap.com", true, true);

			Thread t = new Thread(new RequestUrlHandler(requestedUrl, password, this));
			t.start();
		}
		else
		{
			Utilities.MsgBox("Cat got your keyboard?",
					"Please limit the website name and password to numbers and letters, nothing else.",
					setupActivity);
		}

	}

	public void OnUrlRequested(boolean connectionSuccess, String guid)
	{
		if (!connectionSuccess)
		{
			// Connection error
			setupActivity.handler.post(setupActivity.updateResultsConnectionFailure);
		}
		else if (guid.length() > 0)
		{
			// Guid returned, inform user
			setupActivity.guid = guid;
			setupActivity.handler.post(setupActivity.updateResultsUrlRequest);
		}
		else
		{
			// Subdomain not available
			setupActivity.handler.post(setupActivity.updateResultsNotAvailable);
		}

		pd.dismiss();

	}

}

interface ISeeMyMapSetupHelper
{
	public void OnUrlRequested(boolean connectionSuccess, String guid);
}

class RequestUrlHandler implements Runnable
{

	ISeeMyMapSetupHelper events;
	String requestedUrl;
	String password;

	public RequestUrlHandler(String requestedUrl, String password, ISeeMyMapSetupHelper events)
	{
		this.events = events;
		this.requestedUrl = requestedUrl;
		this.password = password;
	}

	public void run()
	{

		boolean success = false;
		String getMapResponse = null;
		String guid = "";

		try
		{
			getMapResponse = Utilities.GetUrl(Utilities.GetSeeMyMapRequestUrl(requestedUrl, password));
			success = true;
		}
		catch (Exception e)
		{
			success = false;
		}

		if (getMapResponse != null && getMapResponse.length() > 0)
		{
			if(getMapResponse.indexOf("/>") > 0)
			{
				success = true;
				guid = "";
			}
			else
			{
				success = true;
				guid = getMapResponse.substring(getMapResponse.indexOf('>') + 1,
						getMapResponse.lastIndexOf('<'));	
			}
			
		}

		events.OnUrlRequested(success, guid);

	}

}
