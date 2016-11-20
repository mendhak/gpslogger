package com.mendhak.gpslogger.common;


import android.location.Location;

public class Locations {


    public static Location getLocationWithAdjustedAltitude(Location loc, PreferenceHelper ph) {
        if(!loc.hasAltitude()){ return loc; }

        if(ph.shouldAdjustAltitudeFromGeoIdHeight() && loc.getExtras() != null){
            String geoidheight = loc.getExtras().getString("GEOIDHEIGHT");
            if (!Strings.isNullOrEmpty(geoidheight)) {
                loc.setAltitude((float) loc.getAltitude() - Float.valueOf(geoidheight));
            }
            else {
                //If geoid height not present for adjustment, don't record an elevation at all.
                loc.removeAltitude();
            }
        }

        if(loc.hasAltitude() && ph.getSubtractAltitudeOffset() != 0){
            loc.setAltitude(loc.getAltitude() - ph.getSubtractAltitudeOffset());
        }

        return loc;
    }
}
