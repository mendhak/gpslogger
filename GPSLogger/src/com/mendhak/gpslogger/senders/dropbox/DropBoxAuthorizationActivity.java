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

    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "DROPBOX_ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "DROPBOX_ACCESS_SECRET";
    final static private Session.AccessType ACCESS_TYPE = Session.AccessType.APP_FOLDER;
    boolean loggedIn = false;
    DropboxAPI<AndroidAuthSession> dropboxApi;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dropboxauth);

        AndroidAuthSession session = buildSession();
        dropboxApi = new DropboxAPI<AndroidAuthSession>(session);

        Button authButton = (Button) findViewById(R.id.btnAuthorizeDropBox);
        authButton.setOnClickListener(this);

        Button testButton = (Button) findViewById(R.id.btnDBTest);
        testButton.setOnClickListener(this);


        // Display the proper UI state if logged in or not
        setLoggedIn(dropboxApi.getSession().isLinked());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        AndroidAuthSession session = dropboxApi.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if(session.authenticationSuccessful())
        {
            try
            {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
            }
            catch(IllegalStateException e)
            {
                Utilities.MsgBox(getString(R.string.error), getString(R.string.dropbox_couldnotauthorize),
                        DropBoxAuthorizationActivity.this);
                Utilities.LogError("DropBoxAuthorizationActivity.onResume", e);
            }
        }
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
            tvDescription.setVisibility(View.GONE);
        }
        else
        {
            authButton.setText(R.string.dropbox_authorize);
            tvDescription.setVisibility(View.VISIBLE);
        }
    }

    public void onClick(View view)
    {

        if(view.getId() == R.id.btnDBTest)
        {
            // Uploading content.
            String fileContents = "Hello World!";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContents.getBytes());
            try
            {
                Utilities.ShowProgress(DropBoxAuthorizationActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
                File gpsDir = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
                File testFile = new File(gpsDir,  "test.gpx");

                FileInputStream fis = new FileInputStream(testFile);
                DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(testFile.getName(), fis, testFile.length(), null);
                Log.i("DbExampleLog", "The uploaded file's rev is: " + upEntry.rev);
                Utilities.HideProgress();
            }
            catch(DropboxUnlinkedException e)
            {
                // User has unlinked, ask them to link again here.
                Log.e("DbExampleLog", "User has unlinked.");
            }
            catch(DropboxException e)
            {
                Log.e("DbExampleLog", "Something went wrong while uploading.");
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return;
        }

        // This logs you out if you're logged in, or vice versa
        if(loggedIn)
        {
            logOut();
        }
        else
        {
            try
            {
                // Start the remote authentication
                dropboxApi.getSession().startAuthentication(DropBoxAuthorizationActivity.this);
            }
            catch(Exception e)
            {
                Utilities.LogError("DropBoxAuthorizationActivity.onClick", e);
            }
        }
    }

    private void logOut()
    {
        // Remove credentials from the session
        dropboxApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }


    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeKeys(String key, String secret)
    {
        // Save the access key for later
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private void clearKeys()
    {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }


    private AndroidAuthSession buildSession()
    {
        int dropboxAppKey = getResources().getIdentifier("dropbox_appkey", "string", getPackageName());
        int dropboxAppSecret = getResources().getIdentifier("dropbox_appsecret", "string", getPackageName());
        AppKeyPair appKeyPair = new AppKeyPair(getString(dropboxAppKey), getString(dropboxAppSecret));
        AndroidAuthSession session;

        String[] stored = getKeys();
        if(stored != null)
        {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        }
        else
        {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @return Array of [access_key, access_secret], or null if none stored
     */
    private String[] getKeys()
    {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if(key != null && secret != null)
        {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        }
        else
        {
            return null;
        }
    }

}
