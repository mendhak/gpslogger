package com.mendhak.gpslogger;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.Random;

public class Common
{

    public static void SetDeviceTowerLocation(double latitude, double longitude, Context ctx)
    {
        //Create a test provider
        LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        try
        {
            locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, false, true, false, false, false, false, false,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        }
        catch(IllegalArgumentException iae)
        {
            //Provider already exists
        }

//        locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
//        locationManager.clearTestProviderStatus(LocationManager.GPS_PROVIDER);
//
//        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        Location location = new Location(LocationManager.NETWORK_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(System.currentTimeMillis()+15);
        location.setAccuracy(1);

        LogInfo("Setting test location " + String.valueOf(latitude) + ", " + String.valueOf(longitude)
                + ", " + System.currentTimeMillis());
        locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, location);
        //locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
    }
    
    public static void SetDeviceGpsLocation(double latitude, double longitude, Context ctx)
    {
        //Create a test provider
        LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        try
        {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, false, false, false,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        }
        catch(IllegalArgumentException iae)
        {
            //Provider already exists
        }

//        locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
//        locationManager.clearTestProviderStatus(LocationManager.GPS_PROVIDER);
//
//        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(System.currentTimeMillis()+15);
        location.setAccuracy(1);

        LogInfo("Setting test location " + String.valueOf(latitude) + ", " + String.valueOf(longitude)
                + ", " + System.currentTimeMillis());
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
        //locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
    }
    
    public static Location GetRandomLocation()
    {
        Location loc = new Location("Test");

        Random r = new Random();
        double randomLat =  (180 * r.nextDouble()) - 90;
        double randomLon = (360 * r.nextDouble()) - 180;

        LogInfo(String.valueOf(randomLat));
        loc.setLatitude(randomLat);
        
        LogInfo(String.valueOf(randomLon));
        loc.setLongitude(randomLon);
        
        return loc;
    }
    
    public static void LogInfo(String message)
    {
        Log.i("GPSLoggerTests", message);
    }
    
    
}
