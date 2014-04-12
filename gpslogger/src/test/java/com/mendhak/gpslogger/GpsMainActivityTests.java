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
    }

    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testCanSeeMenuItems() {
        setDrawerVisibility(true);
        solo.sleep(100);
        assertTrue("Could not find menu Logging Settings", solo.searchText(getActivity().getString(R.string.title_drawer_loggingsettings)));
        assertTrue("Could not find menu Upload Settings", solo.searchText(getActivity().getString(R.string.title_drawer_uploadsettings)));
        assertTrue("Could not find menu General Settings", solo.searchText(getActivity().getString(R.string.title_drawer_generalsettings)));
    }

    public void testHelpButtonOpensFAQPage(){
        setDrawerVisibility(false);
        solo.clickOnView(solo.getView(R.id.imgHelp));
        solo.assertCurrentActivity("FAQ Screen", Faqtivity.class);
        solo.scrollDown();
        solo.scrollToBottom();
        solo.scrollToTop();
    }

    public void testSinglePointButtonDisabledWhenLoggingStarted() {

        solo.clickOnView(solo.getView(R.id.simple_play));
        solo.sleep(500);
        assertFalse("One Point button should be disabled if main logging enabled", ((GpsMainActivity) solo.getCurrentActivity()).mnuOnePoint.isEnabled());
        solo.clickOnView(solo.getView(R.id.simple_stop));
        solo.sleep(500);
        assertTrue("One Point button should be enabled if main logging stopped",  ((GpsMainActivity) solo.getCurrentActivity()).mnuOnePoint.isEnabled());
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
        solo.waitForText(getActivity().getString(R.string.title_drawer_loggingsettings));
        solo.clickOnText(getActivity().getString(R.string.title_drawer_loggingsettings));
        solo.scrollToTop();
        if(solo.isCheckBoxChecked(0)) { solo.clickOnCheckBox(0); }
        if(solo.isCheckBoxChecked(1)) { solo.clickOnCheckBox(1); }
        solo.clickOnText(getActivity().getString(R.string.log_customurl_title));
        if(solo.isCheckBoxChecked(0)) { solo.clickOnCheckBox(0); }
        solo.goBack();
        solo.goBack();
        assertFalse("Annotate button should be disabled if GPX, KML, URL disabled", ((GpsMainActivity) solo.getCurrentActivity()).mnuAnnotate.isEnabled());

    }

    public void testSpinnerNavigation(){
        setDrawerVisibility(false);
        solo.clickOnText(getActivity().getString(R.string.view_simple));
        solo.clickOnText(getActivity().getString(R.string.view_detailed));
        solo.finishOpenedActivities();

        launchActivity("com.mendhak.gpslogger", GpsMainActivity.class,null);

        //solo.waitForFragmentById(R.layout.fragment_detailed_view);
        assertTrue("Detailed view should be visible if previously selected",
                solo.getView(R.id.detailedview_lat_label).isShown());

        solo.clickOnText(getActivity().getString(R.string.view_detailed));
        solo.clickOnText(getActivity().getString(R.string.view_simple));

    }

}