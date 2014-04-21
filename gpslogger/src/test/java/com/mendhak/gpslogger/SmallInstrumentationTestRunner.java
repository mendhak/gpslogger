package com.mendhak.gpslogger;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

public class SmallInstrumentationTestRunner extends InstrumentationTestRunner {

    @Override
    public void onCreate(Bundle arguments) {

        arguments.putString("size", "small");

        super.onCreate(arguments);
    }
}