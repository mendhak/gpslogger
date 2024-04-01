## How does this integrate with Tasker/Llama or other automation frameworks?

## How to automate GPSLogger?

### Controlling GPSLogger

If your automation app can send intents, you can use those intents to control GPSLogger and get it to perform a few actions. 

To invoke it from Tasker, create a new action under Misc > Send Intent. 

>Action: `com.mendhak.gpslogger.TASKER_COMMAND`  
Extra: `immediatestart:true` (others below)  
Package: `com.mendhak.gpslogger`  
Class: `com.mendhak.gpslogger.TaskerReceiver`  
Target: `Broadcast Receiver`

To invoke it from Automate (LlamaLab), create a Send Broadcast block:

>Package: `com.mendhak.gpslogger`  
Receiver Class: `com.mendhak.gpslogger.TaskerReceiver`  
Action: `com.mendhak.gpslogger.TASKER_COMMAND`  
Extras: `{"immediatestart" as Boolean:"true"}`

To invoke it from your own Android code:

    Intent i = new Intent("com.mendhak.gpslogger.TASKER_COMMAND");
    i.setPackage("com.mendhak.gpslogger");
    i.putExtra("immediatestart", true);
    sendBroadcast(i);


**These are the extras you can send to GPSLogger**:

> `immediatestart` - (true) Start logging    
> `immediatestop` - (true) Stop logging  
> `immediateautosend` - (true) Initiate auto-send file uploads (only works if logging has started)   
> `setnextpointdescription` - (text) Sets the annotation text to use for the next point logged  
> `settimebeforelogging` - (number) Sets preference for logging interval option    
> `setdistancebeforelogging` - (number) Sets preference for distance before logging option  
> `setkeepbetweenfix` - (true/false) Sets preference whether to keep GPS on between fixes  
> `setretrytime` - (number) Sets preference for duration to match accuracy  
> `setabsolutetimeout` - (number) Sets preference for absolute timeout  
> `setprefercelltower` - (true/false) Enables or disables the GPS or celltower listeners  
> `logonce` - (true) Log a single point, then stop  
> `switchprofile` - (text) The name of the profile to switch to  
> `getstatus` - (true) Asks GPSLogger to send its current events broadcast  

### Shortcuts

The app comes with a Start and a Stop **shortcut** (long press home screen, add widget), you can invoke those from some automation apps.


### GPSLogger Events Broadcast

### Listening to GPSLogger

GPSLogger sends a broadcast start/stop of logging, or file uploaded, which you can receive as an event.  

In Tasker, this would be the `Intent Received` event.  
Set the action to `com.mendhak.gpslogger.EVENT`.  
You can then access the extras as `%variablename` or `%arrayname1`.  

In Automate, you can use the Broadcast Receive block.  
Set the Action to `com.mendhak.gpslogger.EVENT`.  
Set the dictionary with broadcast extras to a variable, then access the extras as `myvar["variablename"]` or `myvar["arrayname"][0]`.  

From there in your task, you can look at the following variables.

*Start/Stop logging*

* `gpsloggerevent` - `started` or `stopped`
* `filename` - the base filename that was chosen (no extension)
* `startedtimestamp` - timestamp when logging was started (epoch)
* `duration` - seconds since the current session started
* `distance` - meters travelled since the session started


*File uploaded*

* `gpsloggerevent` - `fileuploaded`
* `filepaths` - an array of file paths that were uploaded, even if it's just a single file
* `sendertype` - which sender was used to upload the file, e.g. `customurl`, `ftp`, etc.


In a custom application, receive the `com.mendhak.gpslogger.EVENT` broadcast and have a look inside the extras.