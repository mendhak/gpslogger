# IntelliJ setup
# Create a separate run configuration (of type Android Tests) for each .java file in this tests module
# For example, I created a 'TestBasicLogging' run configuration for GpsMainActivityBasicLogging.java
# Similarly, I created a 'TestAnnotation' run configuration for GpsMainActivityAnnotation.java


# The tests seem unstable, sometimes they'll run fine,
# and sometimes you'll get the 'Sorry! Application GPSLogger (...) is not responding' dialog
# I have no idea why this happens and whether it's due to Robotium or not

# To run the tests in ANT
# You can run the tests using ANT build.
# Open build.xml, then open the 'Ant Build' window in IntelliJ
# Run the 'RobotiumTests' target
# This launches the emulator, installs the apps, runs each test in testNames.txt
# It does not build the code though, so make sure you build before running it.
#


# To run the tests from commandline
# Have just one emulator running
# Ensure that these have been deployed to the emulator (usually by running at least once)
# No other android devices should be connected to the computer
./adb shell am instrument -w  -e class com.mendhak.gpslogger.GpsMainActivityAnnotation com.mendhak.gpslogger.tests/android.test.InstrumentationTestRunner
./adb shell am instrument -w  -e class com.mendhak.gpslogger.GpsMainActivityBasicLogging  com.mendhak.gpslogger.tests/android.test.InstrumentationTestRunner
./adb shell am instrument -w  -e class com.mendhak.gpslogger.GpsMainActivityTest  com.mendhak.gpslogger.tests/android.test.InstrumentationTestRunner


