package com.mendhak.gpslogger.model;

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
	private static String subdomain;
	private static int minimumDistance;
	private static int minimumSeconds;
	private static String newFileCreation;
	private static String seeMyMapUrl;
	private static String seeMyMapGuid;
	private static Float autoEmailDelay = 0f;
	private static boolean autoEmailEnabled = false;
	private static boolean wasRunning = false;

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
	 * @return the subdomain
	 */
	public static String getSubdomain()
	{
		return subdomain;
	}

	/**
	 * @param subdomain
	 *            the subdomain to set
	 */
	public static void setSubdomain(String subdomain)
	{
		AppSettings.subdomain = subdomain;
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
	 * @return the seeMyMapUrl
	 */
	public static String getSeeMyMapUrl()
	{
		return seeMyMapUrl;
	}

	/**
	 * @param seeMyMapUrl
	 *            the seeMyMapUrl to set
	 */
	public static void setSeeMyMapUrl(String seeMyMapUrl)
	{
		AppSettings.seeMyMapUrl = seeMyMapUrl;
	}

	/**
	 * @return the seeMyMapGuid
	 */
	public static String getSeeMyMapGuid()
	{
		return seeMyMapGuid;
	}

	/**
	 * @param seeMyMapGuid
	 *            the seeMyMapGuid to set
	 */
	public static void setSeeMyMapGuid(String seeMyMapGuid)
	{
		AppSettings.seeMyMapGuid = seeMyMapGuid;
	}

	/**
	 * @return the autoEmailDelay
	 */
	public static Float getAutoEmailDelay()
	{
		return autoEmailDelay;

	}

	/**
	 * @param autoEmailDelay
	 *            the autoEmailDelay to set
	 */
	public static void setAutoEmailDelay(Float autoEmailDelay)
	{
		AppSettings.autoEmailDelay = autoEmailDelay;
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

}
