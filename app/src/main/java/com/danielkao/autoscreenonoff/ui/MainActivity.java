package com.danielkao.autoscreenonoff.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.danielkao.autoscreenonoff.R;
import com.danielkao.autoscreenonoff.receiver.TurnOffReceiver;
import com.danielkao.autoscreenonoff.util.CV;

/**
 * Invisible trampoline whose only job is requesting device-admin activation;
 * with the CLOSE_AFTER extra it also locks the screen once granted.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String TAG = "TurnOff";

    private DevicePolicyManager deviceManager;
    private ComponentName mDeviceAdmin;

    // check if intent is from screenOff request
    private boolean bCloseAfter = false;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (REQUEST_CODE_ENABLE_ADMIN == requestCode)
		{
			if (resultCode == Activity.RESULT_OK) {
                Log.v(TAG, "add device admin okay!!");
                if(bCloseAfter){
                    shutdown();
                    bCloseAfter=false;
                }
                // let the service pick the sensor up now that locking works
                if(CV.getPrefAutoOnoff(this) || CV.getPrefChargingOn(this)){
                    Intent i = CV.serviceIntent(this, CV.SERVICEACTION_TOGGLE);
                    i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_SETTING);
                    CV.startService(this, i);
                }
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

		deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, TurnOffReceiver.class);

        // get value from intent
        Intent i = getIntent();
        if(null != i){
            bCloseAfter = i.getBooleanExtra(CV.CLOSE_AFTER,false);
        }

		// handle activeAdmin privilege
		if(!isActiveAdmin())
		{
			sendDeviceAdminIntent();
			return;
		}

        // already an admin: nothing to ask
        if(bCloseAfter)
            shutdown();
        finish();
	}

    private boolean isActiveAdmin() {
        return deviceManager.isAdminActive(mDeviceAdmin);
    }

    private void sendDeviceAdminIntent(){
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getResources().getString(R.string.device_management_explanation));
		startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    private void shutdown(){
    	deviceManager.lockNow();
    }
}
