package com.mendhak.gpslogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Utilities {
	
	static void MsgBox(String title, String message, Context className) {
		AlertDialog alertDialog = new AlertDialog.Builder(
				className).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		alertDialog.show();
	}
	
	static String EncodeHTML(String s)
	{
	    StringBuffer out = new StringBuffer();
	    for(int i=0; i<s.length(); i++)
	    {
	        char c = s.charAt(i);
	        if(c > 127 || c=='"' || c=='<' || c=='>' || c=='&')
	        {
	           out.append("&#"+(int)c+";");
	        }
	        else
	        {
	            out.append(c);
	        }
	    }
	    return out.toString();
	}
	
	
	static String CleanDescription(String desc)
	{
		desc = desc.replace("<","");
		desc = desc.replace(">", "");
		desc = desc.replace("&","&amp;");
		desc = desc.replace("\"", "&quot;");
		
		return desc;
	}
	
	static String GetUrl(String url)
	{
		URL serverAddress = null;
		HttpURLConnection connection = null;
		// OutputStreamWriter wr = null;
		BufferedReader rd = null;
		StringBuilder sb = null;
		String line = null;

		try {

			serverAddress = new URL(url);
			// set up out communications stuff
			connection = null;

			// Set up the initial connection
			connection = (HttpURLConnection) serverAddress
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(10000);

			connection.setConnectTimeout(10000);
			connection.connect();

			// read the result from the server
			rd = new BufferedReader(new InputStreamReader(connection
					.getInputStream()));
			sb = new StringBuilder();

			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}

			return sb.toString();

		} catch(Exception e) {
			//Swallow
		} finally {
			// close the connection, set all objects
			// to null
			connection.disconnect();
			rd = null;
			sb = null;
			// wr = null;
			connection = null;
		}
		
		return "";

	}
	
	public static boolean Flag(){
		return false;
	}

	public static String GetIsoDateTime(Date dateToFormat) {
			
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String dateTimeString = sdf.format(dateToFormat);
		dateTimeString = dateTimeString.replaceAll("\\+0000$", "Z");
		dateTimeString = dateTimeString.replaceAll("(\\d\\d)$", ":$1"); 
		//Because the Z in SimpleDateFormat gives you '+0900', so we need to add the colon ourselves
		
		return dateTimeString;
		
	}
	
	public static int MetersToFeet(int m)
	{
		return (int) Math.round(m * 3.2808399);
	}
	
	public static int FeetToMeters(int f)
	{
		return (int) Math.round(f / 3.2808399);
	}
	
	public static int MetersToFeet(double m)
	{
		return MetersToFeet((int)m);
	}
	
	public static int FeetToMeters(double f)
	{
		return FeetToMeters((int)f);
	}

	public static String GetSeeMyMapAddLocationUrl(String seeMyMapGuid,
			double currentLatitude, double currentLongitude, String input) {
		
		String whereUrl = "http://www.example.com/savepoint/?guid="
			+ seeMyMapGuid
			+ "&lat="
			+ String.valueOf(currentLatitude)
			+ "&lon=" + String.valueOf(currentLongitude) + "&des=" + input;
		return whereUrl;
		
	}

	public static String GetSeeMyMapRequestUrl(String requestedUrl,
			String password) {
		
		String requestUrl = "http://www.example.com/requestmap/"
		+ requestedUrl + "/" + password;
		
		return requestUrl;
	}


}
