package com.mendhak.gpslogger.helpers;

import java.io.File;
import android.app.ProgressDialog;
import android.os.Environment;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.Utilities;

public class AutoEmailHelper implements IAutoSendHelper
{

	ProgressDialog pd;
	GpsMainActivity mainActivity;

	public AutoEmailHelper(GpsMainActivity activity)
	{
		this.mainActivity = activity;
	}

	public void SendLogFile(String currentFileName, String personId)
	{

		pd = new ProgressDialog(mainActivity, ProgressDialog.STYLE_HORIZONTAL);
		pd.setMax(100);
		pd.setIndeterminate(true);
		pd = ProgressDialog.show(mainActivity, mainActivity.getString(R.string.autoemail_sending),
				mainActivity.getString(R.string.please_wait), true, true);

		Thread t = new Thread(new AutoSendHandler(currentFileName, personId, this));
		t.start();

	}

	public void OnRelay(boolean connectionSuccess, String errorMessage)
	{
		pd.dismiss();
		if(errorMessage != null && errorMessage.length()>0 && errorMessage.indexOf("faultstring")>0)
		{
			String errorCode = errorMessage.substring(errorMessage.indexOf("xml:lang=\"en-US\">") + 17,
					errorMessage.indexOf("</faultstring>"));
			
			if(errorCode.equalsIgnoreCase("toomanyemails"))
			{
				mainActivity.handler.post(mainActivity.updateResultsEmailSendError);
			}
			
			if(errorCode.equalsIgnoreCase("invalidgpx"))
			{
				mainActivity.handler.post(mainActivity.updateResultsBadGPX);
			}	
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

		String[] files = new String[] {foundFile.getAbsolutePath()};
		File zipFile = new File(gpxFolder.getPath(), currentFileName +".zip");

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
					+ "xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<a:FileName>" + zipFile.getName() + "</a:FileName>"
					+ "<a:PersonId>"
					+ personId + "</a:PersonId>" + "<a:XmlBody>"
					+ Utilities.EncodeHTML(base64Contents)
					+ "</a:XmlBody></body></RelayFile></s:Body></s:Envelope>";

			Utilities.LogInfo("Posting...");
			String result = Utilities.PostUrl(Utilities.GetEmailBaseUrl() +"/basic", postBody,
					"http://tempuri.org/IEmailService/RelayFile");

			helper.OnRelay(true, result);
		}
		catch (Exception e)
		{
			helper.OnRelay(false, e.getMessage());
		}

	}

}
