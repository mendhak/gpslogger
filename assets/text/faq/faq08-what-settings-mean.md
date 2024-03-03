## What do the various settings mean? (Accuracy, retry interval for accuracy, etc)

![7b](images/7b.png)

**Log GPS/GNSS locations** - Logs points from the satellite location listener. 

**Log network locations** - Logs points from the cell tower location listener. 

**Log passive locations** - Logs points from other apps, is subject to some restrictions. It will log GPS/network points if those were selected above. It may bypass other filters such as time, distance, and retry duration.

**Logging interval** - How long to wait after a point has been logged to try logging again.

**Distance filter** - When a point becomes available, the app will check to ensure that this much distance exists between the previous and current points. If it isn't this distance, the point is discarded.

**Accuracy filter** - When a point becomes available, the app will check to ensure that this point has a minimum accuracy specified. If it does not match the specified accuracy, the point is discarded. This is useful if you are inside a building for a while.

**Duration to match accuracy** - When searching for a point, the app can continue searching for this many seconds until it finds a point that meets the accuracy and distance filter criteria above.

**Choose best accuracy in duration** - After matching a point with the desired accuracy, the app will continue searching for this many seconds to find a point with even better accuracy.  This is useful if you are in a location where GPS accuracy is poor, and don't need the location point immediately.

**Absolute timeout** - When searching for a point and trying over and over, the app will give up when this timeout is reached.  This is useful for when you're inside buildings, GPS tends to keep searching and finding nothing.  

**Keep GPS on between fixes** - Normally, the app stops using GPS between points, to save battery.  This means when it's time to log the next point, the GPS needs to be 'woken up' again and this takes a little time.  Keeping GPS on between fixes causes this 'wake up' time to be reduced.

