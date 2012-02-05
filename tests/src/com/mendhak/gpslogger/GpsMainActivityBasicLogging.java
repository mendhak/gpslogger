package com.mendhak.gpslogger;

import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.Smoke;
import com.jayway.android.robotium.solo.Solo;

public class GpsMainActivityBasicLogging extends ActivityInstrumentationTestCase2<GpsMainActivity>
{
    private Solo solo;

    public GpsMainActivityBasicLogging()
    {
        super("com.mendhak.gpslogger", GpsMainActivity.class);
    }

    @Override
    public void setUp() throws Exception
    {
        solo = new Solo(getInstrumentation(), getActivity());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(solo.getCurrentActivity());
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();

        if(solo.searchButton("Wait"))
        {
            solo.clickOnButton("Wait");
        }

    }

    @Override
    public void tearDown() throws Exception
    {
        //This is called after each test

        //Closes the current activity

        try
        {

            getActivity().finish();
            solo.finishOpenedActivities();

        }
        catch (Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }


    @Smoke
    @FlakyTest
    public void testGPS_BasicLocation_DisplayedOnScreen()
    {

        Common.LogInfo("testGPS_BasicLocation_DisplayedOnScreen");

        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Logging details");
        solo.clickOnText("Time before logging");
        solo.enterText(0, "1");
        solo.clickOnText("OK");
        solo.goBack();
        solo.goBack();


        Common.LogInfo("Getting a random location");
        Location randomLoc = Common.GetRandomLocation();

        //Start logging
        solo.clickOnButton(0);

        Common.LogInfo("Set device location");
        Common.SetDeviceGpsLocation(11.11, 14.11, getInstrumentation().getContext());

        solo.sleep(5000);
        Common.SetDeviceGpsLocation(22.22, 34.11, getInstrumentation().getContext());

        solo.sleep(5000);
        Common.SetDeviceGpsLocation(randomLoc.getLatitude(), randomLoc.getLongitude(), getInstrumentation().getContext());


        // Check if the location was displayed
        assertTrue(solo.searchText(String.valueOf(randomLoc.getLatitude())));
        assertTrue(solo.searchText(String.valueOf(randomLoc.getLongitude())));

        //Stop logging
        solo.clickOnButton(0);


    }


    @Smoke
    public void testSinglePoint_StopsAfterPoint()
    {

        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Logging details");
        solo.clickOnText("Time before logging");
        solo.enterText(0, "1");
        solo.clickOnText("OK");
        solo.goBack();
        solo.goBack();


        Common.LogInfo("Getting a random location");
        Location randomLoc = Common.GetRandomLocation();

        //Start logging
        solo.clickOnButton("Log a single point");

        assertFalse(solo.getButton(0).isEnabled());

        Common.LogInfo("Set device location");

        solo.sleep(1000);

        Common.SetDeviceGpsLocation(randomLoc.getLatitude(), randomLoc.getLongitude(), getInstrumentation().getContext());

        solo.sleep(2000);

        assertTrue(solo.getButton("Log a single point").isEnabled());

    }

    @Smoke
    public void testManagers_PreferCellTower_UsesCellTowerCoordinates()
    {
        Common.LogInfo("testGPS_BasicLocation_DisplayedOnScreen");

        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Logging details");
        solo.clickOnText("Prefer celltowers");
        solo.clickOnText("Time before logging");
        solo.enterText(0, "1");
        solo.clickOnText("OK");
        solo.goBack();
        solo.goBack();


        Common.LogInfo("Getting a random location");
        Location randomLoc = Common.GetRandomLocation();
        Location towerRandomLoc = Common.GetRandomLocation();

        //Start logging
        solo.clickOnButton(0);

        if(solo.searchText("Sorry"))
        {
            solo.clickOnButton(0);
            fail("Network provider needs to be enabled");
        }
        else
        {
            Common.LogInfo("Set device location");
            Common.SetDeviceGpsLocation(11.11, 14.11, getInstrumentation().getContext());
            Common.SetDeviceTowerLocation(22.22, 33.33, getInstrumentation().getContext());

            solo.sleep(5000);
            Common.SetDeviceGpsLocation(22.22, 34.11, getInstrumentation().getContext());
            Common.SetDeviceTowerLocation(33.33, 44.44, getInstrumentation().getContext());

            solo.sleep(5000);
            Common.SetDeviceGpsLocation(randomLoc.getLatitude(), randomLoc.getLongitude(), getInstrumentation().getContext());
            Common.SetDeviceTowerLocation(towerRandomLoc.getLatitude(), towerRandomLoc.getLongitude(), getInstrumentation().getContext());


            // Check if the location was displayed
            assertTrue(solo.searchText(String.valueOf(towerRandomLoc.getLatitude())));
            assertTrue(solo.searchText(String.valueOf(towerRandomLoc.getLongitude())));

            //Stop logging
            solo.clickOnButton(0);
        }


    }


    @Smoke
    public void testContinuity_StartLogging_ResumeActivityStillLogging_1()
    {
        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Logging details");
        solo.clickOnText("Time before logging");
        solo.enterText(0,"1");
        solo.clickOnText("OK");
        solo.goBack();
        solo.goBack();



        //Start logging
        solo.clickOnButton(0);
        //Wait a bit
        solo.sleep(1000);

        //Set device location
        Common.SetDeviceGpsLocation(88.8, 77.7, getInstrumentation().getContext());

        //Wait a bit
        solo.sleep(3000);

        // Check if the location was displayed
        assertTrue(solo.searchText(String.valueOf(88.8)));

        //Now the tearDown gets called
    }


    @Smoke
    public void testContinuity_StartLogging_ResumeActivityStillLogging_2()
    {
        // Check if the location was displayed
        solo.clickOnButton(0);
        assertTrue(solo.searchText(String.valueOf(88.8)));

    }


}
