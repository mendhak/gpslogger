package com.mendhak.gpslogger.common;


import android.location.Location;

public class Maths {
    /**
     * Uses the Haversine formula to calculate the distnace between to lat-long coordinates
     *
     * @param latitude1  The first point's latitude
     * @param longitude1 The first point's longitude
     * @param latitude2  The second point's latitude
     * @param longitude2 The second point's longitude
     * @return The distance between the two points in meters
     */
    public static double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
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
     * Converts given meters/second to nautical mile/hour.
     *
     * @param mps meters per second
     * @return knots
     */
    public static double mpsToKnots(double mps) {
        // Google "meters per second to knots"
        return mps * 1.94384449;
    }

    /**
     * Checks bundle in the Location object for satellties used in fix.
     * @param loc The location object to query
     * @return satellites used in fix, or 0 if no value found.
     */
    public static int getBundledSatelliteCount(Location loc){
        int sat = 0;

        if(loc.getExtras() != null){
            sat = loc.getExtras().getInt("satellites",0);

            if (sat == 0) {
                //Provider gave us nothing, let's look at our bundled count
                sat = loc.getExtras().getInt("SATELLITES_FIX", 0);
            }
        }

        return sat;
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
}
