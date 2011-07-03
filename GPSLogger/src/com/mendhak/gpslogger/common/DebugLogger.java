package com.mendhak.gpslogger.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileLock;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import android.os.Environment;

public class DebugLogger
{
	
	public static void Write(String message)
	{
		Thread t = new Thread(new DebugLogWriter(message));
		t.start();
	}
}

class DebugLogWriter implements Runnable
{
	
	private String	message;
	
	public DebugLogWriter(String message)
	{
		this.message = message;
	}
	
	@Override
	public void run()
	{
		
		if (message == null || message.length() == 0)
		{
			return;
		}
		
		try
		{
			File gpxFolder = new File(
					Environment.getExternalStorageDirectory(), "GPSLogger");
			
			if (!gpxFolder.exists())
			{
				gpxFolder.mkdirs();
			}
			
			File logFile = new File(gpxFolder.getPath(), "debug.log");
			
			if (!logFile.exists())
			{
				logFile.createNewFile();
			}
			
			FileOutputStream logStream = new FileOutputStream(logFile, true);
			BufferedOutputStream logOutputStream = new BufferedOutputStream(
					logStream);
			FileLock lock = logStream.getChannel().lock();
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			String dateString = dateFormat.format(date);
			
			logOutputStream.write((dateString + ":" + message + "\n").getBytes());
			logOutputStream.flush();
			lock.release();
			logOutputStream.close();
		}
		catch (Throwable e)
		{
			// Nothing
		}
		
	}
	
}