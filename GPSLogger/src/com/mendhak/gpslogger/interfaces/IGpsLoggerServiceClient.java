package com.mendhak.gpslogger.interfaces;

import android.location.Location;

public interface IGpsLoggerServiceClient
{

	
	public void OnBeginGpsLogging();
	public void OnStopGpsLogging();
	
	public void OnStatusMessage(String message);
	
	public void OnLocationUpdate(Location loc);
	
	public void OnSatelliteCount(int count);
	
	public void ClearForm();
	
	
}
