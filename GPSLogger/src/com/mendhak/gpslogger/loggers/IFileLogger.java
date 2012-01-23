package com.mendhak.gpslogger.loggers;

import android.location.Location;

public interface IFileLogger
{
    void Write(Location loc) throws Exception;

    void Annotate(String description, Location loc) throws Exception;

}
