package com.mendhak.gpslogger.senders.customurl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.location.Location;
import androidx.test.filters.SmallTest;
import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import java.time.ZoneOffset;
import java.util.TimeZone;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CustomUrlManagerTest {


    @Test
    public void getFormattedUrl_WhenPlaceholders_ValuesSubstituted() throws Exception {

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .putExtra("satellites", 9)
                .withAltitude(45)
                .withAccuracy(8)
                .withBearing(359)
                .withSpeed(9001)
                .withTime(1457205869949l)
                .build();


        CustomUrlManager manager = new CustomUrlManager(null);

        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.111&sat=9&desc=blah&alt=45.0&acc=8.0&dir=359.0&prov=MOCK&spd=9001.0&time=2016-03-05T19:24:29.949Z&battery=91.0&androidId=22&serial=SRS11&activity=";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%act";
        assertThat("Placeholders are substituted", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "blah", "22", 91,
                false, "SRS11", 0, "", "",
                27), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenDistanceAvailable_FormattedWithoutDecimal() throws Exception {

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .build();


        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.111&dist=27";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&dist=%DIST";
        assertThat("Distance formatted without decimal", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "blah", "22", 91,
                false, "SRS11", 0, "", "",
                27), is(expected));

    }


    @Test
    public void getFormattedUrl_WhenValuesMissing_UrlReturnsWhatsAvailable() throws Exception {


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withTime(1457205869949l)
                .build();


        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.111&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT";
        assertThat("Placeholders are substituted", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 0, "", "",
                0), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenTimeStamp_UseUnixEpoch() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=&epoch=1457205869";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT&epoch=%TIMESTAMP";
        assertThat("Unix timestamp is in seconds", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0, false, "", 0, "", "", 0), is(expected));

        expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=&epoch=1457205869000";
        urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT&epoch=%TIMESTAMP000";

        assertThat("Unix timestamp with 000 to fake milliseconds", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0, false,
                "", 0, "", "", 0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenTimeOffsetParameter_UseISODateFormat() throws Exception {
        //This sets the timezone for the JVM
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.ofHours(2)));

        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&timeoff=2016-03-05T21:24:29.949%2B02:00";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&timeoff=%TIMEOFFSET";

        assertThat("TIMEOFFSET parameter is substituted with ISO Date Format, with time offset",
                manager.getFormattedTextblock(urlTemplate, new SerializableLocation(loc), "",
                        "", 0, false, "", 0,
                        "", "", 0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenSerializableLocationHasTimeStampWithOffset_PreferredOverDerivedTimeStamp() throws Exception {
        //This sets the timezone for the JVM
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.ofHours(2)));

        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l)
                .putExtra(BundleConstants.TIME_WITH_OFFSET, "2016-05-02T20:14:19.349+03:00")
                .build();

        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&timeoff=2016-05-02T20:14:19.349%2B03:00";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&timeoff=%TIMEOFFSET";

        assertThat("TIMEOFFSET parameter is substituted with value from bundle",
                manager.getFormattedTextblock(urlTemplate, new SerializableLocation(loc),
                        "", "", 0, false, "",
                        0, "", "", 0), is(expected));
    }


    @Test
    public void getFormattedUrl_WhenDateParameter_UseISODateFormat() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&date=2016-03-05";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&date=%DATE";

        assertThat("DATE parameter is substituted with ISO Date Format",
                manager.getFormattedTextblock(urlTemplate,
                        new SerializableLocation(loc), "", "", 0,
                        false, "", 0, "",
                        "", 0), is(expected));


    }

    @Test
    public void getFormattedUrl_WhenStartTime_AddSessionStartTime() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&stst=1495884681";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&stst=%STARTTIMESTAMP";
        assertThat("Start timestamp is in seconds", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l, "", "", 0),
                is(expected));

        expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&stst=0";
        urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&stst=%STARTTIMESTAMP";
        assertThat("Absence of start timestamp recorded as 0", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 0l, "", "", 0),
                is(expected));
    }

    @Test
    public void getFormattedUrl_WhenFilename_AddFilename() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&fn=20170527abc";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&fn=%FILENAME";
        assertThat("Start timestamp is in seconds", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l, "20170527abc",
                "", 0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenProfilename_AddProfileName() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&fn=20170527abc&profile=Default+Profile";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&fn=%FILENAME&profile=%PROFILE";
        assertThat("Profile name is provided", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l, "20170527abc",
                "Default Profile", 0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenBatteryInfoAvailable_AddBatteryInfo() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&fn=20170527abc&batt=81.0&charging=true";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&fn=%FILENAME&batt=%BATT&charging=%ISCHARGING";
        assertThat("Profile name is provided", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 81, true, "",
                1495884681283l, "20170527abc", "Default Profile",
                0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenHDOPAvailable_AddDopValues() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .putExtra(BundleConstants.HDOP, "4").withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&horizontal=4";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&horizontal=%HDOP";
        assertThat("HDOP value is provided", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l,
                "20170527abc", "Default Profile", 0),
                is(expected));

    }

    @Test
    public void getFormattedUrl_WhenVDOPAvailable_AddDopValues() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .putExtra(BundleConstants.VDOP, "19").withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&horizontal=&vertical=19";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&horizontal=%HDOP&vertical=%VDop";
        assertThat("VDOP value is provided", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l,
                "20170527abc", "Default Profile", 0),
                is(expected));
    }

    @Test
    public void getFormattedUrl_WhenPDOPAvailable_AddDopValues() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .putExtra(BundleConstants.PDOP, "2")
                .withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&horizontal=&positional=2";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&horizontal=%HDOP&positional=%pdop";
        assertThat("PDOP value is provided", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l,
                "20170527abc", "Default Profile", 0),
                is(expected));
    }


    @Test
    public void getFormattedUrl_WhenPostBody_ValuesSubstituted() throws Exception {

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .putExtra("satellites", 9)
                .withAltitude(45)
                .withAccuracy(8)
                .withBearing(359)
                .withSpeed(9001)
                .withTime(1457205869949l)
                .build();


        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "This my post body\nlat=12.193&lon=19.111&sat=9&desc=blah&alt=45.0&acc=8.0&dir=359.0&prov=MOCK&spd=9001.0&time=2016-03-05T19:24:29.949Z&battery=91.0&androidId=22&serial=SRS11&activity=&dist=27";
        String urlTemplate = "This my post body\nlat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%act&dist=%DIST";
        assertThat("Post body parameters are substituted",
                manager.getFormattedTextblock(urlTemplate,
                        new SerializableLocation(loc), "blah", "22", 91,
                        false, "SRS11", 0,
                        "", "", 27.5),
                is(expected));
    }

    @Test
    public void getFormattedUrl_WhenALLParameters_AllKeyValuesAddedDirectly() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .putExtra(BundleConstants.VDOP, "19").withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0" +
                "&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&timestamp=1457205869" +
                "&timeoffset=2016-03-05T21:24:29.949%2B02:00&time=2016-03-05T19:24:29.949Z" +
                "&starttimestamp=1495884681&date=2016-03-05&batt=0.0&ischarging=false&aid=&ser=" +
                "&act=&filename=20170527abc&profile=Default+Profile&hdop=&vdop=19&pdop=&dist=0&";
        String urlTemplate = "http://192.168.1.65:8000/test?%ALL";
        assertThat("ALL parameter is substituted by all key values", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l,
                "20170527abc", "Default Profile", 0),
                is(expected));
    }

    @Test
    public void getFormattedUrl_WhenALLParametersInBody_AllKeyValuesAddedDirectly() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456)
                .putExtra(BundleConstants.VDOP, "19").withTime(1457205869949l).build();
        CustomUrlManager manager = new CustomUrlManager(null);
        String expected = "lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK" +
                "&spd=0.0&timestamp=1457205869&timeoffset=2016-03-05T21:24:29.949%2B02:00" +
                "&time=2016-03-05T19:24:29.949Z&starttimestamp=1495884681&date=2016-03-05" +
                "&batt=0.0&ischarging=false&aid=&ser=&act=&filename=20170527abc" +
                "&profile=Default+Profile&hdop=&vdop=19&pdop=&dist=0&";
        String urlTemplate = "%ALL";
        assertThat("ALL parameter is substituted by all key values", manager.getFormattedTextblock(urlTemplate,
                new SerializableLocation(loc), "", "", 0,
                false, "", 1495884681283l,
                "20170527abc", "Default Profile", 0),
                is(expected));
    }

}
