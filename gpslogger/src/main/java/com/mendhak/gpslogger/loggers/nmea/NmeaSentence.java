package com.mendhak.gpslogger.loggers.nmea;


import com.mendhak.gpslogger.common.Utilities;

public class NmeaSentence {

    String[] nmeaParts;

    public NmeaSentence(String nmeaSentence){
        if(Utilities.IsNullOrEmpty(nmeaSentence)){
            nmeaParts = new String[]{""};
            return;
        }
        nmeaParts = nmeaSentence.split(",");

    }

    public String getLatestPdop(){
        if (nmeaParts[0].equalsIgnoreCase("$GPGSA")) {

            if (nmeaParts.length > 15 && !Utilities.IsNullOrEmpty(nmeaParts[15])) {
                return nmeaParts[15];
            }
        }

        return null;
    }

    public String getLatestVdop(){
        if (nmeaParts[0].equalsIgnoreCase("$GPGSA")) {
            if (nmeaParts.length > 17 &&!Utilities.IsNullOrEmpty(nmeaParts[17]) && !nmeaParts[17].startsWith("*")) {
                return nmeaParts[17].split("\\*")[0];
            }
        }

        return null;
    }

    public String getLatestHdop(){
        if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
            if (nmeaParts.length > 8 &&!Utilities.IsNullOrEmpty(nmeaParts[8])) {
                return nmeaParts[8];
            }
        }

        else if (nmeaParts[0].equalsIgnoreCase("$GPGSA")) {
            if (nmeaParts.length > 16 &&!Utilities.IsNullOrEmpty(nmeaParts[16])) {
                return nmeaParts[16];
            }
        }

        return null;
    }

    public String getGeoIdHeight(){
        if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
            if (nmeaParts.length > 11 &&!Utilities.IsNullOrEmpty(nmeaParts[11])) {
                return nmeaParts[11];
            }
        }

        return null;
    }

    public String getAgeOfDgpsData(){
        if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
            if (nmeaParts.length > 13 && !Utilities.IsNullOrEmpty(nmeaParts[13])) {
                return nmeaParts[13];
            }
        }

        return null;
    }

    public String getDgpsId(){
        if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
            if (nmeaParts.length > 14 &&!Utilities.IsNullOrEmpty(nmeaParts[14]) && !nmeaParts[14].startsWith("*")) {
                return nmeaParts[14].split("\\*")[0];
            }
        }

        return null;
    }

}
