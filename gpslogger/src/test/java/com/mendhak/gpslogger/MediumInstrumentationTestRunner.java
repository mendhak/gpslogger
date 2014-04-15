package com.mendhak.gpslogger;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;


public class MediumInstrumentationTestRunner  extends InstrumentationTestRunner {
    @Override
    public void onCreate(Bundle arguments) {

        arguments.putString("size", "medium");

        super.onCreate(arguments);
    }
}
