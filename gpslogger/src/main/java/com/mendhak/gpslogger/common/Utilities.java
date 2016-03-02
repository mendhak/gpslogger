/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.slf4j.GpsRollingFileAppender;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utilities {

    private static MaterialDialog pd;
    private static org.slf4j.Logger tracer = LoggerFactory.getLogger(Utilities.class.getSimpleName());


    public static void ConfigureLogbackDirectly(Context context) {
        try {
            // reset the default context (which may already have been initialized)
            // since we want to reconfigure it
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            lc.reset();

            //final String LOG_DIR = "/sdcard/GPSLogger";
            final String LOG_DIR = PreferenceHelper.getInstance().getGpsLoggerFolder();

            GpsRollingFileAppender<ILoggingEvent> rollingFileAppender = new GpsRollingFileAppender<>();
            rollingFileAppender.setAppend(true);
            rollingFileAppender.setContext(lc);

            // OPTIONAL: Set an active log file (separate from the rollover files).
            // If rollingPolicy.fileNamePattern already set, you don't need this.
            rollingFileAppender.setFile(LOG_DIR + "/debuglog.txt");
            rollingFileAppender.setLazy(true);

            TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setFileNamePattern(LOG_DIR + "/debuglog.%d.txt");
            rollingPolicy.setMaxHistory(3);
            rollingPolicy.setParent(rollingFileAppender);  // parent and context required!
            rollingPolicy.setContext(lc);
            rollingPolicy.start();

            rollingFileAppender.setRollingPolicy(rollingPolicy);

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setPattern("%d{HH:mm:ss} %-5p %class{0}.%method:%L - %m%n");
            encoder.setContext(lc);
            encoder.start();

            rollingFileAppender.setEncoder(encoder);
            rollingFileAppender.start();

            // setup LogcatAppender
            PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
            encoder2.setContext(lc);
            encoder2.setPattern("%method:%L - %m%n");
            encoder2.start();

            LogcatAppender logcatAppender = new LogcatAppender();
            logcatAppender.setContext(lc);
            logcatAppender.setEncoder(encoder2);
            logcatAppender.start();

            SessionLogcatAppender sessionAppender = new SessionLogcatAppender();
            sessionAppender.setContext(lc);
            sessionAppender.start();

            // add the newly created appenders to the root logger;
            // qualify Logger to disambiguate from org.slf4j.Logger
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.addAppender(rollingFileAppender);
            root.addAppender(logcatAppender);
            root.addAppender(sessionAppender);

        }
        catch(Exception ex){
              System.out.println("Could not configure logging!");
        }

    }




    public static void ShowProgress(Context context, String title, String message) {
        if (context != null) {

            pd = new MaterialDialog.Builder(context)
                    .title(title)
                    .content(message)
                    .progress(true, 0)
                    .show();
        }
    }

    public static void HideProgress() {
        if (pd != null) {
            pd.dismiss();
        }
    }

    static String GetFormattedErrorMessageForDisplay(String message, Throwable throwable) {
        String html = "";
        if(!IsNullOrEmpty(message)){
            html += "<b>" + message.replace("\r\n","<br />").replace("\n","<br />") + "</b> <br /><br />";
        }

        if(throwable != null){
            if(!IsNullOrEmpty(throwable.getMessage())){
                html += throwable.getMessage().replace("\r\n","<br />") + "<br />";
            }
        }
       return html;
    }

    static String GetFormattedErrorMessageForPlainText(String message, Throwable throwable){

        StringBuilder sb = new StringBuilder();
        if(!IsNullOrEmpty(message)){
            sb.append(message).append("\r\n");
        }

        if(throwable != null){
            if(!IsNullOrEmpty(throwable.getMessage())){
                sb.append(throwable.getMessage()).append("\r\n");
            }

            if(throwable.getStackTrace().length > 0) {
                sb.append(Arrays.toString(throwable.getStackTrace()));
            }
        }

        return sb.toString();

    }

    public static void ErrorMsgBox(String title, final String friendlyMessage, final String errorMessage, final Throwable throwable, final Context context){

        final String messageFormatted = GetFormattedErrorMessageForDisplay(errorMessage, throwable);

        MaterialDialog alertDialog = new MaterialDialog.Builder(context)
                .title(title)
                .customView(R.layout.error_alertview, false)
                .autoDismiss(false)
                .negativeText("Copy")
                .positiveText(R.string.ok)
                .neutralText("Details")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {

                        final ExpandableTextView expTv1 = (ExpandableTextView) materialDialog.getCustomView().findViewById(R.id.error_expand_text_view);
                        expTv1.findViewById(R.id.expand_collapse).performClick();

                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {

                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Gpslogger error message", GetFormattedErrorMessageForPlainText(errorMessage, throwable));
                        clipboard.setPrimaryClip(clip);

                        materialDialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                })
                .build();


        final ExpandableTextView expTv1 = (ExpandableTextView) alertDialog.getCustomView().findViewById(R.id.error_expand_text_view);
        expTv1.setText(Html.fromHtml(messageFormatted));
        TextView tv = (TextView) expTv1.findViewById(R.id.expandable_text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvFriendly = (TextView)alertDialog.getCustomView().findViewById(R.id.error_friendly_message);
        tvFriendly.setText(friendlyMessage);

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param context The calling class, such as GpsMainActivity.this or
     *                  mainActivity.
     */
    public static void MsgBox(String title, String message, Context context) {
        MsgBox(title, message, context, null);
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param context   The calling class, such as GpsMainActivity.this or
     *                    mainActivity.
     * @param msgCallback An object which implements IHasACallBack so that the
     *                    click event can call the callback method.
     */
    public static void MsgBox(String title, String message, Context context, final MessageBoxCallback msgCallback) {
        MaterialDialog alertDialog = new MaterialDialog.Builder(context)
                .title(title)
                .content(Html.fromHtml(message))
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        if (msgCallback != null) {
                            msgCallback.messageBoxResult(MessageBoxCallback.OK);
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        if (msgCallback != null) {
                            msgCallback.messageBoxResult(MessageBoxCallback.CANCEL);
                        }
                    }
                })
                .build();

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }

    }

    /**
     * Converts seconds into friendly, understandable description of the duration.
     *
     * @param numberOfSeconds
     * @return
     */
    public static String GetDescriptiveDurationString(int numberOfSeconds,
                                                      Context context) {

        String descriptive;
        int hours;
        int minutes;
        int seconds;

        int remainingSeconds;

        // Special cases
        if(numberOfSeconds==0){
            return "";
        }

        if (numberOfSeconds == 1) {
            return context.getString(R.string.time_onesecond);
        }

        if (numberOfSeconds == 30) {
            return context.getString(R.string.time_halfminute);
        }

        if (numberOfSeconds == 60) {
            return context.getString(R.string.time_oneminute);
        }

        if (numberOfSeconds == 900) {
            return context.getString(R.string.time_quarterhour);
        }

        if (numberOfSeconds == 1800) {
            return context.getString(R.string.time_halfhour);
        }

        if (numberOfSeconds == 3600) {
            return context.getString(R.string.time_onehour);
        }

        if (numberOfSeconds == 4800) {
            return context.getString(R.string.time_oneandhalfhours);
        }

        if (numberOfSeconds == 9000) {
            return context.getString(R.string.time_twoandhalfhours);
        }

        // For all other cases, calculate

        hours = numberOfSeconds / 3600;
        remainingSeconds = numberOfSeconds % 3600;
        minutes = remainingSeconds / 60;
        seconds = remainingSeconds % 60;

        // Every 5 hours and 2 minutes
        // XYZ-5*2*20*

        descriptive = String.format(context.getString(R.string.time_hms_format),
                String.valueOf(hours), String.valueOf(minutes), String.valueOf(seconds));

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
                                               Context context) {

        String direction;
        String cardinal;

        if (bearingDegrees > 348.75 || bearingDegrees <= 11.25) {
            cardinal = context.getString(R.string.direction_north);
        } else if (bearingDegrees > 11.25 && bearingDegrees <= 33.75) {
            cardinal = context.getString(R.string.direction_northnortheast);
        } else if (bearingDegrees > 33.75 && bearingDegrees <= 56.25) {
            cardinal = context.getString(R.string.direction_northeast);
        } else if (bearingDegrees > 56.25 && bearingDegrees <= 78.75) {
            cardinal = context.getString(R.string.direction_eastnortheast);
        } else if (bearingDegrees > 78.75 && bearingDegrees <= 101.25) {
            cardinal = context.getString(R.string.direction_east);
        } else if (bearingDegrees > 101.25 && bearingDegrees <= 123.75) {
            cardinal = context.getString(R.string.direction_eastsoutheast);
        } else if (bearingDegrees > 123.75 && bearingDegrees <= 146.26) {
            cardinal = context.getString(R.string.direction_southeast);
        } else if (bearingDegrees > 146.25 && bearingDegrees <= 168.75) {
            cardinal = context.getString(R.string.direction_southsoutheast);
        } else if (bearingDegrees > 168.75 && bearingDegrees <= 191.25) {
            cardinal = context.getString(R.string.direction_south);
        } else if (bearingDegrees > 191.25 && bearingDegrees <= 213.75) {
            cardinal = context.getString(R.string.direction_southsouthwest);
        } else if (bearingDegrees > 213.75 && bearingDegrees <= 236.25) {
            cardinal = context.getString(R.string.direction_southwest);
        } else if (bearingDegrees > 236.25 && bearingDegrees <= 258.75) {
            cardinal = context.getString(R.string.direction_westsouthwest);
        } else if (bearingDegrees > 258.75 && bearingDegrees <= 281.25) {
            cardinal = context.getString(R.string.direction_west);
        } else if (bearingDegrees > 281.25 && bearingDegrees <= 303.75) {
            cardinal = context.getString(R.string.direction_westnorthwest);
        } else if (bearingDegrees > 303.75 && bearingDegrees <= 326.25) {
            cardinal = context.getString(R.string.direction_northwest);
        } else if (bearingDegrees > 326.25 && bearingDegrees <= 348.75) {
            cardinal = context.getString(R.string.direction_northnorthwest);
        } else {
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
    public static String CleanDescription(String desc) {
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
     * @param dateToFormat The Date object to format.
     * @return The ISO 8601 formatted string.
     */
    public static String GetIsoDateTime(Date dateToFormat) {
        /**
         * This function is used in gpslogger.loggers.* and for most of them the
         * default locale should be fine, but in the case of HttpUrlLogger we
         * want machine-readable output, thus  Locale.US.
         *
         * Be wary of the default locale
         * http://developer.android.com/reference/java/util/Locale.html#default_locale
         */

        // GPX specs say that time given should be in UTC, no local time.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(dateToFormat);
    }

    public static String GetReadableDateTime(Date dateToFormat) {
        /**
         * Similar to GetIsoDateTime(), this function is used in
         * AutoEmailManager, and we want machine-readable output.
         */
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm",
                Locale.US);
        return sdf.format(dateToFormat);
    }






    /**
     * Uses the Haversine formula to calculate the distnace between to lat-long coordinates
     *
     * @param latitude1  The first point's latitude
     * @param longitude1 The first point's longitude
     * @param latitude2  The second point's latitude
     * @param longitude2 The second point's longitude
     * @return The distance between the two points in meters
     */
    public static double CalculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        /*
            Haversine formula:
            A = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
            C = 2.atan2(√a, √(1−a))
            D = R.c
            R = radius of earth, 6371 km.
            All angles are in radians
            */

        double deltaLatitude = Math.toRadians(Math.abs(latitude1 - latitude2));
        double deltaLongitude = Math.toRadians(Math.abs(longitude1 - longitude2));
        double latitude1Rad = Math.toRadians(latitude1);
        double latitude2Rad = Math.toRadians(latitude2);

        double a = Math.pow(Math.sin(deltaLatitude / 2), 2) +
                (Math.cos(latitude1Rad) * Math.cos(latitude2Rad) * Math.pow(Math.sin(deltaLongitude / 2), 2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c * 1000; //Distance in meters

    }


    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean IsNullOrEmpty(String text) {
        return text == null || text.length() == 0;
    }


    public static byte[] GetByteArrayFromInputStream(InputStream is) {

        try {
            int length;
            int size = 1024;
            byte[] buffer;

            if (is instanceof ByteArrayInputStream) {
                size = is.available();
                buffer = new byte[size];
                is.read(buffer, 0, size);
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                buffer = new byte[size];
                while ((length = is.read(buffer, 0, size)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                buffer = outputStream.toByteArray();
            }
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                tracer.warn(SessionLogcatAppender.MARKER_INTERNAL, "GetStringFromInputStream - could not close stream");
            }
        }

        return null;

    }

    /**
     * Loops through an input stream and converts it into a string, then closes the input stream
     *
     * @param is
     * @return
     */
    public static String GetStringFromInputStream(InputStream is) {
        String line;
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // Read response until the end
        try {
            while ((line = rd.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                tracer.warn(SessionLogcatAppender.MARKER_INTERNAL, "GetStringFromInputStream - could not close stream");
            }
        }

        // Return full string
        return total.toString();
    }


    /**
     * Converts an input stream containing an XML response into an XML Document object
     *
     * @param stream
     * @return
     */
    public static Document GetDocumentFromInputStream(InputStream stream) {
        Document doc;

        try {
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            DocumentBuilder builder = xmlFactory.newDocumentBuilder();
            doc = builder.parse(stream);
        } catch (Exception e) {
            doc = null;
        }

        return doc;
    }

    /**
     * Gets the GPSLogger-specific MIME type to use for a given filename/extension
     *
     * @param fileName
     * @return
     */
    public static String GetMimeTypeFromFileName(String fileName) {

        if (fileName == null || fileName.length() == 0) {
            return "";
        }


        int pos = fileName.lastIndexOf(".");
        if (pos == -1) {
            return "application/octet-stream";
        } else {

            String extension = fileName.substring(pos + 1, fileName.length());


            if (extension.equalsIgnoreCase("gpx")) {
                return "application/gpx+xml";
            } else if (extension.equalsIgnoreCase("kml")) {
                return "application/vnd.google-earth.kml+xml";
            } else if (extension.equalsIgnoreCase("zip")) {
                return "application/zip";
            }
        }

        //Unknown mime type
        return "application/octet-stream";

    }

    public static float GetBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 0;

        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    public static String GetAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

    }


    public static String HtmlDecode(String text) {
        if (IsNullOrEmpty(text)) {
            return text;
        }

        return text.replace("&amp;", "&").replace("&quot;", "\"");
    }

    public static String GetBuildSerial() {
        try {
            return Build.SERIAL;
        } catch (Throwable t) {
            return "";
        }
    }

    public static File[] GetFilesInFolder(File folder) {
        return GetFilesInFolder(folder, null);
    }

    public static File[] GetFilesInFolder(File folder, FilenameFilter filter) {

        if (folder == null || !folder.exists() || folder.listFiles() == null) {
            return new File[]{};
        } else {
            if (filter != null) {
                return folder.listFiles(filter);
            }
            return folder.listFiles();
        }
    }


    public static String GetFormattedCustomFileName(String baseName) {
        return GetFormattedCustomFileName(baseName, GregorianCalendar.getInstance());
    }

    public static String GetFormattedCustomFileName(String baseName, Calendar calendar){

        String finalFileName = baseName;
        finalFileName = finalFileName.replaceAll("(?i)%ser", String.valueOf(Utilities.GetBuildSerial()));
        finalFileName = finalFileName.replaceAll("(?i)%hour", String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        finalFileName = finalFileName.replaceAll("(?i)%min", String.format("%02d", calendar.get(Calendar.MINUTE)));
        finalFileName = finalFileName.replaceAll("(?i)%year",  String.valueOf(calendar.get(Calendar.YEAR)));
        finalFileName = finalFileName.replaceAll("(?i)%month", String.format("%02d", calendar.get(Calendar.MONTH) +1));
        finalFileName = finalFileName.replaceAll("(?i)%day", String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH) ));
        return finalFileName;
    }


    public static File GetDefaultStorageFolder(Context context){
        File storageFolder = context.getExternalFilesDir(null);
        if(storageFolder == null){
            storageFolder = context.getFilesDir();
        }
        return storageFolder;
    }

    public static boolean IsPackageInstalled(String targetPackage, Context context){
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }

    public static void SetFileExplorerLink(TextView txtFilename, Spanned htmlString, final String pathToLinkTo, final Context context) {

        final Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.parse("file://" + pathToLinkTo), "resource/folder");
        intent.setAction(Intent.ACTION_VIEW);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            txtFilename.setLinksClickable(true);
            txtFilename.setClickable(true);
            txtFilename.setMovementMethod(LinkMovementMethod.getInstance());
            txtFilename.setSelectAllOnFocus(false);
            txtFilename.setTextIsSelectable(false);
            txtFilename.setText(htmlString);

            txtFilename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    context.startActivity(intent);
                }
            });
        }
    }

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        // note that this may return null if no network is currently active
        return cm.getActiveNetworkInfo();
    }

    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected());
    }


    public static String GetSpeedDisplay(Context context, double metersPerSecond, boolean imperial){

        DecimalFormat df = new DecimalFormat("#.###");
        String result = df.format(metersPerSecond) + context.getString(R.string.meters_per_second);

        if(imperial){
            result = df.format(metersPerSecond * 2.23693629) + context.getString(R.string.miles_per_hour);
        }
        else if(metersPerSecond >= 0.28){
            result = df.format(metersPerSecond * 3.6) + context.getString(R.string.kilometers_per_hour);
        }

        return result;

    }

    public static String GetDistanceDisplay(Context context, double meters, boolean imperial) {
        DecimalFormat df = new DecimalFormat("#.###");
        String result = df.format(meters) + context.getString(R.string.meters);

        if(imperial){
            if (meters <= 804){
                result = df.format(meters * 3.2808399) + context.getString(R.string.feet);
            }
            else {
                result = df.format(meters/1609.344) + context.getString(R.string.miles);
            }
        }
        else if(meters >= 1000){
            result = df.format(meters/1000) + context.getString(R.string.kilometers);
        }

        return result;
    }

    public static String GetTimeDisplay(Context context, long milliseconds) {

        double ms = (double)milliseconds;
        DecimalFormat df = new DecimalFormat("#.##");

        String result = df.format(ms/1000) + context.getString(R.string.seconds);

        if(ms > 3600000){
            result = df.format(ms/3600000) + context.getString(R.string.hours);
        }
        else if(ms > 60000){
            result = df.format(ms/60000) + context.getString(R.string.minutes);
        }

        return result;
    }

    public static void AddFileToMediaDatabase(File file, String mimeType){

        MediaScannerConnection.scanFile(AppSettings.getInstance(),
                new String[] { file.getPath() },
                new String[] { mimeType },
                null);
    }

    public static int parseIntWithDefault(String number, int defaultVal) {
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            return defaultVal;
        }
    }

}
