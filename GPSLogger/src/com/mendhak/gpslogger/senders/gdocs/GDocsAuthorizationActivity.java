package com.mendhak.gpslogger.senders.gdocs;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.mendhak.gpslogger.GpsMainActivity;

public class GDocsAuthorizationActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onResume()
    {

        super.onResume();

        WebView webview = new WebView(this);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setVisibility(View.VISIBLE);
        setContentView(webview);

        String authorizationUrl = GDocsHelper.GetAuthorizationRequestUrl(getApplicationContext());
        

        /* WebViewClient must be set BEFORE calling loadUrl! */
        webview.setWebViewClient(new WebViewClient()
        {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap bitmap)
            {
                System.out.println("onPageStarted : " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                if(GDocsHelper.IsSuccessfulRedirectUrl(url))
                {
                    try
                    {
                        if (url.contains("code="))
                        {
                            
                            GDocsHelper.SaveAccessTokenFromUrl(url, getApplicationContext());

                            view.setVisibility(View.INVISIBLE);
                            startActivity(new Intent(GDocsAuthorizationActivity.this, GpsMainActivity.class));
                            finish();
                        }
                        else if (url.contains("error="))
                        {
                            view.setVisibility(View.INVISIBLE);
                            GDocsHelper.ClearAccessToken(getApplicationContext());
                            startActivity(new Intent(GDocsAuthorizationActivity.this, GDocsSettingsActivity.class));
                            finish();
                        }

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
                System.out.println("onPageFinished : " + url);
            }


        });

        webview.loadUrl(authorizationUrl);


    }

}