package com.mendhak.gpslogger;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;
import com.jayway.android.robotium.solo.Solo;

public class GpsMainContinuity  extends ActivityInstrumentationTestCase2<GpsMainActivity>
{

    private Solo solo;

    public GpsMainContinuity()
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
