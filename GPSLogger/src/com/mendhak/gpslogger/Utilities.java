package com.mendhak.gpslogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import com.mendhak.gpslogger.helpers.Base64;
import com.mendhak.gpslogger.interfaces.IMessageBoxCallback;
import com.mendhak.gpslogger.model.AppSettings;
import com.mendhak.gpslogger.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Utilities
{

	private static final int LOGLEVEL = 5;

	public static void LogInfo(String message)
	{
		if (LOGLEVEL >= 3)
		{
			Log.i("GPSLogger", message);
		}

	}

	public static void LogError(String methodName, Exception ex)
	{
		try
		{
			LogError(methodName + ":" + ex.getMessage());
		}
		catch (Exception e)
		{
			/**/
		}
	}

	private static void LogError(String message)
	{
		Log.e("GPSLogger", message);

	}

	public static void LogDebug(String message)
	{
		if (LOGLEVEL >= 4)
		{
			Log.d("GPSLogger", message);
		}
	}

	public static void LogWarning(String message)
	{
		if (LOGLEVEL >= 2)
		{
			Log.w("GPSLogger", message);
		}
	}

	public static void LogVerbose(String message)
	{
		if (LOGLEVEL >= 5)
		{
			Log.v("GPSLogger", message);
		}
	}

	/**
	 * Gets user preferences, populates the AppSettings class. 
	 * @param context
	 * @return
	 */
	public static void PopulateAppSettings(Context context)
	{
			
		Utilities.LogInfo("Getting preferences");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		AppSettings.setUseImperial(prefs.getBoolean("useImperial", false));
		AppSettings.setUseSatelliteTime(prefs.getBoolean("satellite_time", false));

		AppSettings.setLogToKml(prefs.getBoolean("log_kml", false));

		AppSettings.setLogToGpx(prefs.getBoolean("log_gpx", false));
		
		AppSettings.setShowInNotificationBar(prefs.getBoolean("show_notification", true));

		AppSettings.setPreferCellTower(prefs.getBoolean("prefer_celltower", false));
		AppSettings.setSubdomain(prefs.getString("subdomain", "where"));
		
		String minimumDistanceString = prefs.getString("distance_before_logging", "0");

		if (minimumDistanceString != null && minimumDistanceString.length() > 0)
		{
			AppSettings.setMinimumDistance(Integer.valueOf(minimumDistanceString));
			//appSettings.minimumDistance = Integer.valueOf(minimumDistanceString);
		}
		else
		{
			AppSettings.setMinimumDistance(Integer.valueOf(0));
			//appSettings.minimumDistance = 0;
		}

		if (AppSettings.shouldUseImperial())
		{
			AppSettings.setMinimumDistance(Utilities.FeetToMeters(AppSettings.getMinimumDistance()));
		}

		String minimumSecondsString = prefs.getString("time_before_logging", "60");

		if (minimumSecondsString != null && minimumSecondsString.length() > 0)
		{
			AppSettings.setMinimumSeconds(Integer.valueOf(minimumSecondsString));
		}
		else
		{
			AppSettings.setMinimumSeconds(60);
		}

		AppSettings.setNewFileCreation(prefs.getString("new_file_creation", "onceaday"));

		if (AppSettings.getNewFileCreation().equals("onceaday"))
		{
			AppSettings.setNewFileOnceADay(true);
		}
		else
		{
			AppSettings.setNewFileOnceADay(false);
		}

		AppSettings.setSeeMyMapUrl(prefs.getString("seemymap_URL", ""));

		AppSettings.setSeeMyMapGuid(prefs.getString("seemymap_GUID", ""));


		AppSettings.setAutoEmailEnabled(prefs.getBoolean("autoemail_enabled", false));

		AppSettings.setAutoEmailDelay(Float.valueOf(prefs.getString("autoemail_frequency", "0")));
	
		AppSettings.setWasRunning(prefs.getBoolean("wasRunning",false));
		
	}
	
	/**
	 * Displays a message box to the user with an OK button.
	 * 
	 * @param title
	 * @param message
	 * @param className
	 *            The calling class, such as GpsMainActivity.this or
	 *            mainActivity.
	 */
	public static void MsgBox(String title, String message, Context className)
	{
		MsgBox(title, message, className, null);
	}

	/**
	 * Displays a message box to the user with an OK button.
	 * 
	 * @param title
	 * @param message
	 * @param className
	 *            The calling class, such as GpsMainActivity.this or
	 *            mainActivity.
	 * @param callback
	 *            An object which implements IHasACallBack so that the click
	 *            event can call the callback method.
	 */
	public static void MsgBox(String title, String message, Context className,
			final IMessageBoxCallback msgCallback)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(className).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton(className.getString(R.string.ok), new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				if (msgCallback != null)
				{
					msgCallback.MessageBoxResult(which);
				}
			}
		});
		alertDialog.show();
	}

	/**
	 * Removes bad characters from a string, HTML-encodes it, then URL-encodes
	 * it. Best used when sending to server.
	 * 
	 * @param input
	 * @return
	 */
	public static String CleanString(String input)
	{

		input = input.replace(":", "");

		input = Utilities.EncodeHTML(input);

		input = URLEncoder.encode(input);
		return input;
	}

	/**
	 * Makes string safe for writing to XML file. Removes lt and gt. Best used
	 * when writing to file.
	 * 
	 * @param desc
	 * @return
	 */
	public static String CleanDescription(String desc)
	{
		desc = desc.replace("<", "");
		desc = desc.replace(">", "");
		desc = desc.replace("&", "&amp;");
		desc = desc.replace("\"", "&quot;");

		return desc;
	}

	/**
	 * HTML Encodes a string
	 * 
	 * @param s
	 * @return
	 */
	public static String EncodeHTML(String s)
	{
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&')
			{
				out.append("&#" + (int) c + ";");
			}
			else
			{
				out.append(c);
			}
		}
		return out.toString();
	}

	/**
	 * Converts seconds into friendly, understandable description of time.
	 * 
	 * @param numberOfSeconds
	 * @return
	 */
	public static String GetDescriptiveTimeString(int numberOfSeconds, Context context)
	{

		String descriptive = "";
		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		int remainingSeconds = 0;

		// Special cases
		if (numberOfSeconds == 1)
		{
			return context.getString(R.string.time_onesecond);
		}

		if (numberOfSeconds == 30)
		{
			return context.getString(R.string.time_halfminute);
		}

		if (numberOfSeconds == 60)
		{
			return context.getString(R.string.time_oneminute);
		}

		if (numberOfSeconds == 900)
		{
			return context.getString(R.string.time_quarterhour);
		}

		if (numberOfSeconds == 1800)
		{
			return context.getString(R.string.time_halfhour);
		}

		if (numberOfSeconds == 3600)
		{
			return context.getString(R.string.time_onehour);
		}

		if (numberOfSeconds == 4800)
		{
			return context.getString(R.string.time_oneandhalfhours);
		}

		if (numberOfSeconds == 9000)
		{
			return context.getString(R.string.time_twoandhalfhours);
		}

		// For all other cases, calculate

		hours = numberOfSeconds / 3600;
		remainingSeconds = numberOfSeconds % 3600;
		minutes = remainingSeconds / 60;
		seconds = remainingSeconds % 60;

		// Every 5 hours and 2 minutes
		// XYZ-5*2*20*

		descriptive = context.getString(R.string.time_hms_format, String.valueOf(hours),
				String.valueOf(minutes), String.valueOf(seconds));

		return descriptive;

	}

	/**
	 * Converts given bearing degrees into a rough cardinal direction that's
	 * more understandable to humans.
	 * 
	 * @param bearingDegrees
	 * @return
	 */
	public static String GetBearingDescription(float bearingDegrees, Context context)
	{

		String direction;
		String cardinal;

		if (bearingDegrees > 348.75 || bearingDegrees <= 11.25)
		{
			cardinal = context.getString(R.string.direction_north);
		}
		else if (bearingDegrees > 11.25 && bearingDegrees <= 33.75)
		{
			cardinal = context.getString(R.string.direction_northnortheast);
		}
		else if (bearingDegrees > 33.75 && bearingDegrees <= 56.25)
		{
			cardinal = context.getString(R.string.direction_northeast);
		}
		else if (bearingDegrees > 56.25 && bearingDegrees <= 78.75)
		{
			cardinal = context.getString(R.string.direction_eastnortheast);
		}
		else if (bearingDegrees > 78.75 && bearingDegrees <= 101.25)
		{
			cardinal = context.getString(R.string.direction_east);
		}
		else if (bearingDegrees > 101.25 && bearingDegrees <= 123.75)
		{
			cardinal = context.getString(R.string.direction_eastsoutheast);
		}
		else if (bearingDegrees > 123.75 && bearingDegrees <= 146.26)
		{
			cardinal = context.getString(R.string.direction_southeast);
		}
		else if (bearingDegrees > 146.25 && bearingDegrees <= 168.75)
		{
			cardinal = context.getString(R.string.direction_southsoutheast);
		}
		else if (bearingDegrees > 168.75 && bearingDegrees <= 191.25)
		{
			cardinal = context.getString(R.string.direction_south);
		}
		else if (bearingDegrees > 191.25 && bearingDegrees <= 213.75)
		{
			cardinal = context.getString(R.string.direction_southsouthwest);
		}
		else if (bearingDegrees > 213.75 && bearingDegrees <= 236.25)
		{
			cardinal = context.getString(R.string.direction_southwest);
		}
		else if (bearingDegrees > 236.25 && bearingDegrees <= 258.75)
		{
			cardinal = context.getString(R.string.direction_westsouthwest);
		}
		else if (bearingDegrees > 258.75 && bearingDegrees <= 281.25)
		{
			cardinal = context.getString(R.string.direction_west);
		}
		else if (bearingDegrees > 281.25 && bearingDegrees <= 303.75)
		{
			cardinal = context.getString(R.string.direction_westnorthwest);
		}
		else if (bearingDegrees > 303.75 && bearingDegrees <= 326.25)
		{
			cardinal = context.getString(R.string.direction_northwest);
		}
		else if (bearingDegrees > 326.25 && bearingDegrees <= 348.75)
		{
			cardinal = context.getString(R.string.direction_northnorthwest);
		}
		else
		{
			direction = context.getString(R.string.unknown_direction);
			return direction;
		}

		direction = context.getString(R.string.direction_roughly, cardinal);
		return direction;

	}

	/**
	 * Checks if a person ID exists and if it doesn't, creates one and returns
	 * it.
	 * 
	 * @param context
	 * @return
	 */
	public static String GetPersonId(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String personId = prefs.getString("personId", "");

		if (personId == null || personId == "" || personId.length() == 0)
		{
			personId = String.valueOf(UUID.randomUUID());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("personId", personId);
			editor.commit();
		}

		return personId;
	}

	/**
	 * Returns the contents of the file in a byte array
	 * 
	 * @param file
	 *            File this method should read
	 * @return byte[] Returns a byte[] array of the contents of the file
	 */
	public static byte[] GetBytesFromFile(File file)
	{

		InputStream is;
		try
		{
			is = new FileInputStream(file);
		}
		catch (FileNotFoundException e)
		{
			return null;
		}

		System.out.println("\nDEBUG: FileInputStream is " + file);

		// Get the size of the file
		long length = file.length();
		System.out.println("DEBUG: Length of " + file + " is " + length + "\n");

		/*
		 * You cannot create an array using a long type. It needs to be an int
		 * type. Before converting to an int type, check to ensure that file is
		 * not larger than Integer.MAX_VALUE;
		 */
		if (length > Integer.MAX_VALUE)
		{
			System.out.println("File is too large to process");
			return null;
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		try
		{
			while ((offset < bytes.length)
					&& ((numRead = is.read(bytes, offset, bytes.length - offset)) >= 0))
			{

				offset += numRead;

			}
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length)
		{
			// throw new IOException("Could not completely read file " +
			// file.getName());
			return null;
		}

		try
		{
			is.close();
		}
		catch (IOException e)
		{

		}
		return bytes;

	}
	
	public static String GetStringFromByteArray(byte[] content)
	{
		return Base64.encodeBytes(content);
		//return new String(content);

	}

	/**
	 * Performs a web request on a given URL and returns the response as a
	 * string.
	 * 
	 * @param url
	 *            The URL to perform a web request on.
	 * @return The (usually HTML) response
	 * @throws Exception
	 *             Throws an exception if there was a connection error. Handle
	 *             this and inform the user that there was a problem.
	 */
	public static String GetUrl(String url) throws Exception
	{
		URL serverAddress = null;
		HttpURLConnection connection = null;
		// OutputStreamWriter wr = null;
		BufferedReader rd = null;
		StringBuilder sb = null;
		String line = null;

		try
		{
			serverAddress = new URL(url);
			// set up out communications stuff
			connection = null;

			// Set up the initial connection
			connection = (HttpURLConnection) serverAddress.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(10000);

			connection.setConnectTimeout(10000);
			connection.connect();

			// read the result from the server
			rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			sb = new StringBuilder();

			while ((line = rd.readLine()) != null)
			{
				sb.append(line);
			}

			return sb.toString();

		}
		catch (Exception e)
		{
			throw e;
			// Swallow
		}
		finally
		{
			// close the connection, set all objects
			// to null
			connection.disconnect();
			rd = null;
			sb = null;
			// wr = null;
			connection = null;
		}

	}

	public static String PostUrl(String url, String body, String soapAction)
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			String data = body;

			// Send data
			URL targetUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection();
			conn.addRequestProperty("SOAPAction", soapAction);
			conn.addRequestProperty("Content-Type", "text/xml; charset=utf-8");
			conn.setDoOutput(true);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(data);
			wr.flush();
			// Get the response

			InputStream iStream;
			if (conn.getResponseCode() >= 400)
			{
				iStream = conn.getErrorStream();
			}
			else
			{
				iStream = conn.getInputStream();
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader(iStream));

			String line;
			while ((line = rd.readLine()) != null)
			{
				sb.append(line);
			}
			wr.close();
			rd.close();
		}
		catch (Exception ex)
		{

			sb.append(ex.getMessage());
		}

		return sb.toString();
	}

	/**
	 * Given a Date object, returns an ISO 8601 date time string in UTC. Example:
	 * 2010-03-23T05:17:22Z but not 2010-03-23T05:17:22+04:00
	 * 
	 * @param dateToFormat
	 *            The Date object to format.
	 * @return The ISO 8601 formatted string.
	 */
	public static String GetIsoDateTime(Date dateToFormat)
	{
	
		//GPX specs say that time given should be in UTC, no local time. 
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateTimeString = sdf.format(dateToFormat);
		
		//dateTimeString = dateTimeString.replaceAll("\\+0000$", "Z");
		//dateTimeString = dateTimeString.replaceAll("(\\d\\d)$", ":$1");
		// Because the Z in SimpleDateFormat gives you '+0900', so we need to
		// add the colon ourselves

		return dateTimeString;

	}

	/**
	 * Converts given meters to feet.
	 * 
	 * @param m
	 * @return
	 */
	public static int MetersToFeet(int m)
	{
		return (int) Math.round(m * 3.2808399);
	}

	/**
	 * Converts given feet to meters
	 * 
	 * @param f
	 * @return
	 */
	public static int FeetToMeters(int f)
	{
		return (int) Math.round(f / 3.2808399);
	}

	/**
	 * Converts given meters to feet and rounds up.
	 * 
	 * @param m
	 * @return
	 */
	public static int MetersToFeet(double m)
	{
		return MetersToFeet((int) m);
	}

	/**
	 * Converts given feet to meters and rounds up.
	 * 
	 * @param f
	 * @return
	 */
	public static int FeetToMeters(double f)
	{
		return FeetToMeters((int) f);
	}

	public static String GetRegisterSettingUrl(String personId, String target)
	{
		String regUrl = GetEmailBaseUrl() + "/registersetting/" + personId + "/" + target;
		return regUrl;
	}

	public static String GetSeeMyMapAddLocationUrl(String seeMyMapGuid, double currentLatitude,
			double currentLongitude, String input)
	{

		String whereUrl = GetSeeMyMapBaseUrl() + "/savepoint/?guid=" + seeMyMapGuid + "&lat="
				+ String.valueOf(currentLatitude) + "&lon=" + String.valueOf(currentLongitude) + "&des="
				+ URLEncoder.encode(input);
		return whereUrl;

	}

	public static String GetSeeMyMapAddLocationWithDateUrl(String seeMyMapGuid, double currentLatitude,
			double currentLongitude, String input, String dateTime)
	{
		String whereUrl = GetSeeMyMapBaseUrl() + "/savepointwithdate/?guid=" + seeMyMapGuid + "&lat="
				+ String.valueOf(currentLatitude) + "&lon=" + String.valueOf(currentLongitude) + "&des="
				+ URLEncoder.encode(input) + "&date=" + URLEncoder.encode(dateTime);

		return whereUrl;

	}

	public static String GetSeeMyMapRequestUrl(String requestedUrl, String password, String personId)
	{

		String requestUrl = GetSeeMyMapBaseUrl() + "/requestmap/" + requestedUrl + "/" + password + "/"
				+ personId;

		return requestUrl;
	}

	public static String GetDeleteFirstPointUrl(String seeMyMapGuid)
	{
		String deleteUrl = GetSeeMyMapBaseUrl() + "/clearfirstpoint/" + seeMyMapGuid;

		return deleteUrl;
	}

	public static String GetDeleteLastPointUrl(String seeMyMapGuid)
	{
		String deleteUrl = GetSeeMyMapBaseUrl() + "/clearlastpoint/" + seeMyMapGuid;
		return deleteUrl;
	}

	public static String GetSeeMyMapClearMapUrl(String seeMyMapGuid)
	{
		String clearUrl = GetSeeMyMapBaseUrl() + "/clearmap/" + seeMyMapGuid;
		return clearUrl;
	}

	public static boolean IsValidEmailAddress(String email)
	{
		if (email == null || email.length() == 0)
		{
			return false;
		}

		email = email.trim().toUpperCase();
		if (email.matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static boolean IsValidUrlAndPassword(String requestedUrl, String password)
	{

		requestedUrl = requestedUrl.trim();
		if (requestedUrl != null && requestedUrl.length() > 0 && requestedUrl.matches("[a-zA-Z0-9]+")
				&& password != null && password.length() > 0 && password.matches("[a-zA-Z0-9]+"))
		{
			return true;
		}

		return false;
	}

	public static String GetEmailBaseUrl()
	{
		return "http://";
	}

	public static String GetSeeMyMapBaseUrl()
	{
		return "http://";
	}

	public static boolean Flag()
	{
		return false;
	}

}
