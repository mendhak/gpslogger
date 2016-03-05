package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.os.Bundle;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MockLocations {

    private final Location loc;
    private final Bundle bundle;

    private MockLocations(Location loc, Bundle bundle) {
        this.loc = loc;
        this.bundle = bundle;
    }

    public static MockLocations builder(String providerName, double lat, double lon){

        MockLocations m = new MockLocations(mock(Location.class), mock(Bundle.class));
        when(m.loc.getProvider()).thenReturn(providerName);
        when(m.loc.getLatitude()).thenReturn(lat);
        when(m.loc.getLongitude()).thenReturn(lon);
        return m;
    }


    public MockLocations withAccuracy(float val) {
        when(loc.hasAccuracy()).thenReturn(true);
        when(loc.getAccuracy()).thenReturn(val);
        return this;
    }

    public Location build(){
        return loc;
    }

    public MockLocations withAltitude(double altitude) {
        when(loc.hasAltitude()).thenReturn(true);
        when(loc.getAltitude()).thenReturn(altitude);
        return this;
    }

    public MockLocations withBearing(float bearing) {
        when(loc.hasBearing()).thenReturn(true);
        when(loc.getBearing()).thenReturn(bearing);
        return this;
    }


    public MockLocations withSpeed(float speed) {
        when(loc.hasSpeed()).thenReturn(true);
        when(loc.getSpeed()).thenReturn(speed);
        return this;
    }

    public MockLocations withTime(long date){
        when(loc.getTime()).thenReturn(date);
        return this;
    }

    public MockLocations putExtra(String k, String v) {

//            Bundle b = new Bundle();
//            b.putString("HDOP", "LOOKATTHISHDOP!");

        when(loc.getExtras()).thenReturn(bundle);
        when(bundle.getString(eq(k))).thenReturn(v);

        return this;
    }
}