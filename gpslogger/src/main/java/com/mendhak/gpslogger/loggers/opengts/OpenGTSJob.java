package com.mendhak.gpslogger.loggers.opengts;

import com.mendhak.gpslogger.common.Maths;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.net.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

public class OpenGTSJob extends Job {

    String server;
    int port ;
    String accountName ;
    String path ;
    String deviceId ;
    String communication;
    SerializableLocation[] locations;
    private static final Logger LOG = Logs.of(OpenGTSJob.class);

    public OpenGTSJob(String server, int port, String accountName, String path, String deviceId, String communication, SerializableLocation[] locations){
        super(new Params(1).requireNetwork().persist());

        this.server = server;
        this.port = port;
        this.accountName = accountName;
        this.path = path;
        this.deviceId = deviceId;
        this.communication = communication;
        this.locations = locations;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        LOG.debug("Running OpenGTS Job");

        if(communication.equalsIgnoreCase("UDP")){
            sendRAW(deviceId, accountName, locations);
        }
        else{
            sendHTTP(deviceId, accountName, locations);
        }

        EventBus.getDefault().post(new UploadEvents.OpenGTS().succeeded());
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        LOG.error("Could not send to OpenGTS", throwable);
        EventBus.getDefault().post(new UploadEvents.OpenGTS().failed("Could not send to OpenGTS", throwable));
        return false;
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

            URL url = getUrl(id, accountName, loc);

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

    URL getUrl(String id, String accountName, SerializableLocation loc) throws Exception {
        List<AbstractMap.SimpleEntry<String,String>> qparams = new ArrayList<>();
        qparams.add(new AbstractMap.SimpleEntry<>("id", id));
        qparams.add(new AbstractMap.SimpleEntry<>("dev", id));
        if (!Strings.isNullOrEmpty(accountName)) {
            qparams.add(new AbstractMap.SimpleEntry<>("acct", accountName));
        } else {
            qparams.add(new AbstractMap.SimpleEntry<>("acct", id));
        }

        //OpenGTS 2.5.5 requires batt param or it throws exception...
        qparams.add(new AbstractMap.SimpleEntry<>("batt", "0"));
        qparams.add(new AbstractMap.SimpleEntry<>("code", "0xF020"));
        qparams.add(new AbstractMap.SimpleEntry<>("alt", String.valueOf(loc.getAltitude())));
        qparams.add(new AbstractMap.SimpleEntry<>("gprmc", gprmcEncode(loc)));

        if(path.startsWith("/")){
            path = path.replaceFirst("/","");
        }

        //URI uri = URIUtils.createURI("http", server, port, path, getQuery(qparams), null);
        URL url = new URL(String.format("%s://%s:%d/%s?%s","http",server,port,path,getQuery(qparams)));
        return url;
    }

    private String getQuery(List<AbstractMap.SimpleEntry<String, String>> params)
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry<String, String> pair : params)
        {
            if (first) {
                first = false;
            }
            else {
                result.append("&");
            }

            result.append(pair.getKey());
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
