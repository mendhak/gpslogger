package com.mendhak.gpslogger.loggers.customurl

import java.util.HashMap
import java.util.regex.Pattern
import okhttp3.Credentials


data class CustomUrlRequest @JvmOverloads constructor (var LogURL : String,
                                                       var HttpMethod: String = "GET",
                                                       var HttpBody : String = "",
                                                       var RawHeaders : String = "") : java.io.Serializable  {


    var HttpHeaders : HashMap<String,String> = hashMapOf()


    init {
        HttpMethod = HttpMethod.toUpperCase()
    }

    init {
        val (usr,pwd) = getBasicAuthCredentialsFromUrl(LogURL)

        addAuthorizationHeader(usr,pwd)
        removeCredentialsFromUrl(usr,pwd)
    }

    init {
        HttpHeaders.putAll(getHeadersFromTextBlock(RawHeaders))
    }

    private fun removeCredentialsFromUrl(basicAuthUsername: String, basicAuthPassword: String) {
        LogURL = LogURL.replace("$basicAuthUsername:$basicAuthPassword@","")
    }

    private fun addAuthorizationHeader(usr: String, pwd: String) {
        if(!usr.isBlank() && !pwd.isBlank()){
            val credential = Credentials.basic(usr, pwd)
            HttpHeaders.put("Authorization", credential)
        }
    }

    private fun getBasicAuthCredentialsFromUrl(logUrl: String): Pair<String, String> {

        var result  = Pair<String,String>("","")

        val r = Pattern.compile("(\\w+):(\\w+)@.+")
        val m = r.matcher(logUrl)
        while(m.find()){
            result = Pair<String,String>(m.group(1), m.group(2))
        }

        return result;
    }

    private fun getHeadersFromTextBlock(httpHeaders : String) : HashMap<String,String>{
        val map = hashMapOf<String,String>()
        val lines = httpHeaders.split(regex="\\r?\\n".toRegex())
        for (line in lines){
            if(!line.isBlank() && line.contains(":")){
                val lineKey = line.split(":")[0].trim()
                val lineValue = line.split(":")[1].trim()
                if(!lineKey.isBlank() && !lineKey.isBlank()){
                    map.put(lineKey,lineValue)
                }
            }
        }
        return map
    }

}