package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


class Kml10FileLogger implements IFileLogger
{

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());

    protected final static Object lock = new Object();
    private final boolean useSatelliteTime;
    private final File kmlFile;

    Kml10FileLogger(File kmlFile, boolean useSatelliteTime)
    {
        this.useSatelliteTime = useSatelliteTime;
        this.kmlFile = kmlFile;
    }

    public void Write(Location loc) throws Exception
    {
        Kml10WriteHandler writeHandler = new Kml10WriteHandler(useSatelliteTime, loc, kmlFile);
        EXECUTOR.execute(writeHandler);
    }


    public void Annotate(String description, Location loc) throws Exception
    {
        Kml10AnnotateHandler annotateHandler = new Kml10AnnotateHandler(kmlFile, description);
        EXECUTOR.execute(annotateHandler);
    }

}


class Kml10AnnotateHandler implements Runnable
{
    File kmlFile;
    String description;

    public Kml10AnnotateHandler(File kmlFile, String description)
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
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(kmlFile);

            NodeList placemarkList = doc.getElementsByTagName("Placemark");
            Node lastPlacemark = placemarkList.item(placemarkList.getLength() - 1);

            Node annotation = doc.createElement("name");
            annotation.appendChild(doc.createTextNode(description));

            lastPlacemark.appendChild(annotation);

            String newFileContents = Utilities.GetStringFromNode(doc);

            synchronized (Kml10FileLogger.lock)
            {
                FileOutputStream fos = new FileOutputStream(kmlFile, false);
                fos.write(newFileContents.getBytes());
                fos.close();
            }


        }
        catch (Exception e)
        {
            Utilities.LogError("Kml10FileLogger.Annotate", e);
        }

    }
}

class Kml10WriteHandler implements Runnable
{

    boolean useSatelliteTime;
    Location loc;
    File kmlFile;

    public Kml10WriteHandler(boolean useSatelliteTime, Location loc, File kmlFile)
    {
        this.useSatelliteTime = useSatelliteTime;
        this.loc = loc;
        this.kmlFile = kmlFile;
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

                String initialXml = "<?xml version=\"1.0\"?>"
                        + "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>"
                        + "<Placemark><LineString><extrude>1</extrude><tessellate>1</tessellate>"
                        + "<altitudeMode>absolute</altitudeMode>"
                        + "<coordinates></coordinates></LineString></Placemark>"
                        + "</Document></kml>";
                initialOutput.write(initialXml.getBytes());
                initialOutput.flush();
                initialOutput.close();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(kmlFile);

            NodeList coordinatesList = doc.getElementsByTagName("coordinates");

            if (coordinatesList.item(0) != null)
            {
                Node coordinates = coordinatesList.item(0);
                Node coordTextNode = coordinates.getFirstChild();

                if (coordTextNode == null)
                {
                    coordTextNode = doc.createTextNode("");
                    coordinates.appendChild(coordTextNode);
                }

                String coordText = coordinates.getFirstChild().getNodeValue();
                coordText = coordText + "\n" + String.valueOf(loc.getLongitude()) + ","
                        + String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getAltitude());

                coordinates.removeChild(coordinates.getFirstChild());
                coordinates.appendChild(doc.createTextNode(coordText));
            }

            Node documentNode = doc.getElementsByTagName("Document").item(0);
            Node newPlacemark = doc.createElement("Placemark");

            Node timeStamp = doc.createElement("TimeStamp");
            Node whenNode = doc.createElement("when");
            Node whenNodeText = doc.createTextNode(dateTimeString);
            whenNode.appendChild(whenNodeText);
            timeStamp.appendChild(whenNode);
            newPlacemark.appendChild(timeStamp);

            Node newPoint = doc.createElement("Point");

            Node newCoords = doc.createElement("coordinates");
            newCoords.appendChild(doc.createTextNode(String.valueOf(loc.getLongitude()) + ","
                    + String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getAltitude())));

            newPoint.appendChild(newCoords);

            newPlacemark.appendChild(newPoint);

            documentNode.appendChild(newPlacemark);

            String newFileContents = Utilities.GetStringFromNode(doc);

            synchronized (Kml10FileLogger.lock)
            {
                FileOutputStream fos = new FileOutputStream(kmlFile, false);
                fos.write(newFileContents.getBytes());
                fos.close();
            }

        }
        catch (Exception e)
        {
            Utilities.LogError("Kml10FileLogger.Write", e);
        }
    }
}
