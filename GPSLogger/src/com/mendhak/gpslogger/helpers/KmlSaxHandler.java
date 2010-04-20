package com.mendhak.gpslogger.helpers;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mendhak.gpslogger.interfaces.IGpsLoggerSaxHandler;
import com.mendhak.gpslogger.model.GpxPoint;

public class KmlSaxHandler extends DefaultHandler implements IGpsLoggerSaxHandler
{

	private ArrayList<GpxPoint> Points = new ArrayList<GpxPoint>();

	private String latitude;
	private String longitude;
	private String coordinates;
	private String dateTime;
	private String description;

	private boolean isDateTimeNode;
	private boolean isCoordinatesNode;
	private boolean isDescriptionNode;
	
	public KmlSaxHandler()
	{
		dateTime = "";
		description = "";
		
	}

	public ArrayList<GpxPoint> GetPoints()
	{
		return Points;
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
	{

		isCoordinatesNode = false;
		isDateTimeNode = false;
		isDescriptionNode = false;

		if (namespaceURI.equals("http://www.opengis.net/kml/2.2") && localName.equals("coordinates"))
		{

			isCoordinatesNode = true;
		}

		if (namespaceURI.equals("http://www.opengis.net/kml/2.2") && localName.equals("when"))
		{
			isDateTimeNode = true;
		}

		if (namespaceURI.equals("http://www.opengis.net/kml/2.2") && localName.equals("name"))
		{
			isDescriptionNode = true;
		}

	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{

		if (namespaceURI.equals("http://www.opengis.net/kml/2.2") && localName.equals("name"))
		{

			if (description.length() > 0)
			{
				GpxPoint point = new GpxPoint();
				point.setDateTime(dateTime);
				point.setLatitude(latitude);
				point.setLongitude(longitude);
				point.setDescription(description);

				Points.add(point);
			}

			latitude = "";
			longitude = "";
			description = "";
			dateTime = "";

		}

	}

	public void characters(char[] ch, int start, int length) throws SAXException
	{

		if (isDescriptionNode)
		{
			description = new String(ch, start, length);
		}

		if (isCoordinatesNode)
		{
			coordinates = new String(ch, start, length);
			String[] segments = coordinates.split(",");
			longitude = segments[0];
			latitude = segments[1];
		}

		if (isDateTimeNode)
		{
			dateTime = new String(ch, start, length);
		}

	}

}
