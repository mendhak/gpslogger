package com.mendhak.gpslogger.common;

import android.app.Application;

public class AppSettings extends Application
{
	// ---------------------------------------------------
	// User Preferences
	// ---------------------------------------------------
	private static boolean useImperial = false;
	private static boolean newFileOnceADay;
	private static boolean preferCellTower;
	private static boolean useSatelliteTime;
	private static boolean logToKml;
	private static boolean logToGpx;
	private static boolean showInNotificationBar;
	private static int minimumDistance;
	private static int minimumSeconds;
	private static String newFileCreation;
	private static Float autoEmailDelay = 0f;
	private static boolean autoEmailEnabled = false;
	private static String smtpServer;
	private static String smtpPort;
	private static String smtpUsername;
	private static String smtpPassword;
	private static String autoEmailTarget;
	private static boolean smtpSsl;

	

	/**
	 * @return the useImperial
	 */
	public static boolean shouldUseImperial()
	{
		return useImperial;
	}

	/**
	 * @param useImperial
	 *            the useImperial to set
	 */
	public static void setUseImperial(boolean useImperial)
	{
		AppSettings.useImperial = useImperial;
	}

	/**
	 * @return the newFileOnceADay
	 */
	public static boolean shouldCreateNewFileOnceADay()
	{
		return newFileOnceADay;
	}

	/**
	 * @param newFileOnceADay
	 *            the newFileOnceADay to set
	 */
	public static void setNewFileOnceADay(boolean newFileOnceADay)
	{
		AppSettings.newFileOnceADay = newFileOnceADay;
	}

	/**
	 * @return the preferCellTower
	 */
	public static boolean shouldPreferCellTower()
	{
		return preferCellTower;
	}

	/**
	 * @param preferCellTower
	 *            the preferCellTower to set
	 */
	public static void setPreferCellTower(boolean preferCellTower)
	{
		AppSettings.preferCellTower = preferCellTower;
	}

	/**
	 * @return the useSatelliteTime
	 */
	public static boolean shouldUseSatelliteTime()
	{
		return useSatelliteTime;
	}

	/**
	 * @param useSatelliteTime
	 *            the useSatelliteTime to set
	 */
	public static void setUseSatelliteTime(boolean useSatelliteTime)
	{
		AppSettings.useSatelliteTime = useSatelliteTime;
	}

	/**
	 * @return the logToKml
	 */
	public static boolean shouldLogToKml()
	{
		return logToKml;
	}

	/**
	 * @param logToKml
	 *            the logToKml to set
	 */
	public static void setLogToKml(boolean logToKml)
	{
		AppSettings.logToKml = logToKml;
	}

	/**
	 * @return the logToGpx
	 */
	public static boolean shouldLogToGpx()
	{
		return logToGpx;
	}

	/**
	 * @param logToGpx
	 *            the logToGpx to set
	 */
	public static void setLogToGpx(boolean logToGpx)
	{
		AppSettings.logToGpx = logToGpx;
	}

	/**
	 * @return the showInNotificationBar
	 */
	public static boolean shouldShowInNotificationBar()
	{
		return showInNotificationBar;
	}

	/**
	 * @param showInNotificationBar
	 *            the showInNotificationBar to set
	 */
	public static void setShowInNotificationBar(boolean showInNotificationBar)
	{
		AppSettings.showInNotificationBar = showInNotificationBar;
	}


	/**
	 * @return the minimumDistance
	 */
	public static int getMinimumDistance()
	{
		return minimumDistance;
	}

	/**
	 * @param minimumDistance
	 *            the minimumDistance to set
	 */
	public static void setMinimumDistance(int minimumDistance)
	{
		AppSettings.minimumDistance = minimumDistance;
	}

	/**
	 * @return the minimumSeconds
	 */
	public static int getMinimumSeconds()
	{
		return minimumSeconds;
	}

	/**
	 * @param minimumSeconds
	 *            the minimumSeconds to set
	 */
	public static void setMinimumSeconds(int minimumSeconds)
	{
		AppSettings.minimumSeconds = minimumSeconds;
	}

	/**
	 * @return the newFileCreation
	 */
	public static String getNewFileCreation()
	{
		return newFileCreation;
	}

	/**
	 * @param newFileCreation
	 *            the newFileCreation to set
	 */
	public static void setNewFileCreation(String newFileCreation)
	{
		AppSettings.newFileCreation = newFileCreation;
	}

	
	/**
	 * @return the autoEmailDelay
	 */
	public static Float getAutoEmailDelay()
	{
		if(autoEmailDelay >= 8f)
		{
			return 8f;
		}
		else
		{
			return autoEmailDelay;	
		}
		

	}

	/**
	 * @param autoEmailDelay
	 *            the autoEmailDelay to set
	 */
	public static void setAutoEmailDelay(Float autoEmailDelay)
	{
		
		if(autoEmailDelay >= 8f)
		{
			AppSettings.autoEmailDelay = 8f;
		}
		else
		{
			AppSettings.autoEmailDelay = autoEmailDelay;	
		}
		
		
	}

	/**
	 * @return the autoEmailEnabled
	 */
	public static boolean isAutoEmailEnabled()
	{
		return autoEmailEnabled;
	}

	/**
	 * @param autoEmailEnabled
	 *            the autoEmailEnabled to set
	 */
	public static void setAutoEmailEnabled(boolean autoEmailEnabled)
	{
		AppSettings.autoEmailEnabled = autoEmailEnabled;
	}
	

	public static void setSmtpServer(String smtpServer) 
	{
		AppSettings.smtpServer = smtpServer;
	}

	public static String getSmtpServer() 
	{
		return smtpServer;
	}

	public static void setSmtpPort(String smtpPort) 
	{
		AppSettings.smtpPort = smtpPort;
	}

	public static String getSmtpPort() 
	{
		return smtpPort;
	}

	public static void setSmtpUsername(String smtpUsername) 
	{
		AppSettings.smtpUsername = smtpUsername;
	}

	public static String getSmtpUsername()
	{
		return smtpUsername;
	}

	public static void setSmtpPassword(String smtpPassword) 
	{
		AppSettings.smtpPassword = smtpPassword;
	}

	public static String getSmtpPassword() 
	{
		return smtpPassword;
	}

	public static void setSmtpSsl(boolean smtpSsl) {
		AppSettings.smtpSsl = smtpSsl;
	}

	public static boolean isSmtpSsl() 
	{
		return smtpSsl;
	}

	public static void setAutoEmailTarget(String autoEmailTarget) {
		AppSettings.autoEmailTarget = autoEmailTarget;
	}

	public static String getAutoEmailTarget() {
		return autoEmailTarget;
	}



}
