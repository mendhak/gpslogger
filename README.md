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


Feel free to adopt and document your own OS and IDEs.  These instructions are for Ubuntu 13.10 with IntelliJ 13.1.1

### Set up your Android Development Environment

Follow the instructions on the [Android Developer Website](http://developer.android.com/sdk/installing/index.html) to set up your computer for development.

On Ubuntu 64bit, you'll also need `ia32-libs`, follow [these instructions](http://stackoverflow.com/a/21956268/974369).

### Get Gradle

If you don't already have Gradle, download it to `/opt/gradle/gradle-1.x`.  Add the following to your `~/.bashrc` so that it is available to IntelliJ later on

    GRADLE_HOME=/opt/gradle/gradle-1.11/bin
    export GRADLE_HOME
    PATH=$PATH:$GRADLE_HOME
    export PATH


### Get IntelliJ IDEA

Download and install [IntelliJ IDEA Community Edition](http://www.jetbrains.com/idea/download/index.html), which is free.
Note that Gradle 1.11 does not work well with anything earlier than 13.1.

### Install Git

    sudo apt-get install git

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

### Import the project

Open up IntelliJ and choose to import a project.  Select the topmost `build.gradle` file under GPSLogger.

If you get an Import dialog, choose to *Import project from external model*

![import](https://farm3.staticflickr.com/2808/13543335914_3b709dca56_o.png)

On the next screen, choose the defaults and proceed (default gradle wrapper)

![import](https://farm3.staticflickr.com/2861/13543635053_042a02c11d_b.jpg)

Give it a minute and IntelliJ/Gradle will configure the projects and download the various libraries.

IntelliJ may not know where your Android SDK is.  You can find this under *File > Project Structure...* where you should set the Project SDK.  You will want to use Java 1.6 with Android 4 or above.


### Test.xml

Create a test.xml in the project at res/values/test.xml with an empty resources tag

    <resource />

This file can be used to store OAuth keys if you want OpenStreetMap and DropBox functionality (below).  This file is ignored in the .gitignore file and will not be committed.

### OpenStreetMap Setup (Optional)

Sign up for an account with [OpenStreetMap](http://openstreetmap.org) and log in.

Click on 'oauth settings'

Click on 'Register your application'

Fill in the form with these details

![Oauth settings](http://farm9.staticflickr.com/8147/7645348952_f2834d18e9_o.png)

After registering the application, you will receive a 'Consumer Key' and a 'Consumer Secret'.  Place the keys in your test.xml like this:

    <string name="osm_consumerkey">ABCDEF</string>
    <string name="osm_consumersecret">GHIJKLMNOP</string>


### Dropbox Setup (Optional)

Sign up for an account with Dropbox.com

Go to the [Dropbox Developers page](https://www.dropbox.com/developers/apps) and click on 'Create an App'

Use these settings, but choose a unique name

![Dropbox settings](http://farm8.staticflickr.com/7139/7645470952_5c75ac3ac2_o.png)

After creating the app, you will receive an app key and secret (the ones in the screenshot are fake)

![Dropbox settings](http://farm8.staticflickr.com/7267/7645470752_ae9a7e4ed2_o.png)

Add the Dropbox app key to your test.xml file


    <string name="dropbox_appkey">12341234</string>
    <string name="dropbox_appsecret">abcdabcdefg</string>


Replace the Dropbox app key to your AndroidManifest.xml file

    <!-- Change this to be db- followed by your app key -->
    <data android:scheme="db-12341234"/>

### Google Docs/Drive Setup (Optional)

Go to the [Google APIs Console](https://code.google.com/apis/console/) and create a new project.

After registering a project, click on API Access and click the 'Create another Client ID' button

Choose "Installed Application" and then under Installed Application Type, choose "Android".  Follow the instructions under
[Learn More](https://developers.google.com/console/help/#installed_applications) to specify the package name and
the SHA1 fingerprint of your debug certificate.

![GAPI Console](http://farm3.staticflickr.com/2866/9113223789_222f62a51a_c.jpg)

The Google Docs feature requires the [Google Play Services Framework](http://developer.android.com/google/play-services/index.html),
so ensure that the emulator you are using is Android 4.2.2 (API level 17) or greater if you want to use this feature.

![AVD](http://farm6.staticflickr.com/5322/9113255381_9fba026576_o.png)

You can also debug directly against your phone - all phones Android 2.2 and above should have this framework installed.


### Running tests

This solution has a few [Robotium](https://code.google.com/p/robotium/) tests.  To run them, first ensure that you
have an emulator up and running or your phone is connected.  In other words, `adb devices` should show a connected device.

Then run the tests using the gradle wrapper

     ./gradlew connectedAndroidTest --info

If a test fails and you want a little more info, you can add the `stacktrace` and `debug` flags

    ./gradlew connectedAndroidTest --debug --stacktrace

You can also try running the tests straight from the IDE, but at the time of writing, IntelliJ and Android Studio are
only just starting to include this functionality.  Your mileage may vary.  I have found that if GPSLogger is already
 installed on the device when running the test, at least one test fails.

 ![tests](https://farm8.staticflickr.com/7424/13796700395_021e03cd8e_o.png)

 The solution is to ensure that you uninstall the app from the emulator before running the tests.
 If you use the gradle wrapper command shown above, it installs and uninstalls for you.



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