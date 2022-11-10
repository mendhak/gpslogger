# Why does GPS take a long time to find a fix?

The gist of a phone using GPS is that it needs to know where every satellite in the GPS constellation is and it usually wants to use your data connection to do this. The GPS satellites do broadcast this information at different intervals; specifically, each satellite broadcasts almanac data and ephemeris data.

The almanac is not very precise and gives a rough overview of the constellation and their positions over the next few months. A GPS receiver can use this to get a rough idea of where it is and which satellites to select. When a phone has been off for a long time, or suddenly shifted to a new location, it needs to reacquire almanac data.

The ephemeris data is more precise; it is broadcast more frequently and goes stale quite quickly. It's only once a GPS receiver has its almanac data that it knows to look for the ephemeris data. Once the phone has both of these pieces of information, it can then figure out where it is. This process can take around 12 minutes and is known to be very flaky; any interference or interruption in the process means that the GPS receiver in the phone needs to start over. The satellites only broadcast at 50 bytes/second.

Because it takes so long, to assist with this, many mobile operators deploy aGPS servers; these servers have already downloaded the almanac and ephemeris data and your phone can download it from them at a faster rate then from the satellites. But it does mean that your phone has to be on a familiar network. Being on roaming or with a restricted data plan will prevent this from happening.

All in all, there are a lot of factors at play. The problem could be anything from missing almanacs to data to hardware. GPSLogger simply waits for the OS to be ready with its information.