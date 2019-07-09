## Load Profiles from a URL

![profilesdemo](images/19a_from_url_2.gif)

You can load a profile with your settings from a URL.  This is also an easy way of providing your users or yourself with a preset profile. 

Create a `.properties` file with your settings in it - see [instructions](#saveyourcurrentsettingsasaprofile).  Host it at an accessible URL, self signed URLs will not work here.
  
To load the profile from the app, press the 'Default Profile', which switches to the profile menu, then choose 'From URL'. In the dialog, give the URL of a properties file. GPSLogger will attempt to download the file, switch to it as a profile and apply the properties.

