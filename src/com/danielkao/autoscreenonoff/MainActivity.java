package com.danielkao.autoscreenonoff;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.danielkao.autoscreenonoff.SensorMonitorService.LocalBinder;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String TAG = "TurnOff";
    
    private DevicePolicyManager deviceManager;
    private ComponentName mDeviceAdmin;
    
    //service
    private SensorMonitorService sensorService;
    // pref: turn on auto on/off
    private boolean mIsAutoOn;
    
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (REQUEST_CODE_ENABLE_ADMIN == requestCode)
		{
			if (resultCode == Activity.RESULT_OK) {
				// Has become the device administrator.
				if(false == mIsAutoOn){
					shutdown();
				}
				Log.v(TAG, "add device admin okay!!");
			} else {
				//Canceled or failed: turn off Enabler
				Log.v(TAG, "add device admin not okay");

			}
			finish();
		}
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//get pref
		mIsAutoOn = CV.getPrefAutoOnoff(this.getApplicationContext());

		deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, TurnOffReceiver.class);

		// handle activeAdmin previlige
		if(!isActiveAdmin())
		{
			sendDeviceAdminIntent();
			return;
		}
		
	}


	@Override
	protected void onStop() {
		super.onStop();
		if(sensorService != null)
			unbindService(mConnection);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

    private boolean isActiveAdmin() {
        return deviceManager.isAdminActive(mDeviceAdmin);
    }
    
    private void sendDeviceAdminIntent(){
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getResources().getString(R.string.device_management_explanation));
        //"Need this privilege to turn off the screen");
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
        }
    };
}
