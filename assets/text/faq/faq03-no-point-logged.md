## My time interval has passed, but no point was logged 
## Sometimes, the app will not log for long periods of time

Sometimes your specified time interval will have passed, but no point was logged.  There are a few reasons this could happens. 

* On Android 6+ (Marshmallow), a new feature called *[doze mode](http://lifehacker.com/how-android-doze-works-and-how-to-tweak-it-to-save-you-1785921957)* was introduced, which severely restricts activity on the device after certain periods of inactivity. You can choose to [whitelist GPSLogger](https://android.stackexchange.com/a/129075/14996) which does not bypass doze mode but occasionally provides logging windows in which to work. It will not make a great difference though, doze mode is quite aggressive.

* Many vendors are also known to introduce their own _additional_ poorly written but aggressive battery optimization mechanisms.  App developers don't have a way of detecting or working around these, and unfortunately the apps receive all the blame.  You can see some partial workarounds on the [Don't Kill My App site](https://dontkillmyapp.com/?app=GPSLogger)

* The GPS system will have attempted to find its location and given up after a while. This in turn means that Android OS will not have given a location to GPSLogger

* The accuracy was below your *Accuracy filter* settings, or the distance was below your *Distance filter* settings, so GPSLogger didn't log it. You can try setting a *retry interval* in which GPSLogger can wait for a more accurate point to show up and then use it.  Or you can allow for slightly more inaccurate fixes - your mileage may vary as every phone is different in terms of how accurate a fix it can get on a regular basis. 

