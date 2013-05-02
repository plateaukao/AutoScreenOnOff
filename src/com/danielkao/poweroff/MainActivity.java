package com.danielkao.poweroff;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String TAG = "TurnOff";
    
    DevicePolicyManager deviceManager;
    ComponentName mDeviceAdmin;
    
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


		deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, TurnOffReceiver.class);

		// check if activated. if not, send the intent
		if(!isActiveAdmin())
			sendDeviceAdminIntent();
		else{
			shutdown();
			finish();
			return;
		}
		
		setContentView(R.layout.activity_main);
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
    	Handler handlerUI = new Handler();
    	handlerUI.postDelayed(new Runnable() {
    		@Override
    		public void run() {
    			deviceManager.lockNow();
    		}
    	}, 500);
    }
        
}
