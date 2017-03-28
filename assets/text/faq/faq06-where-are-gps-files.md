## Where is the file being logged? How do I get to it?

## What is the default folder path?

Due to recent restrictions, the default GPSLogger folder is different per device. You can see it on the simple/detailed screens and it may be something like 

> Example: `/storage/emulated/0/Android/data/com.mendhak.gpslogger/files/`

> Example: `/sdcard/Android/data/com.mendhak.gpslogger/files/` 

The initial part will be different for your device. 

If you have a file explorer installed, you can click on the folder paths in the simple/detailed screens.

To copy the GPSLogger files, you can connect your phone to your computer and mount the SD card, then copy straight from the above folder. You can also change the default folder in the app settings. 

Finally, note that due to the [restrictions introduced in Android KitKat](http://commonsware.com/blog/2014/04/09/storage-situation-removable-storage.html), any files in the default folder will be removed if you uninstall the app.

## How to log to an external SD card?

Under logging details, pick the 'save to folder' dialog.  Keep navigating upwards using the `..` until you see the label of your SD card.  Pick that and then navigate down into the GPSLogger data folder.  Your path may end up looking like this:

> Example: `/storage/1b04-100a/Android/data/com.mendhak.gpslogger/files`

Note that GPSLogger can only write to its application folders.  File explorers are able to write to any location but they make use of a special media hack which this app cannot rely on. 