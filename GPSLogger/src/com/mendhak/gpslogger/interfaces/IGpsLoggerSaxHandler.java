package com.mendhak.gpslogger.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import org.xml.sax.ContentHandler;

import com.mendhak.gpslogger.model.GpxPoint;

public interface IGpsLoggerSaxHandler extends ContentHandler
{

	public ArrayList<GpxPoint> GetPoints();

}
