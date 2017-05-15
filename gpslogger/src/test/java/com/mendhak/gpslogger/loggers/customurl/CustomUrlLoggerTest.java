package com.mendhak.gpslogger.loggers.customurl;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;

import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.loggers.MockLocations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CustomUrlLoggerTest {

    @Test
    public void getFormattedUrl_WhenPlaceholders_ValuesSubstituted() throws Exception {

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .putExtra("satellites",9)
                .withAltitude(45)
                .withAccuracy(8)
                .withBearing(359)
                .withSpeed(9001)
                .withTime(1457205869949l)
                .putExtra(BundleConstants.DETECTED_ACTIVITY, "TILTED")
                .build();


        CustomUrlLogger logger = new CustomUrlLogger("",0,"");
        String expected ="http://192.168.1.65:8000/test?lat=12.193&lon=19.111&sat=9&desc=blah&alt=45.0&acc=8.0&dir=359.0&prov=MOCK&spd=9001.0&time=2016-03-05T19:24:29.949Z&battery=91.0&androidId=22&serial=SRS11&activity=TILTED";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%act";
        assertThat("Placeholders are substituted", logger.getFormattedUrl(urlTemplate,loc, "blah", "22", 91, "SRS11"), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenValuesMissing_UrlReturnsWhatsAvailable() throws Exception {


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withTime(1457205869949l)
                .build();


        CustomUrlLogger logger = new CustomUrlLogger("",0,"");
        String expected ="http://192.168.1.65:8000/test?lat=12.193&lon=19.111&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT";
        assertThat("Placeholders are substituted", logger.getFormattedUrl(urlTemplate,loc, "", "", 0, ""), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenTimeStamp_UseUnixEpoch() throws Exception {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        CustomUrlLogger logger = new CustomUrlLogger("",0,"");
        String expected="http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=&epoch=1457205869";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT&epoch=%TIMESTAMP";
        assertThat("Unix timestamp is in seconds", logger.getFormattedUrl(urlTemplate, loc, "", "", 0, ""), is(expected));

        expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=&epoch=1457205869000";
        urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT&epoch=%TIMESTAMP000";

        assertThat("Unix timestamp with 000 to fake milliseconds", logger.getFormattedUrl(urlTemplate, loc, "", "", 0, ""), is(expected));

    }

    @Test
    public void getBasicAuth_BasicAuthPresent_ReturnsUsernamePassword() throws Exception {
        CustomUrlLogger logger = new CustomUrlLogger("",0,"");

        assertThat("Basic auth user pass are detected", logger.getBasicAuth("http://bob:Passw0rd@example.com/%SER").getKey(), is("bob") );
        assertThat("Basic auth user pass are detected", logger.getBasicAuth("http://bob:Passw0rd@example.com/%SER").getValue(), is("Passw0rd") );
    }

    @Test
    public void getBasicAuth_NoCredsPresent_ReturnsEmptyPair() throws Exception {
        CustomUrlLogger logger = new CustomUrlLogger("",0,"");

        assertThat("Basic auth user pass are absent", logger.getBasicAuth("http://example.com/%SER").getKey(), is("") );
        assertThat("Basic auth user pass are absent", logger.getBasicAuth("http://example.com/%SER").getValue(), is("") );
    }

    @Test
    public void removeCredentialsFromUrl_CredentialsPresent_RemovedFromUrl(){

        CustomUrlLogger logger = new CustomUrlLogger("",0,"");
        assertThat("Basic auth user pass are removed", logger.removeCredentialsFromUrl("http://bob:Passw0rd@example.com/%SER","bob","Passw0rd"), is("http://example.com/%SER") );

    }

}