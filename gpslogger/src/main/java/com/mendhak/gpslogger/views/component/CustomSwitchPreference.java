package com.mendhak.gpslogger.views.component;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;


/**
 * This class only exists due to yet *another* SwitchPreference bug where the value gets reset if you scroll
 * the switchpreference off screen (the OS reuses these components...)
 * http://stackoverflow.com/questions/15632215/preference-items-being-automatically-re-set/15744076#15744076
 */

public class CustomSwitchPreference extends SwitchPreference {
    public CustomSwitchPreference(Context context) {
        super(context);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}