## Why does GPS drift when I am stationary?
## Why does my GPS speed sometimes show massive spikes?


This is normal with GPS receivers, especially in areas where there are objects nearby (trees, people, buildings, mountains).  GPS receivers receive their signals over multiple paths.  The main path of course is the satellites themselves, but it also receives signals due to reflection off those nearby objects.  The receiver now sees more signal sources than are actually present and based on various algorithms, chooses to believe one of them.  Due to the reflected signals, the distance and clock offset from the satellite also differs and the calculation based on this is now slightly incorrect.  Considering that there may be several satellite signals being reflected, the overall outcome is the GPS drift that you see. 

To deal with this you can use the distance filter in the performance settings.    

For more details on multipath, see [this page](https://www.e-education.psu.edu/geog862/node/1721).