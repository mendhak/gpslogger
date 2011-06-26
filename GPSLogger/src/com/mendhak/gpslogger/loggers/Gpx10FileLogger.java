package com.mendhak.gpslogger.loggers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Date;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import android.location.Location;


public class Gpx10FileLogger implements IFileLogger
{
	private FileLock gpxLock;
	private File gpxFile = null;
	private boolean useSatelliteTime = false;
	private boolean addNewTrackSegment;
	
	public Gpx10FileLogger(File gpxFile, boolean useSatelliteTime, boolean addNewTrackSegment)
	{
		this.gpxFile = gpxFile;
		this.useSatelliteTime = useSatelliteTime;
		this.addNewTrackSegment = addNewTrackSegment;
	}

	@Override
	public void Write(Location loc) throws Exception
	{
		try
		{
			Date now;

			if (useSatelliteTime)
			{
				now = new Date(loc.getTime());
			}
			else
			{
				now = new Date();
			}

			String dateTimeString = Utilities.GetIsoDateTime(now);
			
			if (!gpxFile.exists())
			{
				gpxFile.createNewFile();
				
				FileOutputStream initialWriter = new FileOutputStream(gpxFile, true);
				BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

				String initialXml = "<?xml version=\"1.0\"?>"
						+ "<gpx version=\"1.0\" creator=\"GPSLogger - http://gpslogger.mendhak.com/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">"
						+ "<time>" + dateTimeString + "</time>" + "<bounds />" + "<trk></trk></gpx>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			int offsetFromEnd = (addNewTrackSegment) ? 12 : 21;

			long startPosition = gpxFile.length() - offsetFromEnd;

			String trackPoint = GetTrackPointXml(loc, dateTimeString);

			// Leaving this commented code in - may want to give user the choice
			// to
			// pick between WPT and TRK. Choice is good.
			//
			// String waypoint = "<wpt lat=\"" +
			// String.valueOf(loc.getLatitude())
			// + "\" lon=\"" + String.valueOf(loc.getLongitude()) + "\">"
			// + "<time>" + dateTimeString + "</time>";
			//
			// if (loc.hasAltitude()) {
			// waypoint = waypoint + "<ele>"
			// + String.valueOf(loc.getAltitude()) + "</ele>";
			// }
			//
			// if (loc.hasBearing()) {
			// waypoint = waypoint + "<course>"
			// + String.valueOf(loc.getBearing()) + "</course>";
			// }
			//
			// if (loc.hasSpeed()) {
			// waypoint = waypoint + "<speed>"
			// + String.valueOf(loc.getSpeed()) + "</speed>";
			// }
			//
			// waypoint = waypoint + "<src>" + loc.getProvider() + "</src>";
			//
			// if (satellites > 0) {
			// waypoint = waypoint + "<sat>" + String.valueOf(satellites)
			// + "</sat>";
			// }
			//
			// waypoint = waypoint + "</wpt></gpx>";

			RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
			gpxLock = raf.getChannel().lock();
			raf.seek(startPosition);
			raf.write(trackPoint.getBytes());
			gpxLock.release();
			raf.close();

		}
		catch (Exception e)
		{
			Utilities.LogError("Gpx10FileLogger.Write", e);
			throw new Exception("Could not write to GPX file");
//			Log.e("Main", callingClient.getString(R.string.could_not_write_to_file) + e.getMessage());
//			callingClient.SetStatus(callingClient.getString(R.string.could_not_write_to_file)
//					+ e.getMessage());
		}

		
	}
	
	
	private String GetTrackPointXml(Location loc, String dateTimeString)
	{
		String track = "";
		if (Session.shouldAddNewTrackSegment())
		{
			track = track + "<trkseg>";
		}

		track = track + "<trkpt lat=\"" + String.valueOf(loc.getLatitude()) + "\" lon=\""
				+ String.valueOf(loc.getLongitude()) + "\">";

		if (loc.hasAltitude())
		{
			track = track + "<ele>" + String.valueOf(loc.getAltitude()) + "</ele>";
		}

		if (loc.hasBearing())
		{
			track = track + "<course>" + String.valueOf(loc.getBearing()) + "</course>";
		}

		if (loc.hasSpeed())
		{
			track = track + "<speed>" + String.valueOf(loc.getSpeed()) + "</speed>";
		}

		track = track + "<src>" + loc.getProvider() + "</src>";

		if(Session.getSatelliteCount()>0)
		{
			track = track + "<sat>" + String.valueOf(Session.getSatelliteCount()) + "</sat>";
		}

		track = track + "<time>" + dateTimeString + "</time>";

		track = track + "</trkpt>";

		track = track + "</trkseg></trk></gpx>";

		return track;
	}

	@Override
	public void Annotate(String description) throws Exception
	{
		if (!gpxFile.exists())
		{
			return;
		}
		int offsetFromEnd = 29;

		long startPosition = gpxFile.length() - offsetFromEnd;

		description = "<name>" + description + "</name><desc>" + description
				+ "</desc></trkpt></trkseg></trk></gpx>";
		RandomAccessFile raf = null;
		try
		{
			raf = new RandomAccessFile(gpxFile, "rw");
			gpxLock = raf.getChannel().lock();
			raf.seek(startPosition);
			raf.write(description.getBytes());
			gpxLock.release();
			raf.close();
		}
		catch (Exception e)
		{
			Utilities.LogError("Gpx10FileLogger.Annotate", e);
			throw new Exception("Could not annotate GPX file");
		}
		
	}

	
	
}
