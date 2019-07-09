
## Load Profiles by clicking a link

 ![profilesdemo](images/19a_from_link_2.gif)
 
 You can also load a profile in GPSLogger by clicking a link on a web page.  This is also an easy way to provide your users or yourself with a preset profile - all they need to do is click a link on a page, no typing or pasting.
 
 Create a `.properties` file with your settings in it - see [instructions](#saveyourcurrentsettingsasaprofile).  Host it at an accessible URL, self signed URLs will not work here.
 
 You will then need to host a web page with a hyperlink to that file in it, with this structure. 
 
     <a href="gpslogger://properties/https://www.mendhak.com/test.properties">Download this profile</a>
     
Basically `gpslogger://properties/` followed by the actual URL of your `.properties` file. 

Due to deficiencies in the Chrome browser, this needs to be a hyperlink on a webpage.  The link must be clicked in a browser (Firefox, Chrome) on their Android device.     

