package com.danielkao.autoscreenonoff;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;

public final class ConstantValues {
	static final boolean debug = false;
	
	public static final String TAG = "SensorMonitor";
	public static final String PREF = "SensorMonitorPref";
    public static final String PREF_CHARGING_ON = "prefChargingOn";
	public static final String IS_AUTO_ON = "is_audo_on";

	public static final int SERVICEACTION_TOGGLE = 0;
    public static final int SERVICEACTION_TURNON = 1;
    public static final int SERVICEACTION_TURNOFF = 2;
	public static final String SERVICEACTION = "serviceaction";
	public static final String SERVICE_INTENT_ACTION = "com.danielkao.autoscreenonoff.serviceaction";
    public static final String UPDATE_WIDGET_ACTION = "com.danielkao.autoscreenonoff.updatewidget";
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static void logv(Object...argv){
		if(!debug)
			return;
		
		if(argv.length == 1)
			Log.v(TAG, (String) argv[0]);
		else
		{
			Object [] slicedObj = Arrays.copyOfRange(argv, 1, argv.length);
			Log.v(TAG,String.format((String) argv[0], (Object[])slicedObj));
		}
	}

    public static boolean getPrefAutoOnoff(Context context) {
        SharedPreferences sp = context.getSharedPreferences(ConstantValues.PREF,
                Activity.MODE_PRIVATE);
        return sp.getBoolean(ConstantValues.IS_AUTO_ON, false);
    }

    /*
    public static boolean isPowerConnected(Context context){
        // Check battery sticky broadcast
        final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return (batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING);
    }
    */

    public static boolean getPrefChargingOn(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isPrefChargingOn = sp.getBoolean(ConstantValues.PREF_CHARGING_ON, false);
        ConstantValues.logv("prefchargingon: %b",isPrefChargingOn);
        return isPrefChargingOn;
    }
}
