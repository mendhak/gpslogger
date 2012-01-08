package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Kml22FileLogger implements IFileLogger
{
    protected final static Object lock = new Object();
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private final boolean useSatelliteTime;
    private final boolean addNewTrackSegment;
    private final File kmlFile;

    public Kml22FileLogger(File kmlFile, boolean useSatelliteTime, boolean addNewTrackSegment)
    {
        this.useSatelliteTime = useSatelliteTime;
        this.kmlFile = kmlFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }


    public void Write(Location loc) throws Exception
    {
        Kml22WriteHandler writeHandler = new Kml22WriteHandler(useSatelliteTime, loc, kmlFile, addNewTrackSegment);
        EXECUTOR.execute(writeHandler);
    }

    public void Annotate(String description) throws Exception
    {
        Kml22AnnotateHandler annotateHandler = new Kml22AnnotateHandler(kmlFile, description);
        EXECUTOR.execute(annotateHandler);
    }
}

class Kml22AnnotateHandler implements Runnable
{
    File kmlFile;
    String description;

    public Kml22AnnotateHandler(File kmlFile, String description)
    {
        this.kmlFile = kmlFile;
        this.description = description;
    }


    @Override
    public void run()
    {
        if (!kmlFile.exists())
        {
            return;
        }

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(kmlFile);

            Node documentNode = doc.getElementsByTagName("Document").item(0);
            Node newPlacemark = doc.createElement("Placemark");

            Node nameNode = doc.createElement("name");
            nameNode.appendChild(doc.createTextNode(description));

            Node pointNode = doc.createElement("Point");
            Node coordinatesNode = doc.createElement("coordinates");

            //Find the latest coordinates from the list of gx:coords.
            NodeList gxCoords = doc.getElementsByTagNameNS("http://www.google.com/kml/ext/2.2", "coord");
            Node latestCoordinates = gxCoords.item(gxCoords.getLength() - 1);
            coordinatesNode.appendChild(doc.createTextNode(latestCoordinates.getTextContent().replace(" ", ",")));

            pointNode.appendChild(coordinatesNode);
            newPlacemark.appendChild(nameNode);
            newPlacemark.appendChild(pointNode);

            documentNode.appendChild(newPlacemark);

            synchronized (Kml22FileLogger.lock)
            {

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                Result output = new StreamResult(kmlFile);
                Source input = new DOMSource(doc);

                transformer.transform(input, output);
            }
        }
        catch (Exception e)
        {
            Utilities.LogError("Kml22FileLogger.Annotate", e);
        }
    }
}

class Kml22WriteHandler implements Runnable
{

    boolean useSatelliteTime;
    boolean addNewTrackSegment;
    File kmlFile;
    Location loc;


    public Kml22WriteHandler(boolean useSatelliteTime, Location loc, File kmlFile, boolean addNewTrackSegment)
    {

        this.useSatelliteTime = useSatelliteTime;
        this.loc = loc;
        this.kmlFile = kmlFile;
        this.addNewTrackSegment = addNewTrackSegment;
    }


    @Override
    public void run()
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

            if (!kmlFile.exists())
            {
                kmlFile.createNewFile();

                FileOutputStream initialWriter = new FileOutputStream(kmlFile, true);
                BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);


                String initialXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">"
                        + "<Document>"
                        + "<name>" + dateTimeString + "</name>"
                        + "<Placemark><gx:Track></gx:Track></Placemark>"
                        + "</Document></kml>";
                initialOutput.write(initialXml.getBytes());
                initialOutput.flush();
                initialOutput.close();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(kmlFile);

            Node documentNode = doc.getElementsByTagName("Document").item(0);

            NodeList trackNodes = doc.getElementsByTagNameNS("http://www.google.com/kml/ext/2.2", "Track");
            Node gxTrack;

            if (addNewTrackSegment || trackNodes.getLength() == 0)
            {
                Node placeMark = doc.createElement("Placemark");
                documentNode.appendChild(placeMark);
                gxTrack = doc.createElementNS("http://www.google.com/kml/ext/2.2", "gx:Track");
                placeMark.appendChild(gxTrack);

            }
            else
            {
                gxTrack = trackNodes.item(trackNodes.getLength() - 1);
            }


            Node when = doc.createElement("when");
            Node gxCoord = doc.createElementNS("http://www.google.com/kml/ext/2.2", "gx:coord");
            gxCoord.appendChild(doc.createTextNode(String.valueOf(loc.getLongitude()) + " "
                    + String.valueOf(loc.getLatitude()) + " " + String.valueOf(loc.getAltitude())));
            gxTrack.appendChild(when);
            when.appendChild(doc.createTextNode(dateTimeString));
            gxTrack.appendChild(gxCoord);

            synchronized (Kml22FileLogger.lock)
            {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                Result output = new StreamResult(kmlFile);
                Source input = new DOMSource(doc);

                transformer.transform(input, output);
            }

        }
        catch (Exception e)
        {
            Utilities.LogError("Kml22FileLogger.Write", e);
        }
    }
}
