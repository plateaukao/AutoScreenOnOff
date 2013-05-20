package com.danielkao.autoscreenonoff;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

public final class ConstantValues {
	static final boolean debug = true;
	
	public static final String TAG = "SensorMonitor";
	public static final String PREF = "SensorMonitorPref";
	public static final String IS_AUTO_ON = "is_audo_on";
	public static final int SERVICEACTION_TOGGLE = 0;
    public static final int SERVICEACTION_TURNON = 1;
    public static final int SERVICEACTION_TURNOFF = 2;
	public static final String SERVICEACTION = "serviceaction";
	public static final String SERVICE_INTENT_ACTION = "com.danielkao.autoscreenonoff.serviceaction";
	
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
}
