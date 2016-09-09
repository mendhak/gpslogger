## How can I define a preset file with my own values?

Many people actually distribute GPSLogger to colleagues preinstalled on phones, with some preset values. 

If you create a file in the default folder or at `/sdcard/gpslogger.properties`, then GPSLogger will read this file each time it loads and apply those settings to the application. 

For example, in the file you can put `accuracy_before_logging=42` and that will reset the *Accuracy Filter* to 42 meters each time the application starts. There are many properties that can be applied and you can glean a [full list here](https://github.com/mendhak/gpslogger/blob/master/gpslogger/src/main/java/com/mendhak/gpslogger/common/PreferenceNames.java).

The most common examples of properties would be `log_gpx`, `log_kml`, `time_before_logging`, `opengts_*` for OpenGTS settings, `smtp_*` for email settings.