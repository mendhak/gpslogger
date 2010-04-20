package com.mendhak.gpslogger.model;

/**
 * Represents a single GPS Point, containing Latitude, Longitude, DateTime and Description.
 * @author mendhak
 *
 */
public class GpxPoint
{
	private String latitude;
	private String longitude;
	private String datetime;
	private String description;

	public String getLatitude()
	{
		return latitude;
	}

	public String getLongitude()
	{
		return longitude;
	}

	public String getDateTime()
	{
		return datetime;
	}

	public String getDescription()
	{
		return description;
	}

	public void setLatitude(String value)
	{
		latitude = value;
	}

	public void setLongitude(String value)
	{
		longitude = value;
	}

	public void setDateTime(String value)
	{
		datetime = value;
	}

	public void setDescription(String value)
	{
		description = value;
	}

}
