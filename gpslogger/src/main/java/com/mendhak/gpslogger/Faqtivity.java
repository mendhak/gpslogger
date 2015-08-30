/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/


package com.mendhak.gpslogger;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mendhak.gpslogger.views.component.ExpandableListAdapter;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Faqtivity extends ActionBarActivity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    WebView browser;
    private org.slf4j.Logger tracer = LoggerFactory.getLogger(Faqtivity.class.getSimpleName());
    /**
     * Event raised when the form is created for the first time
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_faq);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        }
        catch(Exception ex){
            //http://stackoverflow.com/questions/26657348/appcompat-v7-v21-0-0-causing-crash-on-samsung-devices-with-android-v4-2-2
            tracer.error("Thanks for this, Samsung", ex);
        }

        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                if(groupPosition != previousGroup)
                    expListView.collapseGroup(previousGroup);
                previousGroup = groupPosition;

            }
        });

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

//
//        browser = (WebView) findViewById(R.id.faqwebview);
//        WebSettings settings = browser.getSettings();
//        settings.setLoadWithOverviewMode(true);
//        settings.setUseWideViewPort(true);
//        settings.setBuiltInZoomControls(true);
//        settings.setDisplayZoomControls(false);
//        settings.setJavaScriptEnabled(true);
//
//        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//
//        browser.loadUrl("http://code.mendhak.com/gpslogger/index.html");

    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        listDataHeader.add(getString(R.string.faq_generalsection));
        listDataHeader.add(getString(R.string.faq_preferencesandfilters));
        listDataHeader.add(getString(R.string.faq_advancedsection));

        List<String> generalTopics = new ArrayList<String>();
        generalTopics.add(getString(R.string.faq_topic_whyisntitaccurate));
        generalTopics.add(getString(R.string.faq_topic_howtoremovenotification));
        generalTopics.add(getString(R.string.faq_topic_usemylocaltimezone));
        generalTopics.add(getString(R.string.faq_topic_imperial));
        generalTopics.add(getString(R.string.faq_topic_whydoesfixtakelongtime));


        List<String> preferencesAndFiltersTopics = new ArrayList<String>();
        preferencesAndFiltersTopics.add(getString(R.string.faq_topic_whatvariousfiltersmean));
        preferencesAndFiltersTopics.add(getString(R.string.faq_topic_whereisthefilelogged));
        preferencesAndFiltersTopics.add(getString(R.string.faq_topic_howtogetthefile));
        preferencesAndFiltersTopics.add(getString(R.string.faq_topic_loadingpresets));


        List<String> advancedTopics = new ArrayList<String>();
        advancedTopics.add(getString(R.string.faq_topic_howgpsworks));
        advancedTopics.add(getString(R.string.faq_topic_thirdpartyintegration));
        advancedTopics.add(getString(R.string.faq_topic_taskerintegration));


        listDataChild.put(listDataHeader.get(0), generalTopics);
        listDataChild.put(listDataHeader.get(1), preferencesAndFiltersTopics);
        listDataChild.put(listDataHeader.get(2), advancedTopics);
    }

    @Override
    protected void onDestroy() {
        setVisible(false);
        super.onDestroy();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setVisible(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
