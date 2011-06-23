package com.mendhak.gpslogger.helpers;

import java.io.File;
import java.util.Date;

import android.app.Activity;
import android.os.Environment;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.Utilities;
import com.mendhak.gpslogger.interfaces.IAutoSendHelper;
import com.mendhak.gpslogger.model.AppSettings;
import com.mendhak.gpslogger.model.Session;

public class AutoEmailHelper implements IAutoSendHelper
{

	private GpsLoggingService	mainActivity;
	private boolean				forcedSend	= false;

	public AutoEmailHelper(GpsLoggingService activity)
	{
		this.mainActivity = activity;
	}

	public void SendLogFile(String currentFileName, boolean forcedSend)
	{
		this.forcedSend = forcedSend;

		Thread t = new Thread(new AutoSendHandler(currentFileName, this));
		t.start();

	}

	public void SendTestEmail(String smtpServer, String smtpPort,
			String smtpUsername, String smtpPassword, boolean smtpUseSsl,
			String emailTarget, Activity callingActivity, IAutoSendHelper helper)
	{

		Thread t = new Thread(new TestEmailHandler(helper, smtpServer,
				smtpPort, smtpUsername, smtpPassword, smtpUseSsl, emailTarget));
		t.start();
	}

	public void OnRelay(boolean connectionSuccess, String errorMessage)
	{

		if (!connectionSuccess)
		{
			mainActivity.handler.post(mainActivity.updateResultsEmailSendError);
		}
		else
		{
			// This was a success
			Utilities.LogInfo("Email sent");

			if (!forcedSend)
			{
				Utilities.LogDebug("setEmailReadyToBeSent = false");
				Session.setEmailReadyToBeSent(false);
				Session.setAutoEmailTimeStamp(System.currentTimeMillis());
			}
		}

	}
}

class AutoSendHandler implements Runnable
{

	private String			currentFileName;
	private IAutoSendHelper	helper;

	public AutoSendHandler(String currentFileName,	IAutoSendHelper helper)
	{
		this.currentFileName = currentFileName;
		this.helper = helper;
	}

	public void run()
	{
		File gpxFolder = new File(Environment.getExternalStorageDirectory(),
				"GPSLogger");

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

		String[] files = new String[]
		{ foundFile.getAbsolutePath() };
		File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");

		try
		{

			Utilities.LogInfo("Zipping file");
			ZipHelper zh = new ZipHelper(files, zipFile.getAbsolutePath());
			zh.Zip();

			Mail m = new Mail(AppSettings.getSmtpUsername(),
					AppSettings.getSmtpPassword());

			String[] toArr =
			{ AppSettings.getAutoEmailTarget() };
			m.setTo(toArr);
			m.setFrom(AppSettings.getSmtpUsername());
			m.setSubject("GPS Log file generated at "
					+ Utilities.GetReadableDateTime(new Date()) + " - "
					+ zipFile.getName());
			m.setBody(zipFile.getName());
			

			m.setPort(AppSettings.getSmtpPort());
			m.setSecurePort(AppSettings.getSmtpPort());
			m.setSmtpHost(AppSettings.getSmtpServer());
			m.setSsl(AppSettings.isSmtpSsl());
			m.addAttachment(zipFile.getAbsolutePath());

			Utilities.LogInfo("Sending email...");

			if (m.send())
			{
				helper.OnRelay(true, "Email was sent successfully.");
			}
			else
			{
				helper.OnRelay(false, "Email was not sent.");
			}
		}
		catch (Exception e)
		{
			helper.OnRelay(false, e.getMessage());
			Utilities.LogError("AutoSendHandler.run", e);
		}

	}

}

class TestEmailHandler implements Runnable
{
	String			smtpServer;
	String			smtpPort;
	String			smtpUsername;
	String			smtpPassword;
	boolean			smtpUseSsl;
	String			emailTarget;
	IAutoSendHelper	helper;

	public TestEmailHandler(IAutoSendHelper helper, String smtpServer,
			String smtpPort, String smtpUsername, String smtpPassword,
			boolean smtpUseSsl, String emailTarget)
	{
		this.smtpServer = smtpServer;
		this.smtpPort = smtpPort;
		this.smtpPassword = smtpPassword;
		this.smtpUsername = smtpUsername;
		this.smtpUseSsl = smtpUseSsl;
		this.emailTarget = emailTarget;
		this.helper = helper;
	}

	public void run()
	{
		try
		{

			Mail m = new Mail(smtpUsername, smtpPassword);

			String[] toArr =
			{ emailTarget };
			m.setTo(toArr);
			m.setFrom(smtpUsername);
			m.setSubject("Test Email from GPSLogger at "
					+ Utilities.GetReadableDateTime(new Date()));
			m.setBody("Test Email from GPSLogger at "
					+ Utilities.GetReadableDateTime(new Date()));

			m.setPort(smtpPort);
			m.setSecurePort(smtpPort);
			m.setSmtpHost(smtpServer);
			m.setSsl(smtpUseSsl);
			m.setDebuggable(true);

			Utilities.LogInfo("Sending email...");
			if (m.send())
			{
				helper.OnRelay(true, "Email was sent successfully.");
			}
			else
			{
				helper.OnRelay(false, "Email was not sent.");
			}
		}
		catch (Exception e)
		{
			helper.OnRelay(false, e.getMessage());
			Utilities.LogError("AutoSendHandler.run", e);
		}

	}
}
