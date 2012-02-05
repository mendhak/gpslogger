package com.mendhak.gpslogger;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;
import android.widget.TextView;
import com.jayway.android.robotium.solo.Solo;

public class GpsMainActivityAnnotation extends ActivityInstrumentationTestCase2<GpsMainActivity> {


    private Solo solo;

    public GpsMainActivityAnnotation()
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
    }

    @Override
    public void tearDown() throws Exception {
        try
        {
            getActivity().finish();
            solo.finishOpenedActivities();
        }
        catch (Throwable throwable)
        {
            throwable.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    /**
     * Starts logging, logs a point, annotates a message, tries to annotate a message and gets the 'not yet' message
     */
    @Smoke
    public void testGPS_BasicLocation_MenuAnnotation()
    {

        Common.LogInfo("testGPS_BasicLocation_Annotation");

        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Logging details");
        solo.clickOnText("Time before logging");
        solo.enterText(0, "1");
        solo.clickOnText("OK");
        solo.goBack();
        solo.clickOnText("Log to GPX");
        solo.goBack();


        Common.LogInfo("Getting a random location");
        Location randomLoc = Common.GetRandomLocation();

        //Start logging
        solo.clickOnButton(0);

        Common.SetDeviceGpsLocation(randomLoc.getLatitude(), randomLoc.getLongitude(), getInstrumentation().getContext());
        Common.LogInfo("Set device location");
        solo.sleep(5000);

        solo.clickOnMenuItem("Annotate");
        solo.enterText(0, "Annotation!");
        solo.clickOnText("OK");
        solo.clickOnMenuItem("Annotate");
        assertTrue(solo.searchText("Not Yet"));
        solo.clickOnText("OK");

        solo.clickOnButton(0);

        // Check if the location was displayed

    }




}
