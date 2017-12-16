package com.mendhak.gpslogger.loggers.customurl

import android.util.Log
import java.util.regex.Pattern


data class CustomUrlRequest @JvmOverloads constructor (var LogURL : String,
                                                       var HttpMethod: String = "GET",
                                                       var HttpBody : String = "") : java.io.Serializable  {



    init {
        HttpMethod = HttpMethod.toUpperCase()
    }

    var BasicAuthUsername : String = ""
    var BasicAuthPassword : String = ""


    init {

        val (usr,pwd) = getBasicAuthCredentialsFromUrl(LogURL)
        BasicAuthUsername = usr
        BasicAuthPassword = pwd

        removeCredentialsFromUrl(usr,pwd)
    }

    private fun removeCredentialsFromUrl(basicAuthUsername: String, basicAuthPassword: String) {
        LogURL = LogURL.replace("$basicAuthUsername:$basicAuthPassword@","")
    }


    fun getBasicAuthCredentialsFromUrl(logUrl: String): Pair<String, String> {

        var result  = Pair<String,String>("","")

        val r = Pattern.compile("(\\w+):(\\w+)@.+")
        val m = r.matcher(logUrl)
        while(m.find()){
            result = Pair<String,String>(m.group(1), m.group(2))
        }

        return result;
    }




}