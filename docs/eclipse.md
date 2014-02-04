## Download tools and projects

Download and extract to your preferred location:

 * [SDK Tools Only](https://developer.android.com/sdk/index.html) (android-sdk_r22.3-linux.tgz)
 * [Eclipse Standard](http://www.eclipse.org/downloads/) (Eclipse Standard 4.3.1 Eclipse Standard 4.3.1)
 * [Actionbar Sherlock 4.4.0 (ABS)](http://actionbarsherlock.com/download.html)
 * GPSLogger source code:

        git clone https://github.com/mendhak/gpslogger.git

    Now you'll have the root folder `gpslogger/`

Remmember where you have placed each.

## Configure Eclipse

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

Update *SDK Location* with the path to the directory of the SDK that you [just downloaded](eclipse.md#download-tools-and-projects).

### SDK platforms

In the window:

 > Window > Android SDK Manager

Install the following:
 * API 19
    * SDK Platform
    * Google APIs
 * Extras
    * Android Support Library 
    * Google Play services

## Import dependencies

### Actionbar Sherlock (ABS)

Create this folder:

    mkdir gpslogger/gen-external-apklibs

Copy ABS to the new folder, use a command similar to:

    cp -R ~/Downloads/JakeWharton-ActionBarSherlock-5a15d92/actionbarsherlock \ 
          gpslogger/gen-external-apklibs/actionbarsherlock

In the Eclipse menu:

 > File > Import > Maven > Existing Maven Projects
 
In `Root directory` browse for the folder that you just copied `actionbarsherlock/` and import it.

Right click on the project and go to:

 > Properties > Android
 
Verify that:
 1. `Project Build Target` matches the version indicated in the manifest file, examples:
     * ABS v4.2.0 targetSdkVersion=16, use:
       * Target Name `Android 4.1.2` API Level `16`
     * ABS v4.4.0 targetSdkVersion=17, use:
       * Target Name `Android 4.2.2` API Level `17`
 2. `Is Library` is checked.

Click ok and open the properties window again.

Now go to:

 > Java Build Path > Order and Export

 1. Select the appropriate entry: `Android 4.#.#`
 
### Google Play services

We have already installed the library in a previous step, in [SDK platforms](eclipse.md#sdk-platforms).

Make a copy of the library project and put it inside the root folder of the project.

    cp YOUR_SDK_PATH/extras/google/google_play_services/libproject/google-play-services_lib/ gpslogger/

In the Eclipse menu:

 > File > Import > Android > Existing Android Code Into Workspace
 
In *Root directory* browse for the folder that you just copied (google-play-services_lib) and import it.

Right click on the project and go to: 

 > Properties > Android
 
 Verify that:
 1. `Project Build Target` matches the following: 
     * Target Name `Android 4.4.2` API Level `19`
 2. `Is Library` is checked.

## Import GPSLogger

In the Eclipse menu:

 > File > Import > Maven > Existing Maven Projects
 
In *Root directory* browse for your GPSLogger clone `gpslogger/` and import it.

In the left side panel, Package Explorer, you will see four nodes:
 1. actionbarsherlock
 2. google-play-services_lib
 3. gpslogger
 4. gpslogger-parent

Right click on number three, gpslogger, and go to

 > Properties > Android

Verify that:
 1. `Project Build Target` matches the following: 
     * Target Name `Android 4.4.2` API Level `19`
 2. The `Library` section has this two project references:
     1. actionbarsherlock
     2. google-play-services_lib

In the same window go to:

 > Java Build Path > Order and Export 

 1. Un-select the entry `Maven Dependencies`

