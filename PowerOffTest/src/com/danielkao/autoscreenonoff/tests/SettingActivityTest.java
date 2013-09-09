package com.danielkao.autoscreenonoff.tests;

import android.preference.SwitchPreference;
import android.test.ActivityInstrumentationTestCase2;
import com.danielkao.autoscreenonoff.AutoScreenOnOffPreferenceActivity;
import com.danielkao.autoscreenonoff.CV;

/**
 * Created by plateau on 2013/06/03.
 */
public class SettingActivityTest extends ActivityInstrumentationTestCase2<AutoScreenOnOffPreferenceActivity> {
    AutoScreenOnOffPreferenceActivity activity;

    public SettingActivityTest(){
        super("com.danielkao.autoscreenonoff", AutoScreenOnOffPreferenceActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        activity = (AutoScreenOnOffPreferenceActivity)getActivity();
    } // end of setUp() method definition

    public void testSettingActivityExists(){
        assertTrue(activity!=null);
    }
    public void testPrefState(){
        SwitchPreference spAuto = (SwitchPreference) activity.getPreferenceScreen().findPreference(CV.PREF_AUTO_ON);
        SwitchPreference spCharge = (SwitchPreference) activity.getPreferenceScreen().findPreference(CV.PREF_CHARGING_ON);
        if(spAuto.isEnabled())
            assertTrue(!spCharge.isEnabled());
        if(spCharge.isEnabled())
            assertTrue(!spAuto.isEnabled());
    }
}
