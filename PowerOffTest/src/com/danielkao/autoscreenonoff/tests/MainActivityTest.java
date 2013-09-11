package com.danielkao.autoscreenonoff.tests;

import android.test.ActivityInstrumentationTestCase2;
import com.danielkao.autoscreenonoff.ui.MainActivity;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.danielkao.autoscreenonoff.tests.MainActivityTest \
 * com.danielkao.autoscreenonoff.tests/android.test.InstrumentationTestRunner
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public MainActivityTest() {
        super("com.danielkao.autoscreenonoff", MainActivity.class);
    }

    public void test1(){
        assertTrue(true);
    }
}
