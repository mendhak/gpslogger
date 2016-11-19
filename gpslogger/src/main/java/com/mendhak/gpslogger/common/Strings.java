package com.mendhak.gpslogger.common;


import android.content.Context;
import android.os.Build;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {
    /**
     * Converts seconds into friendly, understandable description of the duration.
     *
     * @param numberOfSeconds
     * @return
     */
    public static String getDescriptiveDurationString(int numberOfSeconds,
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
    public static String getBearingDescription(float bearingDegrees,
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
    public static String cleanDescription(String desc) {
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
    public static String getIsoDateTime(Date dateToFormat) {
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

    public static String getReadableDateTime(Date dateToFormat) {
        /**
         * Similar to getIsoDateTime(), this function is used in
         * AutoEmailManager, and we want machine-readable output.
         */
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm",
                Locale.US);
        return sdf.format(dateToFormat);
    }

    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null ||  text.trim().length() == 0;
    }

    public static String htmlDecode(String text) {
        if (isNullOrEmpty(text)) {
            return text;
        }

        return text.replace("&amp;", "&").replace("&quot;", "\"");
    }

    public static String getBuildSerial() {
        try {
            return Build.SERIAL;
        } catch (Throwable t) {
            return "";
        }
    }

    public static String getFormattedCustomFileName(String baseName) {
        return getFormattedCustomFileName(baseName, GregorianCalendar.getInstance(), PreferenceHelper.getInstance());
    }

    public static String getFormattedCustomFileName(String baseName, Calendar calendar, PreferenceHelper ph){

        String finalFileName = baseName;
        finalFileName = finalFileName.replaceAll("(?i)%ser", String.valueOf(getBuildSerial()));
        finalFileName = finalFileName.replaceAll("(?i)%ver", String.valueOf(BuildConfig.VERSION_NAME));
        finalFileName = finalFileName.replaceAll("(?i)%hour", String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        finalFileName = finalFileName.replaceAll("(?i)%min", String.format("%02d", calendar.get(Calendar.MINUTE)));
        finalFileName = finalFileName.replaceAll("(?i)%year",  String.valueOf(calendar.get(Calendar.YEAR)));
        finalFileName = finalFileName.replaceAll("(?i)%month", String.format("%02d", calendar.get(Calendar.MONTH) +1));
        finalFileName = finalFileName.replaceAll("(?i)%day", String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH) ));
        finalFileName = finalFileName.replaceAll("(?i)%profile", String.valueOf(ph.getCurrentProfileName()));
        return finalFileName;
    }

    public static int toInt(String number, int defaultVal) {
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static String getSpeedDisplay(Context context, double metersPerSecond, boolean imperial){

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

    public static String getDistanceDisplay(Context context, double meters, boolean imperial) {
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

    public static String getTimeDisplay(Context context, long milliseconds) {

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

    public static Map<String, String> getAvailableLocales(Context context) {

        Map<String, String> locales = new TreeMap<>();

        String[] availableLocales = BuildConfig.TRANSLATION_ARRAY;

        for (String foundLocale : availableLocales) {

            String displayName = new Locale(foundLocale).getDisplayName(new Locale(foundLocale));
            if (!displayName.equalsIgnoreCase(foundLocale)) {
                displayName = new Locale(foundLocale).getDisplayName(new Locale("en"));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                displayName = Locale.forLanguageTag(foundLocale).getDisplayName(Locale.forLanguageTag(foundLocale));

                if (displayName.equalsIgnoreCase(foundLocale)) {
                    displayName = Locale.forLanguageTag(foundLocale).getDisplayName(Locale.forLanguageTag("en"));
                }
            }

            locales.put(foundLocale, displayName);
        }


        return locales;
    }

    public static String getSanitizedMarkdownForFaqView(String md){
        if(Strings.isNullOrEmpty(md)){ return "";}

        //         \[[^\]]+\]\(((?!http)[^\)]+)\)
        //         Matches any Markdown link that isn't starting with http...
        String output = md;

        Matcher imgMatcher = Pattern.compile("(!\\[[^\\]]+\\]\\((?!http)[^\\)]+\\))", Pattern.MULTILINE).matcher(md);
        while(imgMatcher.find()){
            String group = imgMatcher.group(1);
            output = output.replace(group,"");
        }

        Matcher linkMatcher = Pattern.compile("\\[[^\\]]+\\]\\(((?!http)[^\\)]+)\\)", Pattern.MULTILINE).matcher(md);
        while(linkMatcher.find()){
            String group = linkMatcher.group(1);
            output = output.replace(group,"http://code.mendhak.com/gpslogger/"+group);
        }

        return output;
    }

    public static String DecimalDegreesToDegreesMinutesSeconds(double decimaldegrees, boolean isLatitude) {
        String cardinality = (decimaldegrees<0) ? "S":"N";

        if(!isLatitude){
            cardinality = (decimaldegrees<0) ? "W":"E";
        }

        //Remove negative sign
        decimaldegrees = Math.abs(decimaldegrees);

        int deg =  (int) Math.floor(decimaldegrees);
        double minfloat = (decimaldegrees-deg)*60;
        int min = (int) Math.floor(minfloat);
        double secfloat = (minfloat-min)*60;
        double sec = Math.round(secfloat * 10000.0)/10000.0;

        // After rounding, the seconds might become 60. These two
        // if-tests are not necessary if no rounding is done.
        if (sec==60) {
            min++;
            sec=0;
        }
        if (min==60) {
            deg++;
            min=0;
        }


        return ("" + deg + "° " + min + "' " + sec + "\" " + cardinality);
    }

    public static String DecimalDegreesToDegreesDecimalMinutes(double decimaldegrees, boolean isLatitude) {
        String cardinality = (decimaldegrees<0) ? "S":"N";

        if(!isLatitude){
            cardinality = (decimaldegrees<0) ? "W":"E";
        }

        //Remove negative sign
        decimaldegrees = Math.abs(decimaldegrees);

        int deg =  (int) Math.floor(decimaldegrees);
        double minfloat = (decimaldegrees-deg)*60;
        double min = Math.round(minfloat*10000.0)/10000.0;

        return ("" + deg + "° " + min + "' " + cardinality);
    }

    public static String DecimalDegreesFormatted(double decimaldegrees) {
        return DecimalDegreesFormatted(decimaldegrees, 6);
    }

    public static String DecimalDegreesFormatted(double decimaldegrees, int maxDecimalPoints) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(maxDecimalPoints);
        return nf.format(decimaldegrees);
    }
}
