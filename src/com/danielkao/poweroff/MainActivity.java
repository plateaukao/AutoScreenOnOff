package com.danielkao.poweroff;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;

import com.danielkao.poweroff.SensorMonitorService.LocalBinder;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String TAG = "TurnOff";
    
    DevicePolicyManager deviceManager;
    ComponentName mDeviceAdmin;
    
    //service
    SensorMonitorService sensorService;
    boolean mBoundLocalBindService;
    // pref: turn on auto on/off
    boolean mIsAutoOn;
    
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (REQUEST_CODE_ENABLE_ADMIN == requestCode)
		{
			if (resultCode == Activity.RESULT_OK) {
				// Has become the device administrator.
				shutdown();
				Log.v(TAG, "add device admin okay!!");
				finish();
			} else {
				//Canceled or failed.
				Log.v(TAG, "add device admin not okay");
			}
		}
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//get pref
		SharedPreferences sp = getSharedPreferences(ConstantValues.PREF, Activity.MODE_PRIVATE);
		mIsAutoOn = sp.getBoolean(ConstantValues.IS_AUTO_ON, false);
		ConstantValues.logv("%b",mIsAutoOn);

		deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, TurnOffReceiver.class);

		// check whether is from another activity
		Intent intentGot = this.getIntent();
		if(intentGot.getExtras() != null)
		{
			Intent i = new Intent(this, SensorMonitorService.class);
			bindService(i , mConnection, Context.BIND_AUTO_CREATE);
			// make it long live
			if(mIsAutoOn){
				this.startService(i);
				finish();
			}
			return;
		}

		// no intent passed from another activity
		if(mIsAutoOn){
			Intent intent = new Intent(this, SensorMonitorService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			this.startService(intent);
			finish();
		} // use manually
		else{
			// check if activated. if not, send the intent
			if(!isActiveAdmin())
				sendDeviceAdminIntent();
			else{
				shutdown();
				finish();
				return;
			}

		}

	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(sensorService != null)
			unbindService(mConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
    private boolean isActiveAdmin() {
        return deviceManager.isAdminActive(mDeviceAdmin);
    }
    
    private void sendDeviceAdminIntent(){
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
				"Need this privilege to turn off the screen");
		startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
		
    }
    
    private void shutdown(){
    	deviceManager.lockNow();
    }
    
    // service connection
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            sensorService = binder.getService();
            mBoundLocalBindService = true;
            if(mIsAutoOn){
            	sensorService.registerSensor();
            }
            else {
            	sensorService.unregisterSensor();
            	finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundLocalBindService = false;
        }
    };
}
