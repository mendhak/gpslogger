package com.mendhak.gpslogger.loggers.customurl

import android.test.suitebuilder.annotation.SmallTest
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat


@SmallTest
@RunWith(MockitoJUnitRunner::class)
class CustomUrlJobTest {


    @Test
    @Throws(Exception::class)
    fun getHttpPostBodyFromUrl_WhenNoQuerystring_EmptyBody() {
        val job = CustomUrlJob(CustomUrlRequest("", "POST"), "", "", null)
        val body = job.getHttpPostBodyFromUrl("http://localhost/")


        val buffer = Buffer()
        body.writeTo(buffer)
        val actual = buffer.readUtf8()

        assertThat("Body should be empty", actual, `is`(""))

    }


    @Test
    @Throws(Exception::class)
    fun getHttpPostBodyFromUrl_BasicLatLong_LatLongInBody() {
        val job = CustomUrlJob(CustomUrlRequest("", "POST"), "", "", null)
        val body = job.getHttpPostBodyFromUrl("http://localhost/?lat=11&lon=22")

        val buffer = Buffer()
        body.writeTo(buffer)
        val actual = buffer.readUtf8()

        assertThat("Body should contain lat long", actual, `is`("lat=11&lon=22"))

    }

    @Test
    @Throws(Exception::class)
    fun getHttpPostBodyFromUrl_Time_IsEncoded() {
        val job = CustomUrlJob(CustomUrlRequest("", "POST"), "", "", null)
        val body = job.getHttpPostBodyFromUrl("http://localhost/?t=2017-06-03T11:15:09Z")

        val buffer = Buffer()
        body.writeTo(buffer)
        val actual = buffer.readUtf8()

        assertThat("Body should contain lat long", actual, `is`("t=2017-06-03T11%3A15%3A09Z"))

    }


}