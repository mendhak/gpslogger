package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


class Gpx10FileLogger implements IFileLogger
{
    protected final static Object lock = new Object();

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private File gpxFile = null;
    private boolean useSatelliteTime = false;
    private final boolean addNewTrackSegment;
    private final int satelliteCount;

    Gpx10FileLogger(File gpxFile, boolean useSatelliteTime, boolean addNewTrackSegment, int satelliteCount)
    {
        this.gpxFile = gpxFile;
        this.useSatelliteTime = useSatelliteTime;
        this.addNewTrackSegment = addNewTrackSegment;
        this.satelliteCount = satelliteCount;
    }


    public void Write(Location loc) throws Exception
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

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment, satelliteCount);
        EXECUTOR.execute(writeHandler);
    }

    public void Annotate(String description) throws Exception
    {
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(description, gpxFile);
        EXECUTOR.execute(annotateHandler);
    }


}

class Gpx10AnnotateHandler implements Runnable
{
    String description;
    File gpxFile;

    public Gpx10AnnotateHandler(String description, File gpxFile)
    {
        this.description = description;
        this.gpxFile = gpxFile;
    }

    @Override
    public void run()
    {

        synchronized (Gpx10FileLogger.lock)
        {
            if (!gpxFile.exists())
            {
                return;
            }

            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(gpxFile);

                NodeList trkptNodeList = doc.getElementsByTagName("trkpt");
                Node lastTrkPt = trkptNodeList.item(trkptNodeList.getLength() - 1);

                Node nameNode = doc.createElement("name");
                nameNode.appendChild(doc.createTextNode(description));
                lastTrkPt.appendChild(nameNode);

                Node descNode = doc.createElement("desc");
                descNode.appendChild(doc.createTextNode(description));
                lastTrkPt.appendChild(descNode);

                String newFileContents = Utilities.GetStringFromNode(doc);


                FileOutputStream fos = new FileOutputStream(gpxFile, false);
                fos.write(newFileContents.getBytes());
                fos.close();


            }
            catch (Exception e)
            {
                Utilities.LogError("Gpx10FileLogger.Annotate", e);
            }
        }
    }
}


class Gpx10WriteHandler implements Runnable
{
    String dateTimeString;
    Location loc;
    private File gpxFile = null;
    private boolean addNewTrackSegment;
    private int satelliteCount;

    public Gpx10WriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment, int satelliteCount)
    {
        this.dateTimeString = dateTimeString;
        this.addNewTrackSegment = addNewTrackSegment;
        this.gpxFile = gpxFile;
        this.loc = loc;
        this.satelliteCount = satelliteCount;
    }


    @Override
    public void run()
    {
        synchronized (Gpx10FileLogger.lock)
        {

            try
            {
                if (!gpxFile.exists())
                {
                    gpxFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(gpxFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    String initialXml = "<?xml version=\"1.0\"?>"
                            + "<gpx version=\"1.0\" creator=\"GPSLogger - http://gpslogger.mendhak.com/\" "
                            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                            + "xmlns=\"http://www.topografix.com/GPX/1/0\" "
                            + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 "
                            + "http://www.topografix.com/GPX/1/0/gpx.xsd\">"
                            + "<time>" + dateTimeString + "</time>" + "<bounds />" + "<trk></trk></gpx>";
                    initialOutput.write(initialXml.getBytes());
                    initialOutput.flush();
                    initialOutput.close();
                }


                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(gpxFile);

                Node trkSegNode;

                NodeList trkSegNodeList = doc.getElementsByTagName("trkseg");

                if (addNewTrackSegment || trkSegNodeList.getLength() == 0)
                {
                    NodeList trkNodeList = doc.getElementsByTagName("trk");
                    trkSegNode = doc.createElement("trkseg");
                    trkNodeList.item(0).appendChild(trkSegNode);
                }
                else
                {
                    trkSegNode = trkSegNodeList.item(trkSegNodeList.getLength() - 1);
                }

                Element trkptNode = doc.createElement("trkpt");

                Attr latAttribute = doc.createAttribute("lat");
                latAttribute.setValue(String.valueOf(loc.getLatitude()));
                trkptNode.setAttributeNode(latAttribute);

                Attr lonAttribute = doc.createAttribute("lon");
                lonAttribute.setValue(String.valueOf(loc.getLongitude()));
                trkptNode.setAttributeNode(lonAttribute);

                if (loc.hasAltitude())
                {
                    Node eleNode = doc.createElement("ele");
                    eleNode.appendChild(doc.createTextNode(String.valueOf(loc.getAltitude())));
                    trkptNode.appendChild(eleNode);
                }

                Node timeNode = doc.createElement("time");
                timeNode.appendChild(doc.createTextNode(dateTimeString));
                trkptNode.appendChild(timeNode);

                trkSegNode.appendChild(trkptNode);

                if (loc.hasBearing())
                {
                    Node courseNode = doc.createElement("course");
                    courseNode.appendChild(doc.createTextNode(String.valueOf(loc.getBearing())));
                    trkptNode.appendChild(courseNode);
                }

                if (loc.hasSpeed())
                {
                    Node speedNode = doc.createElement("speed");
                    speedNode.appendChild(doc.createTextNode(String.valueOf(loc.getSpeed())));
                    trkptNode.appendChild(speedNode);
                }


                Node srcNode = doc.createElement("src");
                srcNode.appendChild(doc.createTextNode(loc.getProvider()));
                trkptNode.appendChild(srcNode);

                if (Session.getSatelliteCount() > 0)
                {
                    Node satNode = doc.createElement("sat");
                    satNode.appendChild(doc.createTextNode(String.valueOf(satelliteCount)));
                    trkptNode.appendChild(satNode);
                }


                String newFileContents = Utilities.GetStringFromNode(doc);


                FileOutputStream fos = new FileOutputStream(gpxFile, false);
                fos.write(newFileContents.getBytes());
                fos.close();

            }
            catch (Exception e)
            {
                Utilities.LogError("Gpx10FileLogger.Write", e);
            }

        }

    }
}


