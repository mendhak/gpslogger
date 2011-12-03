//https://www.dropbox.com/developers/start/setup#android

package com.mendhak.gpslogger.senders.dropbox;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;

import java.io.*;

public class DropBoxAuthorizationActivity extends Activity implements
                                                           View.OnClickListener
{


    DropBoxHelper helper;
    boolean loggedIn = false;



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dropboxauth);

        Button authButton = (Button) findViewById(R.id.btnAuthorizeDropBox);
        authButton.setOnClickListener(this);

        Button testButton = (Button) findViewById(R.id.btnDBTest);
        testButton.setOnClickListener(this);

        helper = new DropBoxHelper(getBaseContext());

        // Display the proper UI state if logged in or not
        setLoggedIn(helper.IsLinked());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        try
        {
            helper.FinishAuthorization();
        }
        catch(Exception e)
        {
            Utilities.MsgBox(getString(R.string.error), getString(R.string.dropbox_couldnotauthorize),
                    DropBoxAuthorizationActivity.this);
            Utilities.LogError("DropBoxAuthorizationActivity.onResume", e);
        }

        setLoggedIn(helper.IsLinked());
        finish();
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean mli)
    {
        loggedIn = mli;
        Button authButton = (Button) findViewById(R.id.btnAuthorizeDropBox);
        TextView tvDescription = (TextView)findViewById(R.id.lblAuthorizeDropBox);

        if(loggedIn)
        {
            authButton.setText(R.string.dropbox_unauthorize);
            tvDescription.setText(R.string.dropbox_unauthorize_description);
        }
        else
        {
            authButton.setText(R.string.dropbox_authorize);
            tvDescription.setText(R.string.dropbox_authorize_description);
        }
    }

    public void onClick(View view)
    {

//        if(view.getId() == R.id.btnDBTest)
//        {
//            // Uploading content.
//            String fileContents = "Hello World!";
//            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContents.getBytes());
//            try
//            {
//                Utilities.ShowProgress(DropBoxAuthorizationActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
//                File gpsDir = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
//                File testFile = new File(gpsDir,  "test.gpx");
//
//                FileInputStream fis = new FileInputStream(testFile);
//                DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(testFile.getName(), fis, testFile.length(), null);
//                Log.i("DbExampleLog", "The uploaded file's rev is: " + upEntry.rev);
//                Utilities.HideProgress();
//            }
//            catch(DropboxUnlinkedException e)
//            {
//                // User has unlinked, ask them to link again here.
//                Log.e("DbExampleLog", "User has unlinked.");
//            }
//            catch(DropboxException e)
//            {
//                Log.e("DbExampleLog", "Something went wrong while uploading.");
//            }
//            catch(FileNotFoundException e)
//            {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//            return;
//        }

        // This logs you out if you're logged in, or vice versa
        if(loggedIn)
        {
            logOut();
        }
        else
        {
            try
            {
                helper.StartAuthentication(DropBoxAuthorizationActivity.this);
            }
            catch(Exception e)
            {
                Utilities.LogError("DropBoxAuthorizationActivity.onClick", e);
            }
        }
    }

    private void logOut()
    {
        helper.UnLink();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }




}
