package com.mendhak.gpslogger.loggers.customurl


data class CustomUrlRequest @JvmOverloads constructor (public val LogURL : String, val HttpMethod: String = "GET" ) : java.io.Serializable  {


}