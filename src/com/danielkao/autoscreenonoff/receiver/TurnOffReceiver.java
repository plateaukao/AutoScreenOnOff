package com.danielkao.autoscreenonoff.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class TurnOffReceiver extends DeviceAdminReceiver {
	private static final String TAG = "TurnOffReceiver";
	  public CharSequence onDisableRequested(Context paramContext, Intent paramIntent)
	  {
	    return "This is an optional message to warn the user about disabling.";
	  }

	  public void onDisabled(Context paramContext, Intent paramIntent)
	  {
		  Log.v(TAG,"onDisabled");
	  }

	  public void onEnabled(Context paramContext, Intent paramIntent)
	  {
		  Log.v(TAG,"onEnabled");
	  }

	  public void onPasswordChanged(Context paramContext, Intent paramIntent)
	  {
	  }

}
