package com.mendhak.gpslogger;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;
import android.widget.TextView;
import com.jayway.android.robotium.solo.Solo;

public class GpsMainActivityTest extends ActivityInstrumentationTestCase2<GpsMainActivity>
{

    private Solo solo;

    public GpsMainActivityTest() {
        super("com.mendhak.gpslogger", GpsMainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
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
            throwable.printStackTrace();
        }
    }

    @Smoke
    public void testSetting_SetGPX_GPXDisplayedAfterButtonClick()
    {
        solo.clickOnMenuItem("Settings");
        //Assert that GpsSettingsActivity activity is opened
        solo.assertCurrentActivity("Expected Settings activity", "GpsSettingsActivity");

        solo.clickOnText("Log to GPX");
        solo.goBack();

        solo.clickOnButton(0);
        solo.sleep(1000);
        solo.clickOnButton(0);


        TextView tv = (TextView)solo.getView(R.id.txtLoggingTo);
        assertTrue(tv.getText().toString().contains("GPX"));
    }

    @Smoke
    public void testSetting_SetKML_KMLDisplayedAfterButtonClick()
    {
        solo.clickOnMenuItem("Settings");
        //Assert that GpsSettingsActivity activity is opened
        solo.assertCurrentActivity("Expected Settings activity", "GpsSettingsActivity");

        solo.clickOnText("Log to KML");
        solo.goBack();

        solo.clickOnButton(0);
        solo.sleep(1000);
        solo.clickOnButton(0);


        TextView tv = (TextView)solo.getView(R.id.txtLoggingTo);
        assertTrue(tv.getText().toString().contains("KML"));
    }

    @Smoke
    public void testSetting_SetGPXKML_BothDisplayedAfterButtonClick()
    {
        solo.clickOnMenuItem("Settings");
        //Assert that GpsSettingsActivity activity is opened
        solo.assertCurrentActivity("Expected Settings activity", "GpsSettingsActivity");

        solo.clickOnText("Log to KML");
        solo.clickOnText("Log to GPX");
        solo.goBack();

        solo.clickOnButton(0);
        solo.sleep(1000);
        solo.clickOnButton(0);


        TextView tv = (TextView)solo.getView(R.id.txtLoggingTo);
        assertTrue(tv.getText().toString().contains("Both"));

    }



    
    @Smoke
    public void testOSMMenu_Uninitiated_OSMActivity()
    {
        solo.clickOnMenuItem("Upload");
        solo.clickOnText("OpenStreetMap");
        solo.assertCurrentActivity("Expected OSMAuthorizationActivity","OSMAuthorizationActivity");
    }

    @Smoke
    public void testDropboxMenu_Uninitiated_DropboxActivity()
    {
        solo.clickOnMenuItem("Upload");
        solo.clickOnText("Dropbox");
        solo.assertCurrentActivity("Expected DropBoxAuthorizationActivity","DropBoxAuthorizationActivity");
    }

    

}
