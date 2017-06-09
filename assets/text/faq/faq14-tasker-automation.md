## How does this integrate with Tasker/Llama or other automation frameworks?

### Controlling GPSLogger

If your automation app can send intents, you can use those intents to control GPSLogger and get it to perform a few actions. 
For example, in Tasker, create a new action under Misc > Send Intent. 

>Action: `com.mendhak.gpslogger.GpsLoggingService`  
Extra: `immediatestart:true (others below)`  
Target: `Service`


These are the extras you can send to GPSLogger:

>`immediatestart` - (true/false) Start logging immediately  

> `immediatestop` - (true/false) Stop logging

> `setnextpointdescription` - (text) Sets the annotation text to use for the next point logged

> `settimebeforelogging` - (number) Sets preference for logging interval option  

> `setdistancebeforelogging` - (number) Sets preference for distance before logging option

> `setkeepbetweenfix` - (true/false) Sets preference whether to keep GPS on between fixes

> `setretrytime` - (number) Sets preference for duration to match accuracy

> `setabsolutetimeout` - (number) Sets preference for absolute timeout
  
> `setprefercelltower` - (true/false) Enables or disables the GPS or celltower listeners

> `logonce` - (true/false) Log a single point, then stop

> `switchprofile` - (text) The name of the profile to switch to

You can also invoke the Start and Stop **shortcuts** that GPSLogger comes with.

### GPSLogger Events Broadcast


GPSLogger sends a broadcast start/stop of logging, which you can receive as an event.
  
In Tasker, this would look like:  
  
> Event: Intent Received  
  Action: com.mendhak.gpslogger.EVENT 
  
From there in your task, you can look at the following variables
 
 * `%gpsloggerevent` - `started` or `stopped`
 * `%filename` - the base filename that was chosen (no extension)
 * `%startedtimestamp` - timestamp when logging was started (epoch)

In a custom application, receive the `com.mendhak.gpslogger.EVENT` broadcast and have a look inside the extras.