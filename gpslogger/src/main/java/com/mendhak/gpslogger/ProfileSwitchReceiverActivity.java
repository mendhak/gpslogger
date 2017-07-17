package com.mendhak.gpslogger;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class ProfileSwitchReceiverActivity extends AppCompatActivity {

    private static final Logger LOG = Logs.of(ProfileSwitchReceiverActivity.class);
    private WebView webView = null;
    private Button button = null;
    private String profileName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_switch_receiver);
        this.webView = (WebView) findViewById(R.id.profileswitch_webview);
        this.button = (Button) findViewById(R.id.profileswitch_button_dismiss);
        button.setEnabled(false);

        Uri url = getIntent().getData();

        LOG.debug("Got implicit event with URL:"+url.toString());

        Toast.makeText(this, String.format("Downloading profile from: %s",url.toString()), Toast.LENGTH_LONG).show();

        ProfileFetcher fetcher = new ProfileFetcher();
        fetcher.execute(new String[]{url.toString()});
    }

    private class ProfileFetcher extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = null;
            OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();

            okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()));

            OkHttpClient client = okBuilder.build();

            Request request = new Request.Builder().url(params[0]).build();
            BufferedSink sink = null;
            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    LOG.debug("Success - response code " + response);
                    String filename = URLUtil.guessFileName(params[0], null, null);
                    profileName = filename.split("\\.",-1)[0];

                    File downloadedFile = new File(Files.storageFolder(getApplicationContext()) + "/" + filename);
                    sink = Okio.buffer(Okio.sink(downloadedFile));
                    sink.writeAll(response.body().source());

                    LOG.debug(String.format("Downloading profile successful, filename: %s, profilename: %s", filename, profileName));

                    //Fetch information website from properties to pass to the postExecute hook
                    Properties props = new Properties();
                    props.load(sink.buffer().inputStream());
                    LOG.debug(props.stringPropertyNames().toString());
                    result = props.getProperty("provisioned_profile_info_url","https://code.mendhak.com/gpslogger/new_profile_provisioned/blank");
                    LOG.debug(result);

                } else {
                    LOG.error("Unexpected response code " + response);
                }

                response.body().close();
            } catch (Throwable t) {
                LOG.debug("fetching url failed with throwable",t);
            } finally {
                try{
                    if (sink!=null) sink.close();
                } catch (IOException e) {

                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            LOG.debug("Post Execute with String:" + s);
            super.onPostExecute(s);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(s);
            button.setEnabled(true);
        }
    }

    public void onClick(View view){
        LOG.debug(String.format("Switched to profile %s, please verify.", profileName));
        Intent intentService = new Intent(ProfileSwitchReceiverActivity.this, GpsLoggingService.class);
        intentService.putExtra(IntentConstants.SWITCH_PROFILE,profileName);
        startService(intentService);

        Intent intent = new Intent(ProfileSwitchReceiverActivity.this, GpsMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

}
