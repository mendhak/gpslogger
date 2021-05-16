package com.mendhak.gpslogger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.ui.Dialogs;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;


public class ProfileLinkReceiverActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private static final Logger LOG = Logs.of(ProfileLinkReceiverActivity.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();
        final String propertiesUrl = intent.getDataString().replace("gpslogger://properties/","");

        LOG.info("Received a gpslogger properties file URL to be handled. " + propertiesUrl);

        Dialogs.progress(ProfileLinkReceiverActivity.this,getString(R.string.please_wait),getString(R.string.please_wait));
        new Thread(new DownloadProfileRunner(propertiesUrl)).start();

    }

    private class DownloadProfileRunner implements Runnable{

        private String url;

        private DownloadProfileRunner(String url){
            this.url = url;
        }

        @Override
        public void run() {

            try {
                final String profileName = Files.getBaseName(url);
                File destFile =  new File(Files.storageFolder(getApplicationContext()) + "/" + profileName + ".properties");
                Files.DownloadFromUrl(url, destFile);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Dialogs.hideProgress();

                        Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
                        serviceIntent.putExtra(IntentConstants.SWITCH_PROFILE, profileName);
                        ContextCompat.startForegroundService(getApplicationContext(),  serviceIntent);

                        Intent intent = new Intent(getApplicationContext(), GpsMainActivity.class);
                        startActivity(intent);


                    }
                });

            } catch (final IOException e) {
                LOG.error("Could not download properties file", e);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Dialogs.hideProgress();

                        Dialogs.error(getString(R.string.error), e.getMessage(),e.getMessage(),e, ProfileLinkReceiverActivity.this);
                    }
                });

            }
        }

    }


}
