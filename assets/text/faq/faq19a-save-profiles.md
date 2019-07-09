
## Save your current settings as a profile

Tap the profile header and then "Save" - this will save your current settings to a `.properties` file in your current GPSLogger directory. The file is named after your profile name.  For example, a profile named `xyz` after being saved will result in `xyz.properties`

You can copy this file or export it via the main share menu. 

Also note - the current profile's settings are automatically saved whenever you switch between profiles.


### Creating a profile manually

You can create your own `.properties` file and put your key value pairs in it. 

For example, in the file you can put `accuracy_before_logging=42` and that will reset the *Accuracy Filter* to 42 meters each time the application starts. There are many properties that can be applied and you can glean a [full list here](https://github.com/mendhak/gpslogger/blob/master/gpslogger/src/main/java/com/mendhak/gpslogger/common/PreferenceNames.java).

The most common examples of properties would be `log_gpx`, `log_kml`, `time_before_logging`, `opengts_*` for OpenGTS settings, `smtp_*` for email settings.
  