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
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


class Gpx10FileLogger implements IFileLogger
{
	private FileLock gpxLock;
	private File gpxFile = null;
	private boolean useSatelliteTime = false;
	private boolean addNewTrackSegment;
    private int satelliteCount;
	
	Gpx10FileLogger(File gpxFile, boolean useSatelliteTime, boolean addNewTrackSegment, int satelliteCount)
	{
		this.gpxFile = gpxFile;
		this.useSatelliteTime = useSatelliteTime;
		this.addNewTrackSegment = addNewTrackSegment;
        this.satelliteCount = satelliteCount;
	}


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
				initialOutput.flush();
				initialOutput.close();
			}


            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(gpxFile);

            Node trkSegNode;

            NodeList trkSegNodeList = doc.getElementsByTagName("trkseg");

            if(addNewTrackSegment || trkSegNodeList.getLength()==0)
            {
                NodeList trkNodeList = doc.getElementsByTagName("trk");
                trkSegNode = doc.createElement("trkseg");
                trkNodeList.item(0).appendChild(trkSegNode);
            }
            else
            {
                trkSegNode = trkSegNodeList.item(trkSegNodeList.getLength()-1);
            }

            Element trkptNode = doc.createElement("trkpt");

            Attr latAttribute = doc.createAttribute("lat");
            latAttribute.setValue(String.valueOf(loc.getLatitude()));
            trkptNode.setAttributeNode(latAttribute);

            Attr lonAttribute = doc.createAttribute("lon");
            lonAttribute.setValue(String.valueOf(loc.getLongitude()));
            trkptNode.setAttributeNode(lonAttribute);

            if(loc.hasAltitude())
            {
                Node eleNode = doc.createElement("ele");
                eleNode.appendChild(doc.createTextNode(String.valueOf(loc.getAltitude())));
                trkptNode.appendChild(eleNode);
            }

            if(loc.hasBearing())
            {
                Node courseNode = doc.createElement("course");
                courseNode.appendChild(doc.createTextNode(String.valueOf(loc.getBearing())));
                trkptNode.appendChild(courseNode);
            }

            if(loc.hasSpeed())
            {
                Node speedNode = doc.createElement("speed");
                speedNode.appendChild(doc.createTextNode(String.valueOf(loc.getSpeed())));
                trkptNode.appendChild(speedNode);
            }


            Node srcNode = doc.createElement("src");
            srcNode.appendChild(doc.createTextNode(loc.getProvider()));
            trkptNode.appendChild(srcNode);

            if(Session.getSatelliteCount() > 0)
            {
                Node satNode = doc.createElement("sat");
                satNode.appendChild(doc.createTextNode(String.valueOf(satelliteCount)));
                trkptNode.appendChild(satNode);
            }


            Node timeNode = doc.createElement("time");
            timeNode.appendChild(doc.createTextNode(dateTimeString));
            trkptNode.appendChild(timeNode);

            trkSegNode.appendChild(trkptNode);

            String newFileContents = Utilities.GetStringFromNode(doc);

			RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
			gpxLock = raf.getChannel().lock();
			raf.write(newFileContents.getBytes());
			gpxLock.release();
			raf.close();

		}
		catch (Exception e)
		{
			Utilities.LogError("Gpx10FileLogger.Write", e);
			throw new Exception("Could not write to GPX file");
		}

		
	}


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
