package com.mendhak.gpslogger.interfaces;

import android.content.Context;
import android.location.Location;

public interface IGpsLoggerServiceClient
{

	/**
	 * Beginning GPS logging
	 */
	public void OnBeginGpsLogging();
	
	/**
	 * Stopping GPS logging
	 */
	public void OnStopGpsLogging();
	
	/**
	 * New message from the service to be displayed on the activity form.
	 * @param message
	 */
	public void OnStatusMessage(String message);
	
	/**
	 * A new location fix has been obtained.
	 * @param loc
	 */
	public void OnLocationUpdate(Location loc);
	
	/**
	 * New satellite count has been obtained.
	 * @param count
	 */
	public void OnSatelliteCount(int count);
	
	/**
	 * Asking the calling activity form to clear itself.
	 */
	public void ClearForm();
	
	/**
	 * Returns the base context 
	 * @return
	 */
	public Context GetContext();

	/**
	 * A new current file name is available.
	 * @param newFileName
	 */
	public void onFileName(String newFileName);

	

}
