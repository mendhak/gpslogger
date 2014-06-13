GPSLogger [![Build Status](https://travis-ci.org/mendhak/gpslogger.svg?branch=master)](https://travis-ci.org/mendhak/gpslogger)
=========

GPSLogger is an Android app that logs GPS information to GPX, KML or text files and has options for annotating and sharing.

[Read about GPSLogger here](http://mendhak.github.com/gpslogger/)

## Download

You can [download it from Google Play](https://play.google.com/store/apps/details?id=com.mendhak.gpslogger).

You can download the APK directly [here](https://sourceforge.net/projects/gfadownload/files/)

## Contribute

You can help with [translations](http://crowdin.net/project/gpslogger-for-android)

You can also submit [pull requests](https://help.github.com/articles/using-pull-requests) for bug fixes and new features.


## License

Licensed under [GPL v2](http://www.gnu.org/licenses/gpl-2.0.html).





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




### Running tests

This solution has a few [Robotium](https://code.google.com/p/robotium/) tests.  To run them, first ensure that you
have an emulator up and running or your phone is connected.  In other words, `adb devices` should show a connected device.

Then run the tests using the gradle wrapper

     ./gradlew connectedAndroidTest --info

If a test fails and you want a little more info, you can add the `stacktrace` and `debug` flags

    ./gradlew connectedAndroidTest --debug --stacktrace

You can also try running the tests straight from the IDE.  Right click on a test class such as `GpsMainActivityTests`
or on the `src/test/java` folder and choose to run it with the Android test instrumentation runner.

![Android tests](https://farm8.staticflickr.com/7248/13943655031_7ee4e7e92f_z.jpg)

And you should get results in the Run tab.

![tests](https://farm8.staticflickr.com/7424/13796700395_021e03cd8e_o.png)

You can run just the quicker `@SmallTest`s using

    ./gradlew connectedAndroidTest -PtestSize=small --info

Overview
======

GPSLogger is composed of a few main components;

![test](https://drive.google.com/uc?export=view&id=0B6IOK82n4BkAankxcFJmYk90Y0U)


### GPS Logging Service

GPSLoggingService is where all the work happens.  This service talks to the location providers (network and satellite).
It sets up timers and alarms for the next GPS point to be requested.  It passes location info to the various loggers
so that they can write files.  It also invokes the auto-uploaders so that they may send their files to Dropbox, etc.

It also passes information to the GPSMainActivity.

### GPS Main Activity

This is the main visible form in the app.   It consists of several 'fragments' - the simple view, detailed view and big view.

It takes care of the main screen, the menus and passing information from the GPSLoggingService to the various fragments.

It also passes requests from the fragments to start or stop logging.

### Session and AppSettings

Floating about are two other objects.  `Session` contains various pieces of information related to the current GPSLogger run,
such as current file name, the last known location, satellite count, and any other information which isn't static but is
needed for the current run of GPSLogger.

`AppSettings` is a representation of the user's preferences.

These objects are visible throughout the application and can be accessed directly by any class, service, activity or fragment.