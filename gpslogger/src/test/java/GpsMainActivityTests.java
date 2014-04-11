package com.mendhak.gpslogger;

import android.app.ActionBar;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageButton;
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
        solo.sendKey(Solo.MENU);
    }


    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testCanSeeMenuItems() {
        solo.assertCurrentActivity("Main Screen", GpsMainActivity.class);
        solo.sendKey(Solo.MENU);
        assertTrue("Could not find menu Logging Settings", solo.searchText("Logging settings"));
        assertTrue("Could not find menu Upload Settings", solo.searchText("Upload settings"));
        assertTrue("Could not find menu General Settings", solo.searchText("General settings"));
        solo.sendKey(Solo.MENU);
    }

    public void testHelpButtonOpensFAQPage(){
        solo.sendKey(Solo.MENU);
        solo.clickOnView(solo.getView(R.id.imgHelp));
        solo.assertCurrentActivity("FAQ Screen", Faqtivity.class);
        solo.scrollDown();
        solo.scrollToBottom();
        solo.scrollToTop();
        //solo.clickOnActionBarItem(R.id.imgHelp);
//        ImageButton img = (ImageButton) solo.getView(R.layout.actionbar).findViewById(R.id.imgHelp);
//        solo.clickOnView(img);
//
//        //solo.clickOnImageButton(R.id.imgHelp);
//        solo.assertCurrentActivity("FAQ Screen", Faqtivity.class);
    }

}