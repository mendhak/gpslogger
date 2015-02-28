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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import org.slf4j.LoggerFactory;

public class Faqtivity extends ActionBarActivity {

    WebView browser;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(Faqtivity.class.getSimpleName());

    /**
     * Event raised when the form is created for the first time
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        tracer.debug("Faqtivity.onCreate");

        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_faq);

        browser = (WebView) findViewById(R.id.faqwebview);
        WebSettings settings = browser.getSettings();
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setJavaScriptEnabled(true);

        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        browser.loadUrl("http://code.mendhak.com/gpslogger/index.html");
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
}
