package com.mendhak.gpslogger.helpers;

import java.io.File;
import android.app.ProgressDialog;
import android.os.Environment;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.Utilities;
import com.mendhak.gpslogger.model.AppSettings;
import com.mendhak.gpslogger.model.Session;

public class AutoEmailHelper implements IAutoSendHelper
{

	ProgressDialog pd;
	GpsLoggingService mainActivity;

	public AutoEmailHelper(GpsLoggingService activity)
	{
		this.mainActivity = activity;
	}

	public void SendLogFile(String currentFileName, String personId)
	{

		try
		{
			if (mainActivity.IsMainFormVisible())
			{
				pd = new ProgressDialog(mainActivity, ProgressDialog.STYLE_HORIZONTAL);
				pd.setMax(100);
				pd.setIndeterminate(true);

				pd = ProgressDialog.show((GpsMainActivity) GpsLoggingService.mainServiceClient,
						mainActivity.getString(R.string.autoemail_sending),
						mainActivity.getString(R.string.please_wait), true, true);

			}	
		}
		catch(Exception ex)
		{
			//	Swallow exception	
		}
		
		

		Thread t = new Thread(new AutoSendHandler(currentFileName, personId, this));
		t.start();

	}

	public void OnRelay(boolean connectionSuccess, String errorMessage)
	{
		try
		{
			if (pd != null)
			{
				pd.dismiss();
			}	
		}
		catch(Exception ex)
		{
			//swallow exception
		}
		
		if (errorMessage != null && errorMessage.length() > 0 && errorMessage.indexOf("faultstring") > 0)
		{
			//If it contains "faultstring", there are errors.
			String errorCode = errorMessage.substring(errorMessage.indexOf("xml:lang=\"en-US\">") + 17,
					errorMessage.indexOf("</faultstring>"));

			if (errorCode.equalsIgnoreCase("toomanyemails"))
			{
				mainActivity.handler.post(mainActivity.updateResultsEmailSendError);
			}

			if (errorCode.equalsIgnoreCase("invalidgpx"))
			{
				mainActivity.handler.post(mainActivity.updateResultsBadGPX);
			}

		}
		else
		{
			//This was a success
			Utilities.LogInfo("Email sent");
			Utilities.LogDebug("setEmailReadyToBeSent = false");
			Session.setEmailReadyToBeSent(false);
			Session.setAutoEmailTimeStamp(System.currentTimeMillis());
		}

	}
}

interface IAutoSendHelper
{
	public void OnRelay(boolean connectionSuccess, String errorMessage);
}

class AutoSendHandler implements Runnable
{

	String currentFileName;
	String personId;
	IAutoSendHelper helper;

	public AutoSendHandler(String currentFileName, String personId, IAutoSendHelper helper)
	{
		this.currentFileName = currentFileName;
		this.personId = personId;
		this.helper = helper;
	}

	public void run()
	{
		File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

		if (!gpxFolder.exists())
		{
			helper.OnRelay(true, null);
			return;
		}

		File gpxFile = new File(gpxFolder.getPath(), currentFileName + ".gpx");
		File kmlFile = new File(gpxFolder.getPath(), currentFileName + ".kml");

		File foundFile = null;

		if (kmlFile.exists())
		{
			foundFile = kmlFile;
		}
		if (gpxFile.exists())
		{
			foundFile = gpxFile;
		}

		if (foundFile == null)
		{
			helper.OnRelay(true, null);
			return;
		}

		String[] files = new String[] { foundFile.getAbsolutePath() };
		File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");

		try
		{

			Utilities.LogInfo("Zipping file");
			ZipHelper zh = new ZipHelper(files, zipFile.getAbsolutePath());
			zh.Zip();

			Utilities.LogInfo("Converting to byte array");
			byte[] contents = Utilities.GetBytesFromFile(zipFile);
			Utilities.LogInfo("Converting to base 64");

			String base64Contents = Utilities.GetStringFromByteArray(contents);

			String postBody = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
					+ "<s:Body><RelayFile xmlns=\"http://tempuri.org/\">"
					+ "<body xmlns:a=\"http://schemas.datacontract.org/2004/07/SeeMyMapWeb\" "
					+ "xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">" + "<a:FileName>"
					+ zipFile.getName() + "</a:FileName>" + "<a:PersonId>" + personId + "</a:PersonId>"
					+ "<a:XmlBody>" + Utilities.EncodeHTML(base64Contents)
					+ "</a:XmlBody></body></RelayFile></s:Body></s:Envelope>";

			Utilities.LogInfo("Posting...");
			String result = Utilities.PostUrl(AppSettings.getEmsu() + "/basic", postBody,
					"http://tempuri.org/IEmailService/RelayFile");

			helper.OnRelay(true, result);
		}
		catch (Exception e)
		{
			helper.OnRelay(false, e.getMessage());
		}

	}

}
