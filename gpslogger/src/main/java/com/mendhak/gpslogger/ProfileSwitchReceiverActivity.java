package com.mendhak.gpslogger;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.ProfileEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

import de.greenrobot.event.EventBus;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okio.BufferedSink;
import okio.Okio;

import static android.R.attr.data;

public class ProfileSwitchReceiverActivity extends AppCompatActivity {

    private static final Logger LOG = Logs.of(ProfileSwitchReceiverActivity.class);


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_switch_receiver);
        Uri url = getIntent().getData();

        EventBus.getDefault().register(this);

        LOG.debug("Got implicit event with URL:"+url.toString());

        Toast.makeText(this, String.format("Downloading profile from: %s",url.toString()), Toast.LENGTH_LONG).show();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();

        okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()));

        OkHttpClient client = okBuilder.build();

        Request request = new Request.Builder().url(url.toString()).build();
        BufferedSink sink = null;
        try {
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                LOG.debug("Success - response code " + response);
                String filename = URLUtil.guessFileName(url.toString(), null, null);

                File downloadedFile = new File(Files.storageFolder(getApplicationContext()) + "/" + filename);
                sink = Okio.buffer(Okio.sink(downloadedFile));
                sink.writeAll(response.body().source());
                //sink.close();

                Toast.makeText(this, String.format("Downloading profile successful, name: %s", filename), Toast.LENGTH_LONG).show();

                EventBus.getDefault().post(new ProfileEvents.SwitchToProfile(filename));
                Toast.makeText(this, String.format("Switched to profile %s, please verify.", filename), Toast.LENGTH_LONG).show();
            } else {
                LOG.error("Unexpected response code " + response);
                Toast.makeText(this, String.format("Downloading profile failed, responsecode: %s", response), Toast.LENGTH_LONG).show();
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


        Intent intent = new Intent(ProfileSwitchReceiverActivity.this, GpsMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

}
