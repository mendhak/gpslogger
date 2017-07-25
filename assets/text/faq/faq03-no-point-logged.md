## My time interval has passed, but no point was logged 
## Sometimes, the app will not log for long periods of time

Sometimes your specified time interval will have passed, but no point was logged.  There are a few reasons this could happens. 

* The GPS system will have attempted to find its location and given up after a while. This in turn means that Android OS will not have given a location to GPSLogger

* The accuracy was below your *Accuracy filter* settings, or the distance was below your *Distance filter* settings, so GPSLogger didn't log it. You can try setting a *retry interval* in which GPSLogger can wait for a more accurate point to show up and then use it.  Or you can allow for slightly more inaccurate fixes - your mileage may vary as every phone is different in terms of how accurate a fix it can get on a regular basis. 

* Additionally, on Android 6+ (Marshmallow), a new feature called *[doze mode](https://android.stackexchange.com/a/129075/14996)* was introduced, which severely restricts activity on the device after certain periods of inactivity. You can choose to [whitelist GPSLogger](http://www.cnet.com/uk/how-to/stop-specific-apps-from-using-doze-on-android-6-0-marshmallow/) which does not bypass doze mode but occasionally provides logging windows in which to work. It will not make a great difference though, doze mode is quite aggressive.

