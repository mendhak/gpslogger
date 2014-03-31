GPSLogger
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


Feel free to adopt and document your own OS and IDEs.  These instructions are for Ubuntu 13.10 with IntelliJ 13

### Set up your Android Development Environment

Follow the instructions on the [Android Developer Website](http://developer.android.com/sdk/installing/index.html) to set up your computer for development.

### Get IntelliJ IDEA

Download and install [IntelliJ IDEA Community Edition](http://www.jetbrains.com/idea/download/index.html), which is free.  I am choosing to use this instead of Eclipse.

### Install Git

    sudo apt-get install git

### Clone the repository

    git clone git://github.com/mendhak/gpslogger.git

### Create local.properties

IntelliJ/Android Studio [may not detect](http://stackoverflow.com/questions/19794200/gradle-android-and-the-android-home-sdk-location) your `ANDROID_HOME` environment variable, so create a file called `local.properties`, pointing at your Android SDK directory.

    cd gpslogger
    echo "sdk.dir=/home/mendhak/Programs/Android" > local.properties

### Import the project

Open up IntelliJ and choose to import a project.  Under the Import dialog, choose to *Import project from external model*

![import](https://farm3.staticflickr.com/2808/13543335914_3b709dca56_o.png)

On the next screen, choose the defaults and proceed

![import](https://farm3.staticflickr.com/2861/13543635053_042a02c11d_b.jpg)

Give it a minute and IntelliJ may ask you to choose your Android SDK.  You can find this under *File > Project Structure...* where you should set the Project SDK.  You will want to use Java 1.6 with Android 4 or above.


### Test.xml

Create a test.xml in the project at res/values/test.xml with an empty resources tag

    <resource />

This file can be used to store OAuth keys if you want OpenStreetMap and DropBox functionality.  This file is ignored in the .gitignore file and will not be committed.

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
