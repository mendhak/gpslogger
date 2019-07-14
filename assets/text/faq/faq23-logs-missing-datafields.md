## Why do the log files sometimes have missing data fields?
## Why is the altitude value missing?


A few fields such as `pdop`, `hdop`, `vdop`, `geoidheight`, `ageodfgpsdata`, `dgpsid` may not always appear in the logs produced.  This is because they are read from the NMEA listener which is different from the actual GPS/GNSS listener provided by Android OS.  Because they are not read simultaneously there can be periods where the NMEA listener reports nothing while GPS/GNSS continues as normal.  In those cases, no additional data is available. 

The altitude value may also go missing if you have MSL checked.  This feature subtracts the `geoidheight` from the reported altitude.  In cases where `geoidheight` is not available, the logic is to not report altitude at all, rather than report an incorrect value.  
