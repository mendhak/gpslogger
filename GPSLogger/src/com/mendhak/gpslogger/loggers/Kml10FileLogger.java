package com.mendhak.gpslogger.loggers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.mendhak.gpslogger.common.Utilities;
import android.location.Location;


class Kml10FileLogger implements IFileLogger
{

    private boolean useSatelliteTime;
    private File kmlFile;
    private FileLock kmlLock;

    Kml10FileLogger(File kmlFile, boolean useSatelliteTime)
    {
        this.useSatelliteTime = useSatelliteTime;
        this.kmlFile = kmlFile;
    }

    public void Write(Location loc) throws Exception
    {
        try
        {

            Date now;

            if(useSatelliteTime)
            {
                now = new Date(loc.getTime());
            }
            else
            {
                now = new Date();
            }

            String dateTimeString = Utilities.GetIsoDateTime(now);

            if(!kmlFile.exists())
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
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(kmlFile);

            NodeList coordinatesList = doc.getElementsByTagName("coordinates");

            if(coordinatesList.item(0) != null)
            {
                Node coordinates = coordinatesList.item(0);
                Node coordTextNode = coordinates.getFirstChild();

                if(coordTextNode == null)
                {
                    coordTextNode = doc.createTextNode("");
                    coordinates.appendChild(coordTextNode);
                }

                String coordText = coordinates.getFirstChild().getNodeValue();
                coordText = coordText + "\n" + String.valueOf(loc.getLongitude()) + ","
                        + String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getAltitude());
                coordinates.getFirstChild().setNodeValue(coordText);

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
            Node newCoordTextNode = doc.createTextNode("");
            newCoords.appendChild(newCoordTextNode);

            newCoords.getFirstChild().setNodeValue(String.valueOf(loc.getLongitude()) + ","
                    + String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getAltitude()));
            newPoint.appendChild(newCoords);

            newPlacemark.appendChild(newPoint);

            documentNode.appendChild(newPlacemark);

            String newFileContents = Utilities.GetStringFromNode(doc);

            RandomAccessFile raf = new RandomAccessFile(kmlFile, "rw");
            kmlLock = raf.getChannel().lock();
            raf.write(newFileContents.getBytes());
            kmlLock.release();
            raf.close();

        }
        catch(Exception e)
        {
            Utilities.LogError("Kml10FileLogger.Write", e);
            throw new Exception("Could not write to KML file");
        }

    }


    public void Annotate(String description) throws Exception
    {

        if(!kmlFile.exists())
        {
            return;
        }

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(kmlFile);

            NodeList placemarkList = doc.getElementsByTagName("Placemark");
            Node lastPlacemark = placemarkList.item(placemarkList.getLength() - 1);

            Node annotation = doc.createElement("name");
            Node annotationText = doc.createTextNode("");
            annotation.appendChild(annotationText);
            annotation.getFirstChild().setNodeValue(description);

            lastPlacemark.appendChild(annotation);

            String newFileContents = Utilities.GetStringFromNode(doc);

            RandomAccessFile raf = new RandomAccessFile(kmlFile, "rw");
            kmlLock = raf.getChannel().lock();
            raf.write(newFileContents.getBytes());
            kmlLock.release();
            raf.close();
        }
        catch(Exception e)
        {
            Utilities.LogError("Kml10FileLogger.Annotate", e);
            throw new Exception("Could not annotate KML file");
        }
    }


}
