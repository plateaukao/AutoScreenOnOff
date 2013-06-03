package com.danielkao.autoscreenonoff;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;

public final class CV {
	static final boolean debug = false;

	public static final String TAG = "SensorMonitor";
    public static final String PREF_CHARGING_ON = "prefChargingOn";
	public static final String PREF_AUTO_ON = "prefAutoOn";
    public static final String PREF_DISABLE_IN_LANDSCAPE= "prefDisableInLandscape";
    public static final String PREF_TIMEOUT_LOCK = "prefTimeout";
    public static final String PREF_TIMEOUT_UNLOCK = "prefTimeoutUnlock";
    public static final String PREF_VIEWED_VERSION_CODE = "prefViewedVersionCode";

    //
    public static final String SERVICEACTION = "serviceaction";
	public static final int SERVICEACTION_TOGGLE = 0;
    public static final int SERVICEACTION_TURNON = 1;
    public static final int SERVICEACTION_TURNOFF = 2;
    public static final int SERVICEACTION_UPDATE_DISABLE_IN_LANDSCAPE = 4;
    //
	public static final String SERVICE_INTENT_ACTION = "com.danielkao.autoscreenonoff.serviceaction";
    public static final String UPDATE_WIDGET_ACTION = "com.danielkao.autoscreenonoff.updatewidget";
    //
    public static final String SERVICETYPE = "servicetype";
    public static final String SERVICETYPE_CHARGING = "charging";
    public static final String SERVICETYPE_SETTING = "setting";
    public static final String SERVICETYPE_WIDGET = "widget";
    // rotation threshold
    public static final int ROTATION_THRESHOLD = 15;

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

    public static void logi(Object...argv){
        if(!debug)
            return;

        if(argv.length == 1)
            Log.i(TAG, (String) argv[0]);
        else
        {
            Object [] slicedObj = Arrays.copyOfRange(argv, 1, argv.length);
            Log.i(TAG,String.format((String) argv[0], (Object[])slicedObj));
        }
    }

    public static boolean getPrefAutoOnoff(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_AUTO_ON, false);
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
        boolean isPrefChargingOn = sp.getBoolean(PREF_CHARGING_ON, false);
        CV.logv("prefchargingon: %b", isPrefChargingOn);
        return isPrefChargingOn;
    }

    public static boolean getPrefDisableInLandscape(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isPrefDisableInLandscape = sp.getBoolean(PREF_DISABLE_IN_LANDSCAPE, false);
        CV.logv("prefdisableinlandscape: %b", isPrefDisableInLandscape);
        return isPrefDisableInLandscape;
    }

    //return milliseconds
    public static int getPrefTimeoutLock(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int i  = Integer.parseInt(sp.getString(PREF_TIMEOUT_LOCK, "0"));
        CV.logv("prefTimeout lock: %d", i);
        return i;
    }

    public static int getPrefTimeoutUnlock(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int i  = Integer.parseInt(sp.getString(PREF_TIMEOUT_UNLOCK, "-1"));
        CV.logv("prefTimeout unlock: %d", i);

        // if the value is -1, means user want it to be the same as lock timeout value
        if(i==-1)
        {
            i = getPrefTimeoutLock(context);
        }
        return i;
    }

    public static boolean isPlugged(Context context){
        Intent intentBat = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return (intentBat.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) > 0);
    }
}
