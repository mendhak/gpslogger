package com.mendhak.gpslogger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class TestPreference extends PreferenceActivity {
    private LinearLayout rootView;
    private LinearLayout titleView;
    private ListView preferenceView;
    private TextView textView;
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        rootView = new LinearLayout(this);
        rootView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        rootView.setOrientation(LinearLayout.VERTICAL);
       
        textView = new TextView(this);
        textView.setText(R.string.app_name);
       
        titleView = new LinearLayout(this);
        titleView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 26));
        //titleView.setBackgroundResource(R.drawable.bar_back);
        titleView.addView(textView);
       
        preferenceView = new ListView(this);
        preferenceView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        preferenceView.setId(android.R.id.list);
       
        PreferenceScreen screen = createPreferenceHierarchy();
        screen.bind(preferenceView);
        preferenceView.setAdapter(screen.getRootAdapter());
       
        rootView.addView(titleView);
        rootView.addView(preferenceView);
       
        this.setContentView(rootView);
        setPreferenceScreen(screen);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
       
        // Inline preferences
        PreferenceCategory cat1 = new PreferenceCategory(this);
        cat1.setTitle("Cat 1");
        root.addPreference(cat1);

        for(int i = 0; i < 5; i ++)
        {
            // Toggle preference
            CheckBoxPreference togglePref = new CheckBoxPreference(this);
            togglePref.setKey("toggle_check" + Integer.toString(i));
            togglePref.setTitle("Toggle Me");
            togglePref.setChecked(prefs.getBoolean("toggle_check" + Integer.toString(i), false));
            togglePref.setSummary("Checkbox " + Integer.toString(i));
            cat1.addPreference(togglePref);
        }
       
     // Inline preferences
        PreferenceCategory cat2 = new PreferenceCategory(this);
        cat2.setTitle("Cat 2");
        root.addPreference(cat2);

        for(int i = 6; i < 13; i ++)
        {
            // Toggle preference
            CheckBoxPreference togglePref = new CheckBoxPreference(this);
            togglePref.setKey("toggle_check" + Integer.toString(i));
            togglePref.setTitle("Toggle Me");
            togglePref.setChecked(prefs.getBoolean("toggle_check" + Integer.toString(i), false));
            togglePref.setSummary("Checkbox " + Integer.toString(i));
            cat2.addPreference(togglePref);
        }
       
        return root;
    }
}