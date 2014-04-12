package com.mendhak.gpslogger;

import android.test.ActivityInstrumentationTestCase2;
import com.robotium.solo.Solo;


public class GpsMainActivityTests extends ActivityInstrumentationTestCase2<GpsMainActivity> {

    private Solo solo;

    public GpsMainActivityTests(){
        super(GpsMainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        //solo.sendKey(Solo.MENU);
        //solo.sleep(3000);
    }


    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

//    public void testCanSeeMenuItems() {
//        setDrawerVisibility(true);
//        solo.sleep(100);
//        assertTrue("Could not find menu Logging Settings", solo.searchText("Logging settings"));
//        assertTrue("Could not find menu Upload Settings", solo.searchText("Upload settings"));
//        assertTrue("Could not find menu General Settings", solo.searchText("General settings"));
//        //solo.sendKey(Solo.MENU);
//    }

    public void testHelpButtonOpensFAQPage(){
        setDrawerVisibility(false);
        solo.clickOnView(solo.getView(R.id.imgHelp));
        solo.assertCurrentActivity("FAQ Screen", Faqtivity.class);
        solo.scrollDown();
        solo.scrollToBottom();
        solo.scrollToTop();
    }

    public void testSinglePointButtonDisabledWhenLoggingStarted() {
        //setDrawerVisibility(false);

        solo.clickOnView(solo.getView(R.id.simple_play));
        System.out.println("Clicked on simple play");
        solo.sleep(2000);
        assertFalse("One Point button should be disabled if main logging enabled", ((GpsMainActivity) solo.getCurrentActivity()).mnuOnePoint.isEnabled());
        solo.finishOpenedActivities();
    }


    private void setDrawerVisibility(boolean visible){
        solo.sleep(100);
        if(solo.getView(R.id.drawer_layout).isShown() != visible){
            solo.sendKey(Solo.MENU);
            solo.sleep(100);
        }
    }

    public void testAnnotateButtonDisabledIfGpxKmlUrlAreDisabled(){

        setDrawerVisibility(true);
        solo.waitForText("Logging settings");
        solo.clickOnText("Logging settings");
        solo.scrollToTop();
        if(solo.isCheckBoxChecked(0)) { solo.clickOnCheckBox(0); }
        if(solo.isCheckBoxChecked(1)) { solo.clickOnCheckBox(1); }
        solo.clickOnText("Log to custom URL");
        if(solo.isCheckBoxChecked(0)) { solo.clickOnCheckBox(0); }
        solo.goBack();
        solo.goBack();
        assertFalse("Annotate button should be disabled if GPX, KML, URL disabled", ((GpsMainActivity) solo.getCurrentActivity()).mnuAnnotate.isEnabled());

    }

}