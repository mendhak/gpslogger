## How can these GPS log files be used?
## How do I geotag photos with a GPS log file?

The GPS files produced by this app are generally used for processing *other* things.  

A common use case is to geotag photos.  Many cameras, especially SLRs, don't have built-in GPS.  After a day (or days) out of photography, you may have hundreds of photos that need to be geotagged so that their locations can appear properly when used elsewhere.  

I have had success with:

* [GeoSetter](http://www.geosetter.de/en/) - GUI, comprehensive options with map display
* [ExifTool](http://askubuntu.com/questions/599395/how-can-i-batch-tag-several-hundred-photos-with-separately-recorded-gps-data) - command line, lots of options
* Lightroom's map module - very basic and limited


 There are of course other uses of the produced files, these are a few I've seen over the years; it's usually a combination of a log file produced from GPSLogger with a secondary software to process the files.  
 
  - Recording your hike, paragliding, flight
  - View it in Google Earth, Google Maps
  - OpenStreetMap tracing
  - Track fleets of trucks or vehicles
  - Volunteer organisations use it as rescue reports
  - Drivers and salespeople using it as a timesheet
  - Tracking of geocaches, gravestones, repair sites, etc.
  - Tracking friends and family on holiday
  - Recording gravestone locations


### Programmatic access

There is a [project](https://github.com/export-mike/gpslogger-android-dropbox-restapi) that can expose the GPX files in Dropbox as a RESTful API, and a hosted [Heroku app](https://github.com/mendhak/gpslogger/issues/452). 


