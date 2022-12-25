package com.mendhak.gpslogger.loggers.gpx;

import androidx.test.filters.SmallTest;
import com.mendhak.gpslogger.BuildConfig;
import com.mendhak.gpslogger.common.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Gpx11WriteHandlerTest {
    @Test
    public void InitialXmlLength_Verify(){
        Gpx11WriteHandler writeHandler = new Gpx11WriteHandler(null, null, null, true);
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null,
                null, null,
                writeHandler.getBeginningXml(Strings.getIsoDateTime(new Date(1483054318298l))).length());


        String actual = writeHandler.getBeginningXml(Strings.getIsoDateTime(new Date(1483054318298l)));
        String expected =   "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><gpx version=\"1.1\" creator=\"GPSLogger " + BuildConfig.VERSION_CODE + " - http://gpslogger.mendhak.com/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v2\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v2 https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd \"><metadata><time>2016-12-29T23:31:58.298Z</time></metadata>";


        assertThat("InitialXml matches", actual, is(expected));
        assertThat("Initial XML Length is correct", actual.length(), is(554));
        assertThat("Initial XML length constant is set for others to use", actual.length(), is(annotateHandler.annotateOffset));
    }

}