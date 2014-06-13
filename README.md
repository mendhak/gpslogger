GPSLogger for Oerhb
=========




Setting up the code
=========


The project is based on the new [Android build system](http://tools.android.com/tech-docs/new-build-system/user-guide) plugin for Gradle.
Feel free to adopt and document your own OS and IDEs.  These instructions are for Ubuntu Linux with IntelliJ 13.1.2 onwards.

### Set up your Android Development Environment

Follow the instructions on the [Android Developer Website](http://developer.android.com/sdk/installing/index.html) to set up your computer for development.

On Ubuntu 64bit, you may also need `ia32-libs`, follow [these instructions](http://stackoverflow.com/a/21956268/974369).  I did not need this for Ubuntu 14.04.


### Get IntelliJ IDEA

Download and install [IntelliJ IDEA Community Edition](http://www.jetbrains.com/idea/download/index.html), which is free.
Note that the Android build system version 0.9 does not work well with anything earlier than IntelliJ 13.1.2.


### Get the Android SDK extra repositories

This project uses certain Google libraries, you will need to add them. Run

    <AndroidSDK>/tools/android

Which brings up the Android SDK manager.  In here, choose

*  Tools > Android SDK build tools 19.0.3
*  Extras > Android Support Repository
*  Extras > Android Support Library
*  Extras > Google Play services
*  Extras > Google Repository

### Clone the GPSLogger repository

    git clone git://github.com/mendhak/gpslogger.git


### Create local.properties

IntelliJ/Android Studio [may not detect](http://stackoverflow.com/questions/19794200/gradle-android-and-the-android-home-sdk-location) your `ANDROID_HOME` environment variable, so create a file called `local.properties`, pointing at your Android SDK directory.

    cd gpslogger
    echo "sdk.dir=/home/mendhak/Programs/Android" > local.properties

### Test.xml

Create a test.xml in the project at gpslogger/src/main/res/values/test.xml with the predefined values to use:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="oerhb_ftp_server">ftp.example.com</string>
        <string name="oerhb_ftp_username">username</string>
        <string name="oerhb_ftp_password">hunter2</string>
        <string name="oerhb_customurl">http://localhost:8989/log?lat=%LAT&amp;long=%LON&amp;member=%MEMBER</string>
    </resources>

This file is ignored in the .gitignore file and will not be committed; it stores FTP details and should not be made public. 



### Import the project

Open up IntelliJ and choose to import a project.  Select the topmost `build.gradle` file under GPSLogger.

If you get an Import dialog, choose to *Import project from external model*

![import](https://farm3.staticflickr.com/2808/13543335914_3b709dca56_o.png)

On the next screen, choose the defaults and proceed (default gradle wrapper)

![import](https://farm3.staticflickr.com/2861/13543635053_042a02c11d_b.jpg)

Give it a minute and IntelliJ/Gradle will configure the projects and download the various libraries.

IntelliJ may not know where your Android SDK is.  You can find this under *File > Project Structure...* where you should set the Project SDK.  You will want to use Java 1.6 with Android 4 or above.


