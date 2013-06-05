package com.mendhak.gpslogger.com.mendhak.gpslogger.fragments;

import android.location.Location;

public interface IWidgetFragment {
    public String getTitle();
    public void onLocationChanged(Location loc);
    public void setSatelliteInfo(int number);
    public void setStatus(final String message);
    public void clear();
}