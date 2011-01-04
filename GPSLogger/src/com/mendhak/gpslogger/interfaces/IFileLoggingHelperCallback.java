package com.mendhak.gpslogger.interfaces;

import android.app.Activity;
import android.content.Context;


public interface IFileLoggingHelperCallback
{

	void SetStatus(String status);

	Context GetContext();
	
	/**
	 * Gets the current activity if applicable. 
	 * @return
	 */
	Activity GetActivity();
	
	String getString(int resId);
	
	
}
