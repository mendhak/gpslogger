package com.mendhak.gpslogger.helpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Date;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.Utilities;

public class FileLoggingHelper
{

	static GpsMainActivity mainActivity;
	FileLock gpxLock;
	FileLock kmlLock;
	public boolean allowDescription = false;

	public FileLoggingHelper(GpsMainActivity activity)
	{
		mainActivity = activity;
	}

	public void WriteToFile(Location loc)
	{

		if (!mainActivity.logToGpx && !mainActivity.logToKml)
		{
			return;
		}

		try
		{

			boolean brandNewFile = false;

			File gpxFolder = new File(
					Environment.getExternalStorageDirectory(), "GPSLogger");

			if (!gpxFolder.exists())
			{
				gpxFolder.mkdirs();
				brandNewFile = true;
			}

			if (mainActivity.logToGpx)
			{
				WriteToGpxFile(loc, gpxFolder, brandNewFile);
			}

			if (mainActivity.logToKml)
			{
				WriteToKmlFile(loc, gpxFolder, brandNewFile);

			}

			allowDescription = true;

		}
		catch (Exception e)
		{
			Log.e("Main", "Could not write file " + e.getMessage());
			mainActivity.SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	public void WriteToKmlFile(Location loc, File gpxFolder,
			boolean brandNewFile)
	{

		try
		{
			File kmlFile = new File(gpxFolder.getPath(),
					mainActivity.currentFileName + ".kml");

			if (!kmlFile.exists())
			{
				kmlFile.createNewFile();
				brandNewFile = true;
			}

			Date now;

			if (mainActivity.useSatelliteTime)
			{
				now = new Date(loc.getTime());
			}
			else
			{
				now = new Date();
			}

			String dateTimeString = Utilities.GetIsoDateTime(now);
			// SimpleDateFormat sdf = new
			// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			// String dateTimeString = sdf.format(now);

			if (brandNewFile)
			{
				FileOutputStream initialWriter = new FileOutputStream(kmlFile,
						true);
				BufferedOutputStream initialOutput = new BufferedOutputStream(
						initialWriter);

				String initialXml = "<?xml version=\"1.0\"?>"
						+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>"
						+ "</Document></kml>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			long startPosition = kmlFile.length() - 17;

			String placemark = "<Placemark><description>" + dateTimeString
					+ "</description><TimeStamp><when>" + dateTimeString
					+ "</when></TimeStamp>" + "<Point><coordinates>"
					+ String.valueOf(loc.getLongitude()) + ","
					+ String.valueOf(loc.getLatitude()) + ","
					+ String.valueOf(loc.getAltitude())
					+ "</coordinates></Point></Placemark></Document></kml>";

			RandomAccessFile raf = new RandomAccessFile(kmlFile, "rw");
			kmlLock = raf.getChannel().lock();
			raf.seek(startPosition);
			raf.write(placemark.getBytes());
			kmlLock.release();
			raf.close();

		}
		catch (IOException e)
		{
			Log.e("Main", "Could not write file " + e.getMessage());
			mainActivity.SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	public void WriteToGpxFile(Location loc, File gpxFolder,
			boolean brandNewFile)
	{

		try
		{
			File gpxFile = new File(gpxFolder.getPath(),
					mainActivity.currentFileName + ".gpx");

			if (!gpxFile.exists())
			{
				gpxFile.createNewFile();
				brandNewFile = true;
			}

			Date now;

			if (mainActivity.useSatelliteTime)
			{
				now = new Date(loc.getTime());
			}
			else
			{
				now = new Date();
			}

			String dateTimeString = Utilities.GetIsoDateTime(now);

			if (brandNewFile)
			{
				FileOutputStream initialWriter = new FileOutputStream(gpxFile,
						true);
				BufferedOutputStream initialOutput = new BufferedOutputStream(
						initialWriter);

				String initialXml = "<?xml version=\"1.0\"?>"
						+ "<gpx version=\"1.0\" creator=\"GPSLogger - http://gpslogger.mendhak.com/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">"
						+ "<time>" + dateTimeString + "</time>" + "<bounds />"
						+ "<trk></trk></gpx>";
				initialOutput.write(initialXml.getBytes());
				// initialOutput.write("\n".getBytes());
				initialOutput.flush();
				initialOutput.close();
			}

			int offsetFromEnd = (mainActivity.addNewTrackSegment) ? 12 : 21;

			long startPosition = gpxFile.length() - offsetFromEnd;

			String trackPoint = GetTrackPointXml(loc, dateTimeString);

			mainActivity.addNewTrackSegment = false;

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
		catch (IOException e)
		{
			Log.e("Main", "Could not write file " + e.getMessage());
			mainActivity.SetStatus("Could not write to file. " + e.getMessage());
		}

	}

	public String GetTrackPointXml(Location loc, String dateTimeString)
	{
		String track = "";
		if (mainActivity.addNewTrackSegment)
		{
			track = track + "<trkseg>";
		}

		track = track + "<trkpt lat=\"" + String.valueOf(loc.getLatitude())
				+ "\" lon=\"" + String.valueOf(loc.getLongitude()) + "\">";

		if (loc.hasAltitude())
		{
			track = track + "<ele>" + String.valueOf(loc.getAltitude())
					+ "</ele>";
		}

		if (loc.hasBearing())
		{
			track = track + "<course>" + String.valueOf(loc.getBearing())
					+ "</course>";
		}

		if (loc.hasSpeed())
		{
			track = track + "<speed>" + String.valueOf(loc.getSpeed())
					+ "</speed>";
		}

		track = track + "<src>" + loc.getProvider() + "</src>";

		if (mainActivity.satellites > 0)
		{
			track = track + "<sat>" + String.valueOf(mainActivity.satellites)
					+ "</sat>";
		}

		track = track + "<time>" + dateTimeString + "</time>";

		track = track + "</trkpt>";

		track = track + "</trkseg></trk></gpx>";

		return track;
	}

	public void Annotate()
	{

		if (!allowDescription)
		{
			Utilities.MsgBox(
					"Not yet",
					"You can't add a description until the next point has been logged to a file.",
					mainActivity);
			return;
		}

		AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);

		alert.setTitle("Add a description");
		alert.setMessage("Use only letters and numbers");

		// Set an EditText view to get user input
		final EditText input = new EditText(mainActivity);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{

				if (!mainActivity.logToGpx && !mainActivity.logToKml)
				{
					return;
				}

				final String desc = Utilities.CleanDescription(input.getText().toString());

				AddNoteToLastPoint(desc);

			}

		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				// Canceled.
			}
		});

		alert.show();

	}

	public void AddNoteToLastPoint(String desc)
	{

		File gpxFolder = new File(Environment.getExternalStorageDirectory(),
				"GPSLogger");

		if (!gpxFolder.exists())
		{
			return;
		}

		int offsetFromEnd;
		String description;
		long startPosition;

		if (mainActivity.logToGpx)
		{

			File gpxFile = new File(gpxFolder.getPath(),
					mainActivity.currentFileName + ".gpx");

			if (!gpxFile.exists())
			{
				return;
			}
			offsetFromEnd = 29;

			startPosition = gpxFile.length() - offsetFromEnd;

			description = "<name>" + desc + "</name><desc>" + desc
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

				mainActivity.SetStatus("Description added to point.");
				allowDescription = false;

			}
			catch (Exception e)
			{
				mainActivity.SetStatus("Couldn't write description to GPX file.");
			}

		}

		if (mainActivity.logToKml)
		{

			File kmlFile = new File(gpxFolder.getPath(),
					mainActivity.currentFileName + ".kml");

			if (!kmlFile.exists())
			{
				return;
			}

			offsetFromEnd = 37;

			description = "<name>" + desc
					+ "</name></Point></Placemark></Document></kml>";

			startPosition = kmlFile.length() - offsetFromEnd;
			try
			{
				RandomAccessFile raf = new RandomAccessFile(kmlFile, "rw");
				kmlLock = raf.getChannel().lock();
				raf.seek(startPosition);
				raf.write(description.getBytes());
				kmlLock.release();
				raf.close();

				allowDescription = false;
			}
			catch (Exception e)
			{
				mainActivity.SetStatus("Couldn't write description to KML file.");
			}

		}

		// </Point></Placemark></Document></kml>

	}

}
