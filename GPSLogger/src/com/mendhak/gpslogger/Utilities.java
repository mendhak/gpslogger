package com.mendhak.gpslogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Utilities
{

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
		AlertDialog alertDialog = new AlertDialog.Builder(className).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				return;
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
	static String EncodeHTML(String s)
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
	public static String GetDescriptiveTimeString(int numberOfSeconds)
	{

		String descriptive = "";
		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		int remainingSeconds = 0;

		// Special cases
		if (numberOfSeconds == 1)
		{
			return "second";
		}

		if (numberOfSeconds == 30)
		{
			return "half a minute";
		}

		if (numberOfSeconds == 60)
		{
			return "minute";
		}

		if (numberOfSeconds == 900)
		{
			return "quarter hour";
		}

		if (numberOfSeconds == 1800)
		{
			return "half an hour";
		}

		if (numberOfSeconds == 3600)
		{
			return "hour";
		}

		if (numberOfSeconds == 4800)
		{
			return "1\u00BD hours";
		}

		if (numberOfSeconds == 9000)
		{
			return "2\u00BD hours";
		}

		// For all other cases, calculate

		hours = numberOfSeconds / 3600;
		remainingSeconds = numberOfSeconds % 3600;
		minutes = remainingSeconds / 60;
		seconds = remainingSeconds % 60;

		if (hours == 1)
		{
			descriptive = String.valueOf(hours) + " hour";
		}
		else if (hours > 1)
		{
			descriptive = String.valueOf(hours) + " hours";
		}

		if (minutes >= 0 && hours > 0)
		{
			String joiner = (seconds > 0) ? ", " : " and ";
			String minuteWord = (minutes == 1) ? " minute" : " minutes";
			descriptive = descriptive + joiner + String.valueOf(minutes) + minuteWord;
			// 4 hours, 2 minutes
			// 1 hours, 0 minutes
			// 2 hours, 0 minutes
			// 3 hours and 35 minutes
			// 1 hour and 8 minutes
		}
		else if (minutes > 0 && hours == 0)
		{
			String minuteWord = (minutes == 1) ? " minute" : " minutes";
			descriptive = String.valueOf(minutes) + minuteWord;
			// 45 minutes
		}

		if ((hours > 0 || minutes > 0) && seconds > 0)
		{
			String secondsWord = (seconds == 1) ? " second" : " seconds";

			descriptive = descriptive + " and " + String.valueOf(seconds) + secondsWord;
			// 2 hours, 0 minutes and 5 seconds
			// 1 hour, 12 minutes and 9 seconds
		}
		else if (hours == 0 && minutes == 0 && seconds > 0)
		{
			String secondsWord = (seconds == 1) ? " second" : " second";
			descriptive = String.valueOf(seconds) + secondsWord;
		}

		return descriptive;

	}

	/**
	 * Converts given bearing degrees into a rough cardinal direction that's
	 * more understandable to humans.
	 * 
	 * @param bearingDegrees
	 * @return
	 */
	public static String GetBearingDescription(float bearingDegrees)
	{

		String direction;

		if (bearingDegrees > 348.75 || bearingDegrees <= 11.25)
		{
			direction = "Roughly North";
		}
		else if (bearingDegrees > 11.25 && bearingDegrees <= 33.75)
		{
			direction = "Roughly North-NorthEast";
		}
		else if (bearingDegrees > 33.75 && bearingDegrees <= 56.25)
		{
			direction = "Roughly NorthEast";
		}
		else if (bearingDegrees > 56.25 && bearingDegrees <= 78.75)
		{
			direction = "Roughly East-NorthEast";
		}
		else if (bearingDegrees > 78.75 && bearingDegrees <= 101.25)
		{
			direction = "Roughly East";
		}
		else if (bearingDegrees > 101.25 && bearingDegrees <= 123.75)
		{
			direction = "Roughly East-SouthEast";
		}
		else if (bearingDegrees > 123.75 && bearingDegrees <= 146.26)
		{
			direction = "Roughly SouthEast";
		}
		else if (bearingDegrees > 146.25 && bearingDegrees <= 168.75)
		{
			direction = "Roughly South-SouthEast";
		}
		else if (bearingDegrees > 168.75 && bearingDegrees <= 191.25)
		{
			direction = "Roughly South";
		}
		else if (bearingDegrees > 191.25 && bearingDegrees <= 213.75)
		{
			direction = "Roughly South-SouthWest";
		}
		else if (bearingDegrees > 213.75 && bearingDegrees <= 236.25)
		{
			direction = "Roughly SouthWest";
		}
		else if (bearingDegrees > 236.25 && bearingDegrees <= 258.75)
		{
			direction = "Roughly West-SouthWest";
		}
		else if (bearingDegrees > 258.75 && bearingDegrees <= 281.25)
		{
			direction = "Roughly West";
		}
		else if (bearingDegrees > 281.25 && bearingDegrees <= 303.75)
		{
			direction = "Roughly West-NorthWest";
		}
		else if (bearingDegrees > 303.75 && bearingDegrees <= 326.25)
		{
			direction = "Roughly NorthWest";
		}
		else if (bearingDegrees > 326.25 && bearingDegrees <= 348.75)
		{
			direction = "Roughly North-NorthWest";
		}
		else
		{
			direction = "Unknown";
		}

		return direction;

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

	/**
	 * Given a Date object, returns an ISO 8601 date time string. Examples:
	 * 2010-03-23T05:17:22Z or 2010-03-23T05:17:22+04:00
	 * 
	 * @param dateToFormat
	 *            The Date object to format.
	 * @return The ISO 8601 formatted string.
	 */
	public static String GetIsoDateTime(Date dateToFormat)
	{

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String dateTimeString = sdf.format(dateToFormat);
		dateTimeString = dateTimeString.replaceAll("\\+0000$", "Z");
		dateTimeString = dateTimeString.replaceAll("(\\d\\d)$", ":$1");
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

	public static String GetSeeMyMapRequestUrl(String requestedUrl, String password)
	{

		String requestUrl = GetSeeMyMapBaseUrl() + "/requestmap/" + requestedUrl + "/" + password;

		return requestUrl;
	}

	public static String GetSeeMyMapClearMapUrl(String seeMyMapGuid)
	{
		String clearUrl = GetSeeMyMapBaseUrl() + "/clearmap/" + seeMyMapGuid;
		return clearUrl;
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

	public static String GetSeeMyMapBaseUrl()
	{
		return "http://www.example.com";
	}

	public static boolean Flag()
	{
		return false;
	}

}
