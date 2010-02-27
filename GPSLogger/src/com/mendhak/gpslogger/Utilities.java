package com.mendhak.gpslogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
		desc = desc.replace("'","\'");
		
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


}
