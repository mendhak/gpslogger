package com.mendhak.gpslogger.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import com.mendhak.gpslogger.R;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Utilities
{
	
	private static final int		LOGLEVEL	= 3;
	private static ProgressDialog	pd;
	
	private static void LogToDebugFile(String message)
	{
		if (AppSettings.isDebugToFile())
		{
			DebugLogger.Write(message);
		}
	}
	
	public static void LogInfo(String message)
	{
		if (LOGLEVEL >= 3)
		{
			Log.i("GPSLogger", message);
		}

		LogToDebugFile(message);
		
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
		LogToDebugFile(message);
	}
	
	@SuppressWarnings("unused")
	public static void LogDebug(String message)
	{
		if (LOGLEVEL >= 4)
		{
			Log.d("GPSLogger", message);
		}
		LogToDebugFile(message);
	}
	
	public static void LogWarning(String message)
	{
		if (LOGLEVEL >= 2)
		{
			Log.w("GPSLogger", message);
		}
		LogToDebugFile(message);
	}
	
	@SuppressWarnings("unused")
	public static void LogVerbose(String message)
	{
		if (LOGLEVEL >= 5)
		{
			Log.v("GPSLogger", message);
		}
		LogToDebugFile(message);
	}
	
	/**
	 * Gets user preferences, populates the AppSettings class.
	 * 
	 * @param context
	 * @return
	 */
	public static void PopulateAppSettings(Context context)
	{
		
		Utilities.LogInfo("Getting preferences");
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		AppSettings.setUseImperial(prefs.getBoolean("useImperial", false));
		AppSettings.setUseSatelliteTime(prefs.getBoolean("satellite_time",
				false));
		
		AppSettings.setLogToKml(prefs.getBoolean("log_kml", false));
		
		AppSettings.setLogToGpx(prefs.getBoolean("log_gpx", false));
		
		AppSettings.setShowInNotificationBar(prefs.getBoolean(
				"show_notification", true));
		
		AppSettings.setPreferCellTower(prefs.getBoolean("prefer_celltower",
				false));
		
		String minimumDistanceString = prefs.getString(
				"distance_before_logging", "0");
		
		if (minimumDistanceString != null && minimumDistanceString.length() > 0)
		{
			AppSettings.setMinimumDistance(Integer
					.valueOf(minimumDistanceString));
		}
		else
		{
			AppSettings.setMinimumDistance(0);
		}
		
		if (AppSettings.shouldUseImperial())
		{
			AppSettings.setMinimumDistance(Utilities.FeetToMeters(AppSettings
					.getMinimumDistance()));
		}
		
		String minimumSecondsString = prefs.getString("time_before_logging",
				"60");
		
		if (minimumSecondsString != null && minimumSecondsString.length() > 0)
		{
			AppSettings
					.setMinimumSeconds(Integer.valueOf(minimumSecondsString));
		}
		else
		{
			AppSettings.setMinimumSeconds(60);
		}
		
		AppSettings.setNewFileCreation(prefs.getString("new_file_creation",
				"onceaday"));
		
		if (AppSettings.getNewFileCreation().equals("onceaday"))
		{
			AppSettings.setNewFileOnceADay(true);
		}
		else
		{
			AppSettings.setNewFileOnceADay(false);
		}
		
		AppSettings.setAutoEmailEnabled(prefs.getBoolean("autoemail_enabled",
				false));
		
		if (Float.valueOf(prefs.getString("autoemail_frequency", "0")) >= 8f)
		{
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("autoemail_frequency", "8");
			editor.commit();
		}
		
		AppSettings.setAutoEmailDelay(Float.valueOf(prefs.getString(
				"autoemail_frequency", "0")));
		
		AppSettings.setSmtpServer(prefs.getString("smtp_server", ""));
		AppSettings.setSmtpPort(prefs.getString("smtp_port", "25"));
		AppSettings.setSmtpSsl(prefs.getBoolean("smtp_ssl", true));
		AppSettings.setSmtpUsername(prefs.getString("smtp_username", ""));
		AppSettings.setSmtpPassword(prefs.getString("smtp_password", ""));
		AppSettings.setAutoEmailTarget(prefs.getString("autoemail_target", ""));
		AppSettings.setDebugToFile(prefs.getBoolean("debugtofile", false));
	}
	
	public static void ShowProgress(Context ctx, String title, String message)
	{
		if (ctx != null)
		{
			pd = new ProgressDialog(ctx, ProgressDialog.STYLE_HORIZONTAL);
			pd.setMax(100);
			pd.setIndeterminate(true);
			
			pd = ProgressDialog.show(ctx, title, message, true, true);
		}
	}
	
	public static void HideProgress()
	{
		if (pd != null)
		{
			pd.dismiss();
		}
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
	 * @param msgCallback
	 *            An object which implements IHasACallBack so that the click
	 *            event can call the callback method.
	 */
	private static void MsgBox(String title, String message, Context className,
			final IMessageBoxCallback msgCallback)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(className).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton(className.getString(R.string.ok),
				new DialogInterface.OnClickListener()
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
	 * Converts seconds into friendly, understandable description of time.
	 * 
	 * @param numberOfSeconds
	 * @return
	 */
	public static String GetDescriptiveTimeString(int numberOfSeconds,
			Context context)
	{
		
		String descriptive;
		int hours;
		int minutes;
		int seconds;
		
		int remainingSeconds;
		
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
		
		descriptive = context.getString(R.string.time_hms_format,
				String.valueOf(hours), String.valueOf(minutes),
				String.valueOf(seconds));
		
		return descriptive;
		
	}
	
	/**
	 * Converts given bearing degrees into a rough cardinal direction that's
	 * more understandable to humans.
	 * 
	 * @param bearingDegrees
	 * @return
	 */
	public static String GetBearingDescription(float bearingDegrees,
			Context context)
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
	 * Given a Date object, returns an ISO 8601 date time string in UTC.
	 * Example: 2010-03-23T05:17:22Z but not 2010-03-23T05:17:22+04:00
	 * 
	 * @param dateToFormat
	 *            The Date object to format.
	 * @return The ISO 8601 formatted string.
	 */
	public static String GetIsoDateTime(Date dateToFormat)
	{
		// GPX specs say that time given should be in UTC, no local time.
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(dateToFormat);
	}
	
	public static String GetReadableDateTime(Date dateToFormat)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
		return sdf.format(dateToFormat);
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
	
	public static boolean IsEmailSetup(Context ctx)
	{
        return AppSettings.isAutoEmailEnabled()
                && AppSettings.getAutoEmailTarget().length() > 0
                && AppSettings.getSmtpServer().length() > 0
                && AppSettings.getSmtpPort().length() > 0
                && AppSettings.getSmtpUsername().length() > 0;

    }
	
	public static OAuthConsumer GetOSMAuthConsumer(Context ctx)
	{
		
		OAuthConsumer consumer = null;
		
		try
		{
			int osmConsumerKey = ctx.getResources().getIdentifier(
					"osm_consumerkey", "string", ctx.getPackageName());
			int osmConsumerSecret = ctx.getResources().getIdentifier(
					"osm_consumersecret", "string", ctx.getPackageName());
			consumer = new CommonsHttpOAuthConsumer(
					ctx.getString(osmConsumerKey),
					ctx.getString(osmConsumerSecret));
			
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(ctx);
			String osmAccessToken = prefs.getString("osm_accesstoken", "");
			String osmAccessTokenSecret = prefs.getString(
					"osm_accesstokensecret", "");
			
			if (osmAccessToken != null && osmAccessToken.length() > 0
					&& osmAccessTokenSecret != null
					&& osmAccessTokenSecret.length() > 0)
			{
				consumer.setTokenWithSecret(osmAccessToken,
						osmAccessTokenSecret);
			}
			
		}
		catch (Exception e)
		{
            //Swallow the exception
		}
		
		return consumer;
	}
	
	public static OAuthProvider GetOSMAuthProvider(Context ctx)
	{
		return new CommonsHttpOAuthProvider(
				ctx.getString(R.string.osm_requesttoken_url),
				ctx.getString(R.string.osm_accesstoken_url),
				ctx.getString(R.string.osm_authorize_url));
		
	}
	
	public static boolean IsOsmAuthorized(Context ctx)
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String oAuthAccessToken = prefs.getString("osm_accesstoken", "");
		
		return (oAuthAccessToken != null && oAuthAccessToken.length() > 0);
	}
	
	public static Intent GetOsmSettingsIntent(Context ctx)
	{
		Intent intentOsm;
		
		if (!IsOsmAuthorized(ctx))
		{
			intentOsm = new Intent(ctx.getPackageName() + ".OSM_AUTHORIZE");
			intentOsm.setData(Uri.parse("gpslogger://authorize"));
		}
		else
		{
			intentOsm = new Intent(ctx.getPackageName() + ".OSM_SETUP");
			
		}
		
		return intentOsm;
	}

   /**
   * Parses XML Nodes and returns a string. This method exists due to a problem in the Android framework;
   * no transformers!
   * http://stackoverflow.com/questions/2290945/writing-xml-on-android
   * @param root
   * @return
   */
  public static String GetStringFromNode(Node root)  {

      StringBuilder result = new StringBuilder();

      if (root.getNodeType() == Node.TEXT_NODE)
      {
          result.append(root.getNodeValue());
      }
      else
      {
          if (root.getNodeType() != Node.DOCUMENT_NODE)
          {
              StringBuffer attrs = new StringBuffer();
              for (int k = 0; k < root.getAttributes().getLength(); ++k)
              {
                  attrs.append(" ")
                      .append(root.getAttributes().item(k).getNodeName())
                      .append("=\"")
                      .append(root.getAttributes().item(k).getNodeValue())
                      .append("\" ");
              }
              result.append("<")
                  .append(root.getNodeName());

              if(attrs.length() > 0)
              {
                  result.append(" ")
                  .append(attrs);
              }

                  result.append(">");
          }
          else
          {
              result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
          }

          NodeList nodes = root.getChildNodes();
          for (int i = 0, j = nodes.getLength(); i < j; i++)
          {
              Node node = nodes.item(i);
              result.append(GetStringFromNode(node));
          }

          if (root.getNodeType() != Node.DOCUMENT_NODE)
          {
              result.append("</").append(root.getNodeName()).append(">");
          }
      }
      return result.toString();
  }


	
}
