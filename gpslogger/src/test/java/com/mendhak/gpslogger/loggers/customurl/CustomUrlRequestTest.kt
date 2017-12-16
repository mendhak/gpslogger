package com.mendhak.gpslogger.loggers.customurl

import org.hamcrest.CoreMatchers.`is`
import org.junit.Test

import org.junit.Assert.*


class CustomUrlRequestTest {

    @Test
    fun getHttpMethod_IsAlwaysCapitalized() {
        var cur = CustomUrlRequest("http://example.com", "get")
        assertThat("Http Method should always be capitalized", cur.HttpMethod, `is`("GET"))

        cur = CustomUrlRequest("http://example.com", "GET")
        assertThat("Http Method should always be capitalized", cur.HttpMethod, `is`("GET"))

        cur = CustomUrlRequest("http://example.com", "put")
        assertThat("Http Method should always be capitalized", cur.HttpMethod, `is`("PUT"))

    }

    @Test
    fun getHttpMethod_CustomMethodNames_AlwaysCapitalized() {
        var cur = CustomUrlRequest("http://example.com", "blah")
        assertThat("Custom HTTP Methods should also be capitalized", cur.HttpMethod, `is`("BLAH"))
    }

    @Test
    fun getBasicAuth_BasicAuthPresent_ReturnsUsernamePassword() {
        var cur = CustomUrlRequest("")
        val (actualUsername, actualPassword) = cur.getBasicAuthCredentialsFromUrl("http://bob:hunter2@example.com/%LOG")
        assertThat("Basic auth username is detected", actualUsername, `is`("bob"))
        assertThat("Basic auth password is detected", actualPassword, `is`("hunter2"))
    }

    @Test
    fun getBasicAuth_NoCredentials_ReturnsEmptyPair() {
        var cur = CustomUrlRequest("")
        val (actualUsername, actualPassword) = cur.getBasicAuthCredentialsFromUrl("http://example.com/%LOG")
        assertThat("Basic auth username is detected", actualUsername, `is`(""))
        assertThat("Basic auth password is detected", actualPassword, `is`(""))
    }

    @Test
    fun removeCredentialsFromUrl_CredentialsPresent_RemovedFromUrl(){
        var cur = CustomUrlRequest("http://bob:hunter2@example.com/%LOG")
        assertThat("Credentials removed from URL", cur.LogURL, `is`("http://example.com/%LOG"))
    }




}