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


/*
 * Copyright Geeksville Industries LLC, a California limited liability corporation.
 * Copyright Marc Poulhiès <dkm@kataplop.net>
 * Code is also published with GPL2
 * http://github.com/geeksville/Gaggle
 */

package com.mendhak.gpslogger.loggers;

import android.location.Location;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.os.SystemClock;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mendhak.gpslogger.common.Utilities;


public class LiveTrack24FileLogger implements IFileLogger {

    public static final String name = "LIVETRACK24";

    /**
     * Sent to server
     */
    private String ourVersion = "0.1";

    /**
     * How many secs between position reports - FIXME, choose correctly
     */
    private int expectedIntervalSecs;

    /**
     * FIXME, get the real phone name
     */
    private String phoneName = android.os.Build.MODEL;

    /**
     * Our app name, FIXME - pick a good name
     */
    private String programName = "Android GpsLogger";

    /**
     * FIXME - pass in from app
     */
    private String vehicleName = "Foo";

    /**
     * Hardwired for paraglider - FIXME
     */
    private int vehicleType = 1;

    /**
     * Claim built in for now
     */
    private String gpsType = "Internal GPS";

    private String userName;
    private String password;
    private final String serverURL;

    private String trackURL, clientURL;

    private String sessionId = null; //(new Random()).nextInt(0x7fffffff);
    private boolean login_ok = false;
    private boolean start_ok = false;

    private int packetNum = 1;
    long lastUpdateTime = SystemClock.elapsedRealtime();

    private static LiveTrack24FileLogger instance = null;

    public static LiveTrack24FileLogger getLiveTrack24Logger(String serverURL, String username,
                                                      String password, int expectedInterval)
            throws MalformedURLException {
        if (instance == null ||
                !instance.serverURL.equals(serverURL) ||
                !instance.userName.equals(username) ||
                !instance.password.equals(password) ||
                expectedInterval != instance.expectedIntervalSecs){
            instance = new LiveTrack24FileLogger(serverURL, username, password, expectedInterval);
        }

        return instance;
    }

    private LiveTrack24FileLogger(String serverURL, String username,
                                  String password, int expectedInterval) throws MalformedURLException {
//             PackageManager pm = context.getPackageManager();
//		PackageInfo pi;
//		try {
//			pi = pm.getPackageInfo(context.getPackageName(), 0);
//
//			ourVersion = pi.versionName;
//		} catch (NameNotFoundException eNnf) {
//			throw new RuntimeException(eNnf); // We better be able to find the
//			// info about our own package
//		}
        Utilities.LogDebug("livetrack24 constructor");

        URL url = new URL(serverURL + "/track.php");
        trackURL = url.toString();
        url = new URL(serverURL + "/client.php");
        clientURL = url.toString();

        this.userName = username;
        this.password = password;
        this.serverURL = serverURL;

//		this.vehicleType = vehicleType;
//		this.vehicleName = vehicleName;
        expectedIntervalSecs = expectedInterval;

        doLogin(); // Login here, so we can find out about bad passwords ASAP
    }

    @Override
    public void close() throws  Exception{
        // FIXME - add support for end of track types (need retrieve etc...)
        HashMap<String,String> m = new HashMap<String, String>();
        m.put("prid", "0");
        sendPacket(PACKET_END, m);

        instance = null;
    }

