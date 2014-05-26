package com.mendhak.gpslogger.loggers.utils;

import android.location.Location;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by dkm on 12/11/13.
 */
public class LocationBuffer {

    public static class BufferedLocation {
        public final long timems;
        public final double lat;
        public final double lon;
        public final int altitude;
        public final int bearing;
        public final int speed;

        public BufferedLocation(long time_ms, double lat, double lon, int alt, int bearing, int speed){
            timems = time_ms;
            this.lat = lat;
            this.lon = lon;
            this.altitude = alt;
            this.bearing = bearing;
            this.speed = speed;
        }
        public Location toLocation(){
            Location loc = new Location("BufferLocation");
            loc.setTime(timems);
            loc.setLatitude(lat);
            loc.setLongitude(lon);
            loc.setAltitude(altitude);
            loc.setBearing(bearing);
            loc.setSpeed(speed);
            return loc;
        }
    }

    private final ConcurrentLinkedQueue<BufferedLocation> loc_buffer = new ConcurrentLinkedQueue<BufferedLocation>();

    public BufferedLocation pop(){
        return loc_buffer.poll();
    }

    public BufferedLocation peek(){
        return loc_buffer.peek();
    }
    public boolean isEmpty(){
        return loc_buffer.isEmpty();
    }
    public int size(){
        return loc_buffer.size();
    }

    public void push(BufferedLocation b){
        loc_buffer.add(b);
    }

    public void push(long time_ms, double lat, double lon, int alt, int bearing, int speed){
        loc_buffer.add(new BufferedLocation(time_ms, lat, lon, alt, bearing, speed));
    }
}
