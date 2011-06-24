package com.mendhak.gpslogger.loggers;

import java.util.ArrayList;

import org.xml.sax.ContentHandler;


public interface IGpsLoggerSaxHandler extends ContentHandler
{

	public ArrayList<GpxPoint> GetPoints();

}