    @Override
    public void Write(Location loc) throws Exception {

        if (!login_ok) {
            Utilities.LogDebug("Livetrack24 Write but login not ok");
            return;
//            throw new Exception("Livetrack24 error");
        }
        if (!start_ok){
            Utilities.LogDebug("Livetrack24 Write but start not ok");
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (lastUpdateTime + (expectedIntervalSecs * 1000) < now) {

            sendPacket(PACKET_POINT, loc);
            lastUpdateTime = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void Annotate(String description, Location loc) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        return name;
    }

    static int PACKET_START = 2; // FIXME, lookup java const syntax
    static int PACKET_END = 3;
    static int PACKET_POINT = 4;

    /**
     * Cleans up illegal chars in a URL
     *
     * @param url
     * @return FIXME move
     */
    static String normalizeURL(String url) {
        return url.replace(' ', '+'); // FIXME, do a better job of this
    }

    void sendPacket(int packetType, Map<String,String> params){
              AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
                   @Override
                   public void onSuccess(String message){
                       Utilities.LogDebug("packet sent ok");
                   }

                   @Override
                   public void onFailure(Throwable e, String response){
                       Utilities.LogDebug("packet NOT sent");
                   }
              };
        sendPacket(packetType, params, handler);
    }

    void sendPacket(int packetType, Map<String,String> params, AsyncHttpResponseHandler handler){
        AsyncHttpClient httpClient = new AsyncHttpClient();

        RequestParams nparams = new RequestParams(params);

        nparams.put("leolive", Integer.toString(packetType));
        nparams.put("sid", sessionId);
        nparams.put("pid", Integer.toString(packetNum));

        httpClient.get(null, trackURL, nparams, handler);
        packetNum++;
    }

    void sendPacket(int packetType, Location loc) {
//		try {
//
//			String urlstr = String.format(Locale.US,
//					"%s?leolive=%d&sid=%d&pid=%d&%s", trackURL, packetType,
//					sessionId, packetNum, options);
//
//						"lat=%f&lon=%f&alt=%d&sog=%d&cog=%d&tm=%d",
//                        loc.getLatitude(),
//						loc.getLongitude(),
//                        loc.hasAltitude() ? (int) loc.getAltitude() : "0",
//                        groundKmPerHr,
//                        loc.getBearing(),
//						unixTimestamp);
        final int groundKmPerHr = (int) (loc.getSpeed() / 3.6);
        final int unixTimestamp = (int) (loc.getTime() / 1000); // Convert from msecs to
        // secs
        final int alt = (int) (loc.hasAltitude() ? loc.getAltitude() : 0);

        HashMap<String,String> params = new HashMap<String,String>();


        params.put("lat", Float.toString((float)loc.getLatitude()));
        params.put("lon", Float.toString((float)loc.getLongitude()));
        params.put("alt", Integer.toString(alt));
        params.put("sog", Integer.toString(groundKmPerHr));
        params.put("cog", Integer.toString((int)loc.getBearing()));
        params.put("tm", Integer.toString(unixTimestamp));

        sendPacket(packetType, params);
//
//			URL url = new URL(normalizeURL(urlstr));
//
//			url.openStream().();
//		} catch (MalformedURLException ex) {
//			// We should have caught this in the constructor
//			throw new RuntimeException(ex);
//		}

    }


    private class AsyncLoginHandler extends AsyncHttpResponseHandler {
        @Override
        public void onSuccess(String response) {
            try {
                Utilities.LogDebug("livetrack24: resp for login");
                final int userID = Integer.parseInt(response);

                if (userID == 0){
                    Utilities.LogDebug("livetrack24: incorrect user/pass");
                    return;
                }

//                if (userID == 0)
//                    throw new Exception("Invalid username or password");
                Utilities.LogDebug("livetrack24: userid=" + userID);
                final Random a = new Random(System.currentTimeMillis());
                int rnd = Math.abs(a.nextInt());
                // we make an int with leftmost bit=1 ,
                // the next 7 bits random
                // (so that the same userID can have multiple active sessions)
                // and the next 3 bytes the userID
                int sid = (rnd & 0x7F000000) | (userID & 0x00ffffff) | 0x80000000;
                sessionId =  Long.toString(sid & 0xFFFFFFFFL);
                Utilities.LogDebug("livetrack24: made a correct session ID " + sessionId);

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("client", programName);
                params.put("v",ourVersion);
                params.put("user",userName);
                params.put("pass",password);
                params.put("phone",phoneName);
                params.put("gps",gpsType);
                params.put("trk1", Integer.toString(expectedIntervalSecs));
                params.put("vtype", Integer.toString(vehicleType));
                params.put("vname",vehicleName);

                sendPacket(PACKET_START, params,new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String message){
                        Utilities.LogDebug("packet START sent ok");
                        start_ok = true;
                    }

                    @Override
                    public void onFailure(Throwable e, String response){
                        Utilities.LogDebug("packet START NOT sent");
                    }
                });

                login_ok = true;
            } catch (NumberFormatException ex) {
                Utilities.LogDebug("livetrack24: got unexpected answer:"+ response);

//                throw new Exception("Unexpected server response");
            }
//            Utilities.LogInfo("Response Success :" + response);
//            callback.OnCompleteLocation();
        }

        @Override
        public void onFailure(Throwable e, String response) {
            Utilities.LogDebug("livetrack24: login resp failed");
//            Utilities.LogError("OnCompleteLocation.MyAsyncHttpResponseHandler Failure with response :" + response, new Exception(e));
//            callback.OnFailure();
        }
    }

    private void doLogin() {
        // If the user has an account on the server then the sessionID must be
        // constructed in the following way to contain the userID
        // First of all your application must get the userID based on the
        // username and password of the user. The url to verify user accounts
        // and get back the userID is
        // http://www.livetrack24.com/client.php?op=login&user=username&pass=pass
        // The username and password are case INSENSITIVE, because on mobile
        // devices it is not easy for all users < ></>o enter the correct case.
        // The result of the page is an integer, 0 if userdata are incorrect, or
        // else the userID of the user

        AsyncHttpClient httpClient = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("op", "login");
        params.put("user", userName);
        params.put("pass", password);

        Utilities.LogDebug("livetrack24: sending login info");
        httpClient.get(null, clientURL, params, new AsyncLoginHandler());
    }
}
