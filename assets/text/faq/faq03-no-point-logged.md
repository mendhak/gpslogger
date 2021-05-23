## My time interval has passed, but no point was logged 
## Sometimes, the app will not log for long periods of time

Sometimes your specified time interval will have passed, but no point was logged.  There are a few reasons this could happens. 

* On Android 6+ (Marshmallow), a new feature called *[doze mode](http://lifehacker.com/how-android-doze-works-and-how-to-tweak-it-to-save-you-1785921957)* was introduced, which severely restricts activity on the device after certain periods of inactivity. Be sure to grant the app permission to run in the background by disabling battery optimization.  If you aren't sure, or if you've denied this permission you can [disable battery optimization for GPSLogger manually](https://android.stackexchange.com/a/129075/14996) which does not bypass doze mode but occasionally provides logging windows in which to work. It will not make a great difference though, doze mode is quite aggressive.

* Many vendors are also known to introduce their own _additional_ poorly written but aggressive battery optimization mechanisms.  App developers don't have a way of detecting or working around these, and unfortunately the apps receive all the blame.  You can see some partial workarounds on the [Don't Kill My App site](https://dontkillmyapp.com/?app=GPSLogger)

* The GPS system will have attempted to find its location and given up after a while. This in turn means that Android OS will not have given a location to GPSLogger

* The accuracy was below your *Accuracy filter* settings, or the distance was below your *Distance filter* settings, so GPSLogger didn't log it. You can try setting a *retry interval* in which GPSLogger can wait for a matching accurate point to show up and then use it.  Or you can allow for slightly more inaccurate fixes - your mileage may vary as every phone is different in terms of how accurate a fix it can get on a regular basis. 

* On Android 8+ (Oreo), the LocationManager has been limited: ["Location updates are provided to background apps only a few times each hour."](https://developer.android.com/about/versions/oreo/background-location-limits)

In **summary**, to try to maximize the locations you can receive, be sure to do the following:   

1. Grant all location permissions to the app.  This includes the background ("Allow all the time") permission.
2. [Disable battery optimization](https://android.stackexchange.com/a/129075/14996) for GPSLogger
3. See the [Don't Kill My App site](https://dontkillmyapp.com/?app=GPSLogger) website
3. Keep the app in the foreground before locking the screen 
