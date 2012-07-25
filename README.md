**Can you help with [translations](http://crowdin.net/project/gpslogger-for-android)?**
------

GPSLogger
=========

[Read about GPSLogger here](http://mendhak.github.com/gpslogger/)

You can search for the app on the Android Market: "GPSLogger for Android" by mendhak

You can bypass the market and get the APK yourself:  [Download here](https://github.com/mendhak/gpslogger/archives/master)

Licensed under [GPL v2](http://www.gnu.org/licenses/gpl-2.0.html).


Setting up the code
=========

These instructions are specific to Ubuntu, feel free to adopt and document it for your own OS and IDEs

### Set up your Android Development Environment

Follow the instructions on the [Android Developer Website](http://developer.android.com/sdk/installing/index.html) to set up your computer for development.

### IntelliJ IDEA

Download and install [IntelliJ IDEA Community Edition](http://www.jetbrains.com/idea/download/index.html), which is free.  I am choosing to use this instead of Eclipse.

### Git

    sudo apt-get install git

### Clone the repository

    git clone git://github.com/mendhak/gpslogger.git

### Maven dependencies

The project comes with a pom.xml file which IntelliJ IDEA recognizes by default (it comes with an enabled Maven plugin). It will download and import the class libraries for you.


### Test.xml

Create a test.xml in the project at res/values/test.xml

This file will be used to store OAuth keys.  This file is ignored in the .gitignore file and will not be committed.

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

Grab the Client ID and Client Secret and add them to the test.xml file

    <string name="gdocs_clientid">123412341234</string>
    <string name="gdocs_clientsecret">123412341234</string>

