//https://www.dropbox.com/developers/start/setup#android

package com.mendhak.gpslogger.senders.dropbox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;

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
    }

    /**
     * Convenience function to change UI state based on being logged in
     *
     * @param newState The new logged in state
     */
    private void setLoggedIn(boolean newState)
    {
        loggedIn = newState;
        Button authButton = (Button) findViewById(R.id.btnAuthorizeDropBox);
        TextView tvDescription = (TextView) findViewById(R.id.lblAuthorizeDropBox);

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
