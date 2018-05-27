GPSLogger [![Build Status](https://travis-ci.org/mendhak/gpslogger.svg?branch=master)](https://travis-ci.org/mendhak/gpslogger) [![Join the chat at https://gitter.im/gpslogger/Lobby](https://badges.gitter.im/gpslogger/Lobby.svg)](https://gitter.im/gpslogger/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![pgp](assets/pgp.png)](https://keybase.io/mendhak)
=========


GPSLogger is an Android app that logs GPS information to various formats (GPX, KML, CSV, NMEA, Custom URL) and has options for uploading (SFTP, Google Drive, Dropbox, Email). This app aims to be as battery efficient as possible.

[Read about GPSLogger's features here](http://mendhak.github.com/gpslogger/)

## Download

You can [download it from Google Play](https://play.google.com/store/apps/details?id=com.mendhak.gpslogger).

You can download directly [from the releases](https://github.com/mendhak/gpslogger/releases).

You can find it on [IzzySoft's F-Droid repo](https://apt.izzysoft.de/fdroid/repo) ([link](https://apt.izzysoft.de/fdroid/index/apk/com.mendhak.gpslogger)) 



## Contribute

You can help with [translations](http://crowdin.net/project/gpslogger-for-android) on Crowdin.

You can also submit [pull requests](https://help.github.com/articles/using-pull-requests) for bug fixes and new features.

I'm not very good at UIs, so any work with the layouts would be appreciated!  


## License and policy

[Licensed under GPL v2](LICENSE.md) | [Third party licenses](assets/text/opensource.md) | [Privacy policy](assets/text/privacypolicy.md)

## Donate

[Bitcoin](https://blockchain.info/payment_request?address=14bKk4sR1AD7avuJfBx2izy2FwyqMXEvcY) | [Paypal](https://paypal.me/mendhak) | [LTC](http://ltc.blockr.io/address/info/LP6gPtk1rkXyKYazyUJAkJpyc4Ghp8qxGs)


## Verifying

It's good practice to verify downloads.  In recent releases, a PGP signature and an SHA256 checksum will accompany each `.apk`.

Import PGP Public Key from [Keybase.io](https://keybase.io/mendhak) or just `gpg --recv-key 6989CF77490369CFFDCBCD8995E7D75C76CBE9A9`

To verify the integrity and signature:

    $ gpg --verify ~/Downloads/gpslogger-71.apk.asc
    
To verify checksum:    
    
    $ sha256sum -c ~/Downloads/gpslogger-71.apk.SHA256


Setting up the code
=========


The project is based on the [Android build system](http://tools.android.com/tech-docs/new-build-system/user-guide) plugin for Gradle.
Feel free to adopt and document your own OS and IDEs.  These instructions are for Ubuntu Linux with IntelliJ IDEA.

### Set up your Android Development Environment

Follow the instructions on the [Android Developer Website](http://developer.android.com/sdk/installing/index.html) to set up your computer for development.



![intellij](assets/logo_IntelliJIDEA.png)

Download and install [IntelliJ IDEA Community Edition](http://www.jetbrains.com/idea/download/index.html), which is free.


### Clone the GPSLogger repository

    git clone git://github.com/mendhak/gpslogger.git

### Get the Android SDK extra repositories

This project uses certain Android libraries, you can install them using Google's poorly implemented [`sdkmanager`](https://developer.android.com/studio/command-line/sdkmanager.html):

      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'tools'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'platform-tools'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'build-tools;26.0.2'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'platforms;android-27'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'platforms;android-25'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'extras;google;m2repository'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'extras;android;m2repository'
      echo y | $HOME/android-sdk/tools/bin/sdkmanager 'extras;google;google_play_services'


### Create local.properties

Create a file called `local.properties`, pointing at your Android SDK directory.

    cd gpslogger
    echo "sdk.dir=/home/mendhak/Programs/Android" > local.properties

### Import the project

Open up IntelliJ and choose to import a project.  Select the topmost `build.gradle` file under GPSLogger.

If you get an Import dialog, choose to *Import project from external model*

![import](assets/import_1.png)

On the next screen, choose the defaults and proceed (default gradle wrapper)

![import](assets/import_2.jpg)

Give it a minute and IntelliJ/Gradle will configure the projects and download the various libraries.

IntelliJ may not know where your Android SDK is.  You can find this under *File > Project Structure...* where you should set the Project SDK.  You will want to use Java 1.6 with Android 4 or above.

### OpenStreetMap Setup (Optional)

Sign up for an account with [OpenStreetMap](https://openstreetmap.org) and log in.

Click on 'oauth settings'

Click on 'Register your application'

Fill in the form with these details

![Oauth settings](assets/osm_oauth_settings.png)

After registering the application, you will receive a 'Consumer Key' and a 'Consumer Secret'.  
Place the keys in your `~/.gradle/gradle.properties` like this:

    GPSLOGGER_OSM_CONSUMERKEY=abcdefgh
    GPSLOGGER_OSM_CONSUMERSECRET=1234123456


### Dropbox Setup (Optional)

Sign up for an account with Dropbox.com

Go to the [Dropbox Developers page](https://www.dropbox.com/developers/apps) and click on 'Create an App'

Use these settings, but choose a unique name

![Dropbox settings](assets/dropbox_settings_create.png)

After creating the app, you will receive an app key and secret (the ones in the screenshot are fake)

![Dropbox settings](assets/dropbox_settings.png)

Place the keys in your `~/.gradle/gradle.properties` like this:


    GPSLOGGER_DROPBOX_APPKEY=abcdefgh
    GPSLOGGER_DROPBOX_APPSECRET=1234123456


Replace the Dropbox app key to your AndroidManifest.xml file

    <!-- Change this to be db- followed by your app key -->
    <data android:scheme="db-12341234"/>

### Google Docs/Drive Setup (Optional)

Go to the [Google APIs Console](https://code.google.com/apis/console/) and create a new project.

After registering a project, click on API Access and click the 'Create another Client ID' button

Choose "Installed Application" and then under Installed Application Type, choose "Android".  Follow the instructions under
[Learn More](https://developers.google.com/console/help/#installed_applications) to specify the package name and
the SHA1 fingerprint of your debug certificate.

![GAPI Console](assets/gapi_console.jpg)

The Google Docs feature requires the [Google Play Services Framework](http://developer.android.com/google/play-services/index.html),
so ensure that the emulator you are using is Android 4.2.2 (API level 17) or greater if you want to use this feature.

![AVD](assets/avd.png)

You can also debug directly against your phone - all phones Android 2.2 and above should have this framework installed.


### Android Wear emulator

You can use the Android AVD to create a Wear device. Once that's up and running it should appear in the list of `adb devices`

Connect phone to computer by USB cable.

Install the Android Wear application from the Play Store, pair a watch and choose to connect to an emulator.

Forward the TCP port that the phone's looking for

    adb -d forward tcp:5601 tcp:5601

Then deploy `gpsloggerwear` straight to the emulator and `gpslogger-gpslogger` to the phone.


Overview
======

GPSLogger is composed of a few main components;

![design](assets/gpslogger_architecture.png)

### Event Bus

The Event Bus is where all the cross communication happens.  Various components raise their events on the Event Bus,
and other parts of the application listen for those events.  The most important one is when a location is obtained,
 it is placed on the event bus and consumed by many fragments.

### GPS Logging Service

GPSLoggingService is where all the work happens.  This service talks to the location providers (network and satellite).
It sets up timers and alarms for the next GPS point to be requested.  It passes location info to the various loggers
so that they can write files.  It also invokes the auto-uploaders so that they may send their files to Dropbox, etc.

It also passes information to the Event Bus.

### GPS Main Activity

This is the main visible form in the app.   It consists of several 'fragments' - the simple view, detailed view and big view.

It takes care of the main screen, the menus and toolbars.

The fragments listen to the Event Bus for location changes and display it in their own way.

### Session and AppSettings

Floating about are two other objects.  `Session` contains various pieces of information related to the current GPSLogger run,
such as current file name, the last known location, satellite count, and any other information which isn't static but is
needed for the current run of GPSLogger.

`AppSettings` is a representation of the user's preferences.

These objects are visible throughout the application and can be accessed directly by any class, service, activity or fragment.
