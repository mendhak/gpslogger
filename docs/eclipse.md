
## Download tools and projects

Download and extract to your preferred location:

 * [SDK Tools Only](https://developer.android.com/sdk/index.html)
 * [Eclipse Standard](http://www.eclipse.org/downloads/).
 * [Actionbar Sherlock 4.2.0 (ABS)](http://actionbarsherlock.com/download.html)
 * GPSLogger:

       git clone https://github.com/mendhak/gpslogger.git

Remmember where you have placed each.

## Configure tools

Execute Eclipse

    ./eclipse/eclipse

### Add plugins

In the menu:

 > Help > Eclipse Marketplace

Search for ```Android Developer Tools for Eclipse```

click *Install*, on the next window click *Install More*

Search for ```Android Configurator for M2E 0.4.3```

click *Install*, on the bottom click *Install Now* and complete the installation.

**Important:** restart Eclipse.

### Set SDK folder

Once Eclipse restarts, in the menu:

 > Window > Preferences > Android > SDK Location

Update *SDK Location* with the path to the directory of the SDK that you downloaded.

### SDK platforms

Again, in the menu:

    Window > Android SDK Manager

Install the following:
 * API 16
    * SDK Platform
    * Google APIs
 * Extras
    * Android Support Library 
    * Google Play services

## Import dependencies

### Actionbar Sherlock (ABS)

Create this folder:

    mkdir GPSLogger/gen-external-apklibs

Copy ABS to the GPSLogger clone, use a similar command:

    cp -R ~/Downloads/JakeWharton-ActionBarSherlock-5a15d92/library GPSLogger/gen-external-apklibs/actionbarsherlock

In the Eclipse menu:

 > File > Import > Maven > Existing Maven Projects
In *Root directory* browse for the folder that you just copied (actionbarsherlock) and import it.

Right click on the project and go to: 

 > Properties > Android
 1. Verify that the *Project Build Target* matches the target declared in the manifest. For version 4.2.0 is API 16.
 2. Verify that *Is Library* is checked and do not close the window.

In the same window go to:

 > Java Build Path > Order and Export 
 1. Select the entry: Android 4.0

### Google Play services

We have already installed the library in a previous step, in [SDK platforms](eclipse.md#sdk-platforms).

Make a copy of the library project and put it in the root folder. Assuming that you are in the root folder of the project, the command would be similar to:

    cp your-own-path-to-sdk/sdk/extras/google/google_play_services/libproject/google-play-services_lib/ .

In the Eclipse menu:

 > File > Import > Android > Existing Android Code Into Workspace
In *Root directory* browse for the folder that you just copied (google-play-services_lib) and import it.

Right click on the project and go to: 

 > Properties > Android
 1. Verify that the *Project Build Target* matches: API level 16
 2. Verify that *Is Library* is checked.

## Import GPSLogger

In the Eclipse menu:

 > File > Import > Maven > Existing Maven Projects
In *Root directory* browse for the GPSLogger clone (gpslogger) and import it.

In the left side panel, Package Explorer, you will see four nodes:
 1. actionbarsherlock
 2. google-play-services_lib
 3. gpslogger
 4. gpslogger-parent

Right click on number three, gpslogger, and go to

 > Properties > Android

 1. In *Project Build Target* verify that the selected API is level 16.
 2. In the *Library* section use the *Add* button to add:
     1. actionbarsherlock
     2. google-play-services_lib

In the same window go to:

 > Java Build Path > Order and Export 

 1. Un-select the entry *Maven Dependencies*

