package com.mendhak.gpslogger;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

/**
 * Created by mendhak on 15/04/14.
 */
public class SmallInstrumentationTestRunner extends InstrumentationTestRunner {

    @Override
    public void onCreate(Bundle arguments) {

        arguments.putString("size", "small");

        super.onCreate(arguments);
    }
}