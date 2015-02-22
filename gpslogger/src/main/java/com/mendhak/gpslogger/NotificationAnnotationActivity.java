package com.mendhak.gpslogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

public class NotificationAnnotationActivity extends Activity {

    //Called from the 'Annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private org.slf4j.Logger tracer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tracer = LoggerFactory.getLogger(GpsLoggingService.class.getSimpleName());

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.add_description);
        alert.setMessage(R.string.letters_numbers);

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                tracer.info("Notification annotation: " + input.getText().toString());

                Intent serviceIntent = new Intent(getApplicationContext(), GpsLoggingService.class);
                serviceIntent.putExtra("setnextpointdescription", input.getText().toString());
                getApplicationContext().startService(serviceIntent);

                finish();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });

        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.dismiss();
                finish();
            }
        });

        AlertDialog alertDialog = alert.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            super.finish();
        }

        return super.onKeyDown(keyCode, event);
    }
}
