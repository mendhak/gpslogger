package com.mendhak.gpslogger.loggers.customurl

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.core.IsNull
import org.junit.Test

import org.junit.Assert.*
import java.util.HashMap


class CustomUrlRequestTest {

    @Test
    fun getHttpMethod_IsAlwaysCapitalized() {
        var cur = CustomUrlRequest("http://example.com", "get")
        assertThat("Http Method should always be capitalized", cur.HttpMethod, `is`("GET"))

        cur = CustomUrlRequest("http://example.com", "GET")
        assertThat("Http Method should always be capitalized", cur.HttpMethod, `is`("GET"))

        cur = CustomUrlRequest("http://example.com", "pUt")
        assertThat("Http Method should always be capitalized", cur.HttpMethod, `is`("PUT"))

    }

    @Test
    fun getHttpMethod_CustomMethodNames_AlwaysCapitalized() {
        val cur = CustomUrlRequest("http://example.com", "blaH")
        assertThat("Custom HTTP Methods should also be capitalized", cur.HttpMethod, `is`("BLAH"))
    }

    @Test
    fun removeCredentialsFromUrl_CredentialsPresent_RemovedFromUrl(){
        val cur = CustomUrlRequest("http://bob:hunter2@example.com/%LOG")
        assertThat("Credentials removed from URL", cur.LogURL, `is`("http://example.com/%LOG"))
    }


    @Test
    fun addBasicAuthorizationHeader_CredentialsPresent(){
        val cur = CustomUrlRequest("http://bob:hunter2@example.com/%LOG")
        assertThat("Authorization header is present", cur.HttpHeaders.get("Authorization"), `is`("Basic Ym9iOmh1bnRlcjI="))
    }
    @Test
    fun addBasicAuthorization_CredentialsNotPresent(){
        val cur = CustomUrlRequest("http://example.com/%SER")
        assertThat("No authorization header is present", cur.HttpHeaders.get("Authorization"), IsNull())
    }


    @Test
    fun getHeadersFromTextBlock_SingleHeader(){
        val headers = "X-Custom: 17"
        val expectedMap = hashMapOf<String,String>(Pair("X-Custom","17"))
        val cur = CustomUrlRequest(LogURL="http://example.com", RawHeaders = headers)

        assertThat("Single line header is parsed", cur.HttpHeaders, `is`(expectedMap))
    }

    @Test
    fun getHeadersFromTextBlock_MultipleHeaders() {
        val headers = "Content-Type: application/json\nAuthorization: Basic 123984234=\nApiToken: 12346"
        val expectedMap = hashMapOf(Pair("Content-Type","application/json"),Pair("Authorization", "Basic 123984234="),Pair("ApiToken", "12346"));

        val cur = CustomUrlRequest(LogURL="http://example.com", RawHeaders = headers)

        assertThat("Headers map created from text block", cur.HttpHeaders, `is`(expectedMap))
    }


    @Test
    fun getHeadersFromTextBlock_NoHeaders(){
        var headers=""
        val expectedMap = emptyMap<String,String>()
        var cur = CustomUrlRequest(LogURL = "http://example.com", RawHeaders = headers)
        assertThat("Dealing with empty headers line", cur.HttpHeaders, `is`(expectedMap))

        headers = "\n    \n :"
        cur = CustomUrlRequest(LogURL = "http://example.com", RawHeaders = headers)
        assertThat("Whitespace results in blank headers", cur.HttpHeaders, `is`(expectedMap))
    }

    @Test
    fun getHeadersFromTextBlock_SpuriousInput(){
        val headers = "blah blah \n ploopity ploopity"
        val expectedMap = emptyMap<String,String>()
        val cur = CustomUrlRequest(LogURL = "http://example.com", RawHeaders = headers )

        assertThat("Headers block should be properly formatted with newlines and colons", cur.HttpHeaders, `is`(expectedMap))
    }



}