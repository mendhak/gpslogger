package com.mendhak.gpslogger.model;

import android.app.Application;
import android.location.Location;

public class Session extends Application
{

	// ---------------------------------------------------
	// Session values - updated as the app runs
	// ---------------------------------------------------
	private static boolean towerEnabled;
	private static boolean gpsEnabled;
	private static boolean isStarted;
	private static boolean isUsingGps;
	private static String currentFileName;
	private static int satellites;
	private static boolean notificationVisible;
	private static long autoEmailDelay;
	private static long latestTimeStamp;
	private static long autoEmailTimeStamp;
	private static boolean addNewTrackSegment = true;
	private static Location currentLocationInfo;
	private static boolean isBound;

	// ---------------------------------------------------
	/**
	 * @return whether GPS (tower) is enabled
	 */
	public static boolean isTowerEnabled()
	{
		return towerEnabled;
	}

	/**
	 * @param towerEnabled
	 *            set whether GPS (tower) is enabled
	 */
	public static void setTowerEnabled(boolean towerEnabled)
	{
		Session.towerEnabled = towerEnabled;
	}

	/**
	 * @return whether GPS (satellite) is enabled
	 */
	public static boolean isGpsEnabled()
	{
		return gpsEnabled;
	}

	/**
	 * @param gpsEnabled
	 *            set whether GPS (satellite) is enabled
	 */
	public static void setGpsEnabled(boolean gpsEnabled)
	{
		Session.gpsEnabled = gpsEnabled;
	}

	/**
	 * @return whether logging has started
	 */
	public static boolean isStarted()
	{
		return isStarted;
	}

	/**
	 * @param isStarted
	 *            set whether logging has started
	 */
	public static void setStarted(boolean isStarted)
	{
		Session.isStarted = isStarted;
		
	}

	/**
	 * @return the isUsingGps
	 */
	public static boolean isUsingGps()
	{
		return isUsingGps;
	}

	/**
	 * @param isUsingGps
	 *            the isUsingGps to set
	 */
	public static void setUsingGps(boolean isUsingGps)
	{
		Session.isUsingGps = isUsingGps;
	}

	/**
	 * @return the currentFileName (without extension)
	 */
	public static String getCurrentFileName()
	{
		return currentFileName;
	}

	/**
	 * @param currentFileName
	 *            the currentFileName to set
	 */
	public static void setCurrentFileName(String currentFileName)
	{
		Session.currentFileName = currentFileName;
	}

	/**
	 * @return the number of satellites visible
	 */
	public static int getSatelliteCount()
	{
		return satellites;
	}

	/**
	 * @param satellites
	 *            sets the number of visible satellites
	 */
	public static void setSatelliteCount(int satellites)
	{
		Session.satellites = satellites;
	}

	/**
	 * @return the notificationVisible
	 */
	public static boolean isNotificationVisible()
	{
		return notificationVisible;
	}

	/**
	 * @param notificationVisible
	 *            the notificationVisible to set
	 */
	public static void setNotificationVisible(boolean notificationVisible)
	{
		Session.notificationVisible = notificationVisible;
	}

	/**
	 * @return the currentLatitude
	 */
	public static double getCurrentLatitude()
	{
		if (getCurrentLocationInfo() != null)
		{
			return getCurrentLocationInfo().getLatitude();
		}
		else
		{
			return 0;
		}
	}
	/**
	 * Determines whether a valid location is available
	 */
	public static boolean hasValidLocation()
	{
		return (getCurrentLocationInfo() != null 
				&& getCurrentLatitude() != 0 
				&& getCurrentLongitude() != 0);
	}
	
	/**
	 * @return the currentLongitude
	 */
	public static double getCurrentLongitude()
	{
		if (getCurrentLocationInfo() != null)
		{
			return getCurrentLocationInfo().getLongitude();
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @return the latestTimeStamp (for location info)
	 */
	public static long getLatestTimeStamp()
	{
		return latestTimeStamp;
	}

	/**
	 * @param latestTimeStamp
	 *            the latestTimeStamp (for location info) to set
	 */
	public static void setLatestTimeStamp(long latestTimeStamp)
	{
		Session.latestTimeStamp = latestTimeStamp;
	}

	/**
	 * @return the autoEmailTimeStamp
	 */
	public static long getAutoEmailTimeStamp()
	{
		return autoEmailTimeStamp;
	}

	/**
	 * @param autoEmailTimeStamp
	 *            the autoEmailTimeStamp to set
	 */
	public static void setAutoEmailTimeStamp(long autoEmailTimeStamp)
	{
		Session.autoEmailTimeStamp = autoEmailTimeStamp;
	}

	/**
	 * @return whether to create a new track segment
	 */
	public static boolean shouldAddNewTrackSegment()
	{
		return addNewTrackSegment;
	}

	/**
	 * @param addNewTrackSegment
	 *            set whether to create a new track segment
	 */
	public static void setAddNewTrackSegment(boolean addNewTrackSegment)
	{
		Session.addNewTrackSegment = addNewTrackSegment;
	}

	/**
	 * @param autoEmailDelay
	 *            the autoEmailDelay to set
	 */
	public static void setAutoEmailDelay(long autoEmailDelay)
	{
		Session.autoEmailDelay = autoEmailDelay;
	}

	/**
	 * @return the autoEmailDelay to use for the timer
	 */
	public static long getAutoEmailDelay()
	{
		return autoEmailDelay;
	}

	/**
	 * @param currentLocationInfo
	 *            the latest Location class
	 */
	public static void setCurrentLocationInfo(Location currentLocationInfo)
	{
		Session.currentLocationInfo = currentLocationInfo;
	}

	/**
	 * @return the Location class containing latest lat-long information
	 */
	public static Location getCurrentLocationInfo()
	{
		return currentLocationInfo;
	}

	/**
	 * @param isBound
	 *            set whether the activity is bound to the GpsLoggingService
	 */
	public static void setBoundToService(boolean isBound)
	{
		Session.isBound = isBound;
	}

	/**
	 * @return whether the activity is bound to the GpsLoggingService
	 */
	public static boolean isBoundToService()
	{
		return isBound;
	}

}
