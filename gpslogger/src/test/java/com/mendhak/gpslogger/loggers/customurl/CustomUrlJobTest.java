package com.mendhak.gpslogger.loggers.customurl;

import android.test.suitebuilder.annotation.SmallTest;
import okhttp3.RequestBody;
import okio.Buffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CustomUrlJobTest {


    @Test
    public void getHttpPostBodyFromUrl_WhenNoQuerystring_EmptyBody() throws Exception {
        CustomUrlJob job = new CustomUrlJob("", "", "", null, "POST");
        RequestBody body = job.getHttpPostBodyFromUrl("http://localhost/");


        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        String actual = buffer.readUtf8();

        assertThat("Body should be empty", actual, is(""));

    }


    @Test
    public void getHttpPostBodyFromUrl_BasicLatLong_LatLongInBody() throws Exception {
        CustomUrlJob job = new CustomUrlJob("", "", "", null, "POST");
        RequestBody body = job.getHttpPostBodyFromUrl("http://localhost/?lat=11&lon=22");

        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        String actual = buffer.readUtf8();

        assertThat("Body should contain lat long", actual, is("lat=11&lon=22"));

    }

    @Test
    public void getHttpPostBodyFromUrl_Time_IsEncoded() throws Exception {
        CustomUrlJob job = new CustomUrlJob("", "", "", null, "POST");
        RequestBody body = job.getHttpPostBodyFromUrl("http://localhost/?t=2017-06-03T11:15:09Z");

        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        String actual = buffer.readUtf8();

        assertThat("Body should contain lat long", actual, is("t=2017-06-03T11%3A15%3A09Z"));

    }


}