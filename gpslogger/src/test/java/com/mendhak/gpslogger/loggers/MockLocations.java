package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.os.Bundle;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.lenient;
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
        lenient().when(m.loc.getProvider()).thenReturn(providerName);
        lenient().when(m.loc.getLatitude()).thenReturn(lat);
        lenient().when(m.loc.getLongitude()).thenReturn(lon);
        return m;
    }


    public MockLocations withAccuracy(float val) {
        lenient().when(loc.hasAccuracy()).thenReturn(true);
        lenient().when(loc.getAccuracy()).thenReturn(val);
        return this;
    }

    public Location build(){
        return loc;
    }

    public MockLocations withAltitude(double altitude) {
        lenient().when(loc.hasAltitude()).thenReturn(true);
        lenient().when(loc.getAltitude()).thenReturn(altitude);
        return this;
    }

    public MockLocations withBearing(float bearing) {
        lenient().when(loc.hasBearing()).thenReturn(true);
        lenient().when(loc.getBearing()).thenReturn(bearing);
        return this;
    }


    public MockLocations withSpeed(float speed) {
        lenient().when(loc.hasSpeed()).thenReturn(true);
        lenient().when(loc.getSpeed()).thenReturn(speed);
        return this;
    }

    public MockLocations withTime(long date){
        lenient().when(loc.getTime()).thenReturn(date);
        return this;
    }

    public MockLocations putExtra(String k, String v) {

        lenient().when(loc.getExtras()).thenReturn(bundle);
        lenient().when(bundle.getString(eq(k))).thenReturn(v);
        lenient().when(bundle.getString(eq(k),anyString())).thenReturn(v);

        return this;
    }

    public MockLocations putExtra(String k, int v) {

        lenient().when(loc.getExtras()).thenReturn(bundle);
        lenient().when(bundle.getInt(eq(k))).thenReturn(v);
        lenient().when(bundle.getInt(eq(k),anyInt())).thenReturn(v);

        return this;
    }

}