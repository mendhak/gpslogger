package com.mendhak.gpslogger.helpers;

import java.util.ArrayList;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.mendhak.gpslogger.interfaces.IGpsLoggerSaxHandler;

import com.mendhak.gpslogger.model.GpxPoint;




public class GpxSaxHandler extends DefaultHandler implements IGpsLoggerSaxHandler
{

	private ArrayList<GpxPoint> Points = new ArrayList<GpxPoint>();

	private String latitude;
	private String longitude;
	private String description;
	private String datetime;
	private boolean isDateTimeNode;
	private boolean isDescriptionNode;
	
	public GpxSaxHandler()
	{
		description = "";
		datetime = "";
	}

	public ArrayList<GpxPoint> GetPoints()
	{
		return Points;
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
	{

		isDateTimeNode = false;
		isDescriptionNode = false;

		if (namespaceURI.equals("http://www.topografix.com/GPX/1/0") && localName.equals("trkpt"))
		{

			latitude = atts.getValue("", "lat");
			longitude = atts.getValue("", "lon");

		}

		if (namespaceURI.equals("http://www.topografix.com/GPX/1/0") && localName.equals("time"))
		{
			isDateTimeNode = true;
		}

		if (namespaceURI.equals("http://www.topografix.com/GPX/1/0") && localName.equals("desc"))
		{
			isDescriptionNode = true;
		}

	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{

		if (namespaceURI.equals("http://www.topografix.com/GPX/1/0") && localName.equals("trkpt"))
		{

			if (description.length() > 0)
			{

				GpxPoint point = new GpxPoint();
				point.setDateTime(datetime);
				point.setLatitude(latitude);
				point.setLongitude(longitude);
				point.setDescription(description);

				Points.add(point);

			}

			latitude = "";
			longitude = "";
			datetime = "";
			description = "";

		}

	}

	public void characters(char[] ch, int start, int length) throws SAXException
	{
		if (isDateTimeNode)
		{
			datetime = new String(ch, start, length);
		}

		if (isDescriptionNode)
		{
			description = new String(ch, start, length);
		}

	}

	
}
