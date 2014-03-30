package com.mendhak.gpslogger.loggers.customurl;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.Utilities;

import java.text.MessageFormat;


public class CustomUrlLoggerActivity extends Activity {


    public void onCreate(Bundle savedInstanceState)
    {
        Utilities.LogDebug("CustomUrlLogger Settings Screen");
        super.onCreate(savedInstanceState);
        // enable the home button so you can go back to the main screen
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.customurl);


        EditText urlText = (EditText)findViewById(R.id.customUrlText);
        CheckBox urlEnabled = (CheckBox)findViewById(R.id.customUrlEnabled);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean customUrlEnabled = prefs.getBoolean("log_customurl_enabled", false);
        urlEnabled.setChecked(customUrlEnabled);


        String customUrl = prefs.getString("log_customurl_url", "");
        if(customUrl.length() > 0)
        {
            urlText.setText(customUrl);
        }

        TextView legendView = (TextView)findViewById(R.id.textViewLegend);

        String legend = MessageFormat.format("{0} %LAT\n{1} %LON\n{2} %DESC\n{3} %SAT\n{4} %ALT\n{5} %SPD\n{6} %ACC\n{7} %DIR\n{8} %PROV\n{9} %TIME\n{10} %BATT\n{11} %AID\n{12} %SER",
                getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                getString(R.string.txt_time_isoformat), "Battery:", "Android ID:", "Serial:");
        legendView.setText(legend);

    }

    /**
     * Called when one of the menu items is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case android.R.id.home:
                Intent intent = new Intent(this, GpsMainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        Utilities.LogDebug("CustomUrlLoggerActivity - Back pressed, saving values");
        EditText urlText = (EditText)findViewById(R.id.customUrlText);
        CheckBox urlEnabled = (CheckBox)findViewById(R.id.customUrlEnabled);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor   = prefs.edit();
        editor.putBoolean("log_customurl_enabled", urlEnabled.isChecked());
        editor.putString("log_customurl_url", urlText.getText().toString());
        editor.commit();

        super.onBackPressed();
    }

}
