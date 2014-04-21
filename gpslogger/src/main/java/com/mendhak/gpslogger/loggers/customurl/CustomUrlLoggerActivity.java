/*******************************************************************************
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.mendhak.gpslogger.loggers.customurl;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.mendhak.gpslogger.R;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;


public class CustomUrlLoggerActivity extends Activity implements View.OnClickListener {


    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(CustomUrlLoggerActivity.class.getSimpleName());

    public void onCreate(Bundle savedInstanceState) {
        tracer.debug("CustomUrlLogger Settings Screen");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customurl);


        EditText urlText = (EditText) findViewById(R.id.customUrlText);
        CheckBox urlEnabled = (CheckBox) findViewById(R.id.customUrlEnabled);

        Button btnSave = (Button) findViewById(R.id.customurl_btnSave);
        btnSave.setOnClickListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean customUrlEnabled = prefs.getBoolean("log_customurl_enabled", false);
        urlEnabled.setChecked(customUrlEnabled);


        String customUrl = prefs.getString("log_customurl_url", "");
        if (customUrl.length() > 0) {
            urlText.setText(customUrl);
        }

        TextView legendView = (TextView) findViewById(R.id.textViewLegend);

        String legend = MessageFormat.format("{0} %LAT\n{1} %LON\n{2} %DESC\n{3} %SAT\n{4} %ALT\n{5} %SPD\n{6} %ACC\n{7} %DIR\n{8} %PROV\n{9} %TIME\n{10} %BATT\n{11} %AID\n{12} %SER",
                getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                getString(R.string.txt_time_isoformat), "Battery:", "Android ID:", "Serial:");
        legendView.setText(legend);

    }


    @Override
    public void onClick(View view) {

        tracer.debug("Saving values");
        EditText urlText = (EditText) findViewById(R.id.customUrlText);
        CheckBox urlEnabled = (CheckBox) findViewById(R.id.customUrlEnabled);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("log_customurl_enabled", urlEnabled.isChecked());
        editor.putString("log_customurl_url", urlText.getText().toString());
        editor.commit();

        Toast.makeText(getApplicationContext(), getString(R.string.values_saved), Toast.LENGTH_LONG).show();
    }
}
