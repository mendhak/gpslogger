package com.mendhak.gpslogger.loggers.customurl;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;


public class CustomUrlLoggerActivity extends Activity {


    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(CustomUrlLoggerActivity.class.getSimpleName());

    public void onCreate(Bundle savedInstanceState)
    {
        tracer.debug("CustomUrlLogger Settings Screen");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customurl);


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


    @Override
    public void onBackPressed()
    {
        tracer.debug("CustomUrlLoggerActivity - Back pressed, saving values");
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
