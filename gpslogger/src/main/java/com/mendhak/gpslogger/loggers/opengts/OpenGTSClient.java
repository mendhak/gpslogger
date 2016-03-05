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

package com.mendhak.gpslogger.loggers.opengts;


import com.mendhak.gpslogger.common.Maths;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.slf4j.Logs;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import java.net.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

//TODO: Remove BasicNameValuePair, use Httpurlconnection or OKHTTP client

/**
 * OpenGTS Client
 *
 * @author Francisco Reynoso <franole @ gmail.com>
 */
public class OpenGTSClient {

    private static final Logger LOG = Logs.of(OpenGTSClient.class);

    private String server;
    private Integer port;
    private String path;


    public OpenGTSClient(String server, Integer port, String path) {
        this.server = server;
        this.port = port;
        this.path = path;
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

    public void sendHTTP(String id, String accountName, SerializableLocation[] locations) throws Exception {

        for (SerializableLocation loc : locations) {

            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("id", id));
            qparams.add(new BasicNameValuePair("dev", id));
            if (!Strings.isNullOrEmpty(accountName)) {
                qparams.add(new BasicNameValuePair("acct", accountName));
            } else {
                qparams.add(new BasicNameValuePair("acct", id));
            }

            //OpenGTS 2.5.5 requires batt param or it throws exception...
            qparams.add(new BasicNameValuePair("batt", "0"));
            qparams.add(new BasicNameValuePair("code", "0xF020"));
            qparams.add(new BasicNameValuePair("alt", String.valueOf(loc.getAltitude())));
            qparams.add(new BasicNameValuePair("gprmc", OpenGTSClient.gprmcEncode(loc)));

            URI uri = URIUtils.createURI("http", server, port, path, getQuery(qparams), null);
            HttpGet httpget = new HttpGet(uri);
            URL url = httpget.getURI().toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
		    conn.setReadTimeout(30000);

            Scanner s;
            if(conn.getResponseCode() != 200){
                s = new Scanner(conn.getErrorStream());
                LOG.error("Status code: " + String.valueOf(conn.getResponseCode()));
                if(s.hasNext()){
                    LOG.error(s.useDelimiter("\\A").next());
                }
            } else {
                LOG.debug("Status code: " + String.valueOf(conn.getResponseCode()));
            }

        }

    }

    private String getQuery(List<NameValuePair> params)
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first) {
                first = false;
            }
            else {
                result.append("&");
            }

            result.append(pair.getName());
            result.append("=");
            result.append(pair.getValue());
        }

        return result.toString();
    }

    public void sendRAW(String id, String accountName, SerializableLocation[] locations) throws Exception {
        for (SerializableLocation loc : locations) {
            if(Strings.isNullOrEmpty(accountName)){
                accountName = id;
            }
            String message = accountName + "/" + id + "/" + gprmcEncode(loc);
                DatagramSocket socket = new DatagramSocket();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(server), port);
                LOG.debug("Sending UDP " + message);
                socket.send(packet);
                socket.close();
        }
    }

   private String getURL() {
        StringBuilder url = new StringBuilder();
        url.append(server);
        if (port != null) {
            url.append(":");
            url.append(port);
        }
        if (path != null) {
            url.append(path);
        }
        return url.toString();
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
    public static String gprmcEncode(SerializableLocation loc) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        DecimalFormat f = new DecimalFormat("0.000000", dfs);

        String gprmc = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,",
                "$GPRMC",
                getNmeaGprmcTime(new Date(loc.getTime())),
                "A",
                getNmeaGprmcCoordinates(Math.abs(loc.getLatitude())),
                (loc.getLatitude() >= 0) ? "N" : "S",
                getNmeaGprmcCoordinates(Math.abs(loc.getLongitude())),
                (loc.getLongitude() >= 0) ? "E" : "W",
                f.format(Maths.mpsToKnots(loc.getSpeed())),
                f.format(loc.getBearing()),
                getNmeaGprmcDate(new Date(loc.getTime()))
        );

        gprmc += "*" + getNmeaChecksum(gprmc);

        return gprmc;
    }

    public static String getNmeaGprmcTime(Date dateToFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String getNmeaGprmcDate(Date dateToFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateToFormat);
    }

    public static String getNmeaGprmcCoordinates(double coord) {
        // “DDDMM.MMMMM”
        int degrees = (int) coord;
        double minutes = (coord - degrees) * 60;

        DecimalFormat df = new DecimalFormat("00.00000", new DecimalFormatSymbols(Locale.US));
        StringBuilder rCoord = new StringBuilder();
        rCoord.append(degrees);
        rCoord.append(df.format(minutes));

        return rCoord.toString();
    }


    public static String getNmeaChecksum(String msg) {
        int chk = 0;
        for (int i = 1; i < msg.length(); i++) {
            chk ^= msg.charAt(i);
        }
        String chk_s = Integer.toHexString(chk).toUpperCase();
        while (chk_s.length() < 2) {
            chk_s = "0" + chk_s;
        }
        return chk_s;
    }

}
