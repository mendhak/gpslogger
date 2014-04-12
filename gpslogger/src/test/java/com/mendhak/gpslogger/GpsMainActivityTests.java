package com.mendhak.gpslogger;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

        Context context = getInstrumentation().getTargetContext();
        final SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferencesEditor.putBoolean("navigation_drawer_learned", true);
        preferencesEditor.commit();

        solo = new Solo(getInstrumentation(), getActivity());

    }

    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testCanSeeMenuItems() {
        solo.sendKey(Solo.MENU);
        assertTrue("Could not find menu Logging Settings", solo.searchText(solo.getCurrentActivity().getString(R.string.title_drawer_loggingsettings)));
        assertTrue("Could not find menu Upload Settings", solo.searchText(solo.getCurrentActivity().getString(R.string.title_drawer_uploadsettings)));
        assertTrue("Could not find menu General Settings", solo.searchText(solo.getCurrentActivity().getString(R.string.title_drawer_generalsettings)));
    }

    public void testHelpButtonOpensFAQPage(){
        solo.clickOnView(solo.getView(R.id.imgHelp));
        solo.assertCurrentActivity("FAQ Screen", Faqtivity.class);
    }

    public void testSinglePointButtonDisabledWhenLoggingStarted() {
        //setDrawerVisibility(false);
        solo.setNavigationDrawer(Solo.CLOSED);
        solo.clickOnView(solo.getView(R.id.simple_play));
        solo.sleep(500);
        assertFalse("One Point button should be disabled if main logging enabled", ((GpsMainActivity) solo.getCurrentActivity()).mnuOnePoint.isEnabled());
        solo.clickOnView(solo.getView(R.id.simple_stop));
        solo.sleep(500);
        assertTrue("One Point button should be enabled if main logging stopped",  ((GpsMainActivity) solo.getCurrentActivity()).mnuOnePoint.isEnabled());
        solo.finishOpenedActivities();
    }


    public void testAnnotateButtonDisabledIfGpxKmlUrlAreDisabled(){

        solo.sendKey(Solo.MENU);
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.title_drawer_loggingsettings));
        solo.scrollToTop();
        if(solo.isCheckBoxChecked(0)) { solo.clickOnCheckBox(0); }
        if(solo.isCheckBoxChecked(1)) { solo.clickOnCheckBox(1); }
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.log_customurl_title));
        if(solo.isCheckBoxChecked(0)) { solo.clickOnCheckBox(0); }
        solo.goBack();
        solo.goBack();
        assertFalse("Annotate button should be disabled if GPX, KML, URL disabled", ((GpsMainActivity) solo.getCurrentActivity()).mnuAnnotate.isEnabled());

    }

    public void testSpinnerNavigation(){
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.view_simple));
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.view_detailed));
        solo.finishOpenedActivities();

        launchActivity("com.mendhak.gpslogger", GpsMainActivity.class,null);

        //solo.waitForFragmentById(R.layout.fragment_detailed_view);
        assertTrue("Detailed view should be visible if previously selected",
                solo.getView(R.id.detailedview_lat_label).isShown());

        solo.clickOnText(solo.getCurrentActivity().getString(R.string.view_detailed));
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.view_simple));

    }

    public void testAutoEmailsRequireFilledValues() {
        solo.sendKey(Solo.MENU);
        solo.setNavigationDrawer(Solo.OPENED);
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.autoemail_title));
        solo.clickOnCheckBox(0);
        solo.goBack();
        assertTrue("Email form without valid values should show alert dialog", solo.searchText(solo.getCurrentActivity().getString(R.string.autoemail_invalid_form)));
        solo.clickOnText("OK");
        assertTrue("Enable emails checkbox should be unchecked", !solo.isCheckBoxChecked(0));
    }

    public void testAutoFtpRequireFilledValues() {
        solo.sendKey(Solo.MENU);
        solo.setNavigationDrawer(Solo.OPENED);
        solo.clickOnText(solo.getCurrentActivity().getString(R.string.autoftp_setup_title));
        solo.clickOnCheckBox(0);
        solo.goBack();
        assertTrue("FTP form without valid values should show alert dialog", solo.searchText(solo.getCurrentActivity().getString(R.string.autoemail_invalid_form)));
        solo.clickOnText("OK");
        assertTrue("Enable FTP checkbox should be unchecked", !solo.isCheckBoxChecked(0));
    }

}