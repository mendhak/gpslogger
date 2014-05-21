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

import android.content.Context;
import android.location.Location;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * OpenGTS Client
 *
 * @author Francisco Reynoso <franole @ gmail.com>
 */
public class OpenGTSClient
{

    private Context applicationContext;
    private IActionListener callback;
    private String server;
    private Integer port;
    private String path;
    private AsyncHttpClient httpClient;
    private int locationsCount = 0;
    private int sentLocationsCount = 0;


    public OpenGTSClient(String server, Integer port, String path, IActionListener callback, Context applicationContext)
    {
        this.server = server;
        this.port = port;
        this.path = path;
        this.callback = callback;
        this.applicationContext = applicationContext;
    }

    public void sendHTTP(String id, Location location)
    {
        sendHTTP(id, new Location[]{location});
    }

    /**
     * Send locations sing HTTP GET request to the server
     * <p/>
     * See <a href="http://opengts.sourceforge.net/OpenGTS_Config.pdf">OpenGTS_Config.pdf</a>
     * section 9.1.2 Default "gprmc" Configuration
     *
     * @param id        id of the device
     * @param locations locations
     */

    public void sendHTTP(String id, Location[] locations)
    {
        try
        {
            locationsCount = locations.length;
            StringBuilder url = new StringBuilder();
            url.append("http://");
            url.append(getURL());

            httpClient = new AsyncHttpClient();

            for (Location loc : locations)
            {
                RequestParams params = new RequestParams();
                params.put("id", id);
                params.put("code", "0xF020");
                params.put("gprmc", OpenGTSClient.GPRMCEncode(loc));
                params.put("alt", String.valueOf(loc.getAltitude()));


                Utilities.LogDebug("Sending URL " + url + " with params " + params.toString());
                httpClient.get(applicationContext, url.toString(), params, new MyAsyncHttpResponseHandler(this));
            }
        }
        catch (Exception e)
        {
            Utilities.LogError("OpenGTSClient.sendHTTP", e);
            OnFailure();
        }
    }

    public void sendRAW(String id, Location location)
    {
        // TODO
    }

    private void sendRAW(String id, Location[] locations)
    {
        // TODO
    }

    private String getURL()
    {
        StringBuilder url = new StringBuilder();
        url.append(server);
        if (port != null)
        {
            url.append(":");
            url.append(port);
        }
        if (path != null)
        {
            url.append(path);
        }
        return url.toString();
    }


    private class MyAsyncHttpResponseHandler extends AsyncHttpResponseHandler
    {
        private OpenGTSClient callback;

        public MyAsyncHttpResponseHandler(OpenGTSClient callback)
        {
            super();
            this.callback = callback;
        }

        @Override
        public void onSuccess(String response)
        {
            Utilities.LogInfo("Response Success :" + response);
            callback.OnCompleteLocation();
        }

        @Override
        public void onFailure(Throwable e, String response)
        {
            Utilities.LogError("OnCompleteLocation.MyAsyncHttpResponseHandler Failure with response :" + response, new Exception(e));
            callback.OnFailure();
        }
    }

    public void OnCompleteLocation()
    {
        sentLocationsCount += 1;
        Utilities.LogDebug("Sent locations count: " + sentLocationsCount + "/" + locationsCount);
        if (locationsCount == sentLocationsCount)
        {
            OnComplete();
        }
    }

    public void OnComplete()
    {
        callback.OnComplete();
    }

    public void OnFailure()
    {
        httpClient.cancelRequests(applicationContext, true);
        callback.OnFailure();
    }

    /**
     * Encode a location as GPRMC string data.
     * <p/>
     * For details check org.opengts.util.Nmea0183#_parse_GPRMC(String)
     * (OpenGTS source)
     *
     * @param loc location
     * @return GPRMC data
     */
    public static String GPRMCEncode(Location loc)
    {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        DecimalFormat f = new DecimalFormat("0.000000", dfs);

        String gprmc = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,",
                "$GPRMC",
                NMEAGPRMCTime(new Date(loc.getTime())),
                "A",
                NMEAGPRMCCoord(Math.abs(loc.getLatitude())),
                (loc.getLatitude() >= 0) ? "N" : "S",
                NMEAGPRMCCoord(Math.abs(loc.getLongitude())),
                (loc.getLongitude() >= 0) ? "E" : "W",
                f.format(MetersPerSecondToKnots(loc.getSpeed())),
                f.format(loc.getBearing()),
                NMEAGPRMCDate(new Date(loc.getTime()))
        );

        gprmc += "*" + NMEACheckSum(gprmc);

        return gprmc;
    }

    public static String NMEAGPRMCTime(Date dateToFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String NMEAGPRMCDate(Date dateToFormat)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String NMEAGPRMCCoord(double coord)
    {
        // “DDDMM.MMMMM”
        int degrees = (int) coord;
        double minutes = (coord - degrees) * 60;

        DecimalFormat df = new DecimalFormat("00.00000", new DecimalFormatSymbols(Locale.US));
        StringBuilder rCoord = new StringBuilder();
        rCoord.append(degrees);
        rCoord.append(df.format(minutes));

        return rCoord.toString();
    }


    public static String NMEACheckSum(String msg)
    {
        int chk = 0;
        for (int i = 1; i < msg.length(); i++)
        {
            chk ^= msg.charAt(i);
        }
        String chk_s = Integer.toHexString(chk).toUpperCase();
        while (chk_s.length() < 2)
        {
            chk_s = "0" + chk_s;
        }
        return chk_s;
    }

    /**
     * Converts given meters/second to nautical mile/hour.
     *
     * @param mps meters per second
     * @return knots
     */
    public static double MetersPerSecondToKnots(double mps)
    {
        // Google "meters per second to knots"
        return mps * 1.94384449;
    }

}
