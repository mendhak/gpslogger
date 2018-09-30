package com.mendhak.gpslogger.common;

import android.support.v4.content.FileProvider;

//This class exists purely to avoid collisions with any
//third-party libraries that might also have a
//file provider.
public class GpsLoggerFileProvider extends FileProvider{
}
