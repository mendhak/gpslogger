/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.loggers.nmea;


import com.mendhak.gpslogger.common.Strings;

public class NmeaSentence {

    String[] nmeaParts;

    public NmeaSentence(String nmeaSentence){
        if(Strings.isNullOrEmpty(nmeaSentence)){
            nmeaParts = new String[]{""};
            return;
        }
        nmeaParts = nmeaSentence.split(",");

    }

    private boolean isGGA() {
        return nmeaParts[0].toUpperCase().contains("GGA");
    }

    private boolean isGSA() {
        return nmeaParts[0].toUpperCase().contains("GSA");
    }

    public boolean isLocationSentence(){
        return isGSA() || isGGA();
    }


    public String getLatestPdop(){
        if (isGSA()) {

            if (nmeaParts.length > 15 && !Strings.isNullOrEmpty(nmeaParts[15])) {
                return nmeaParts[15];
            }
        }

        return null;
    }

    public String getLatestVdop(){
        if (isGSA()) {
            if (nmeaParts.length > 17 &&!Strings.isNullOrEmpty(nmeaParts[17]) && !nmeaParts[17].startsWith("*")) {
                return nmeaParts[17].split("\\*")[0];
            }
        }

        return null;
    }

    public String getLatestHdop(){
        if (isGGA()) {
            if (nmeaParts.length > 8 &&!Strings.isNullOrEmpty(nmeaParts[8])) {
                return nmeaParts[8];
            }
        }
        else if (isGSA()) {
            if (nmeaParts.length > 16 &&!Strings.isNullOrEmpty(nmeaParts[16])) {
                    return nmeaParts[16];
                }
        }

        return null;
    }

    public String getGeoIdHeight(){
        if (isGGA()) {
            if (nmeaParts.length > 11 &&!Strings.isNullOrEmpty(nmeaParts[11])) {
                return nmeaParts[11];
            }
        }

        return null;
    }

    public String getAgeOfDgpsData(){
        if (isGGA()) {
            if (nmeaParts.length > 13 && !Strings.isNullOrEmpty(nmeaParts[13])) {
                return nmeaParts[13];
            }
        }

        return null;
    }

    public String getDgpsId(){
        if (isGGA()) {
            if (nmeaParts.length > 14 &&!Strings.isNullOrEmpty(nmeaParts[14]) && !nmeaParts[14].startsWith("*")) {
                return nmeaParts[14].split("\\*")[0];
            }
        }

        return null;
    }

}
