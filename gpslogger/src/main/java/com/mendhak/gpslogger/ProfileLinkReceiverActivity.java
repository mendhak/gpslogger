package com.mendhak.gpslogger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.events.ProfileEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.loggers.Streams;
import com.mendhak.gpslogger.ui.Dialogs;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileLinkReceiverActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private static final Logger LOG = Logs.of(ProfileLinkReceiverActivity.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();
        String action = intent.getAction();
        final Uri data = intent.getData();

        LOG.info("Received a gpslogger properties file URL to be handled. " + data.toString());

        Dialogs.progress(ProfileLinkReceiverActivity.this,getString(R.string.please_wait),getString(R.string.please_wait));
        new Thread(new DownloadProfileRunner(data.toString())).start();

    }

    Runnable afterDownload = new Runnable() {
        @Override
        public void run() {
            Dialogs.hideProgress();

            Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
            serviceIntent.putExtra(IntentConstants.SWITCH_PROFILE, "test");
            ContextCompat.startForegroundService(getApplicationContext(),  serviceIntent);

            Intent intent = new Intent(getApplicationContext(), GpsMainActivity.class);
            startActivity(intent);

            finish();
        }
    };


    private class DownloadProfileRunner implements Runnable{

        private String url;

        private DownloadProfileRunner(String url){
            this.url = url;
        }

        @Override
        public void run() {
            LOG.debug("Runnable...");
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            try {
                Response response = client.newCall(request).execute();

                if(response.isSuccessful()){
                    LOG.debug("Response successful");
                    InputStream inputStream = response.body().byteStream();

                    File destFile =  new File(Files.storageFolder(getApplicationContext()) + "/test.properties");
                    OutputStream outputStream = new FileOutputStream(destFile);
                    Streams.copyIntoStream(inputStream, outputStream);
                    response.body().close();
                    LOG.debug("Wrote to file");

                    handler.post(afterDownload);
                }

            } catch (IOException e) {
                LOG.error("Could not download properties file", e);
            }
        }


    }


}
