package com.danielkao.poweroff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

public class SensorMonitorService extends Service  implements SensorEventListener {
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private Sensor mProximity;
    
    private boolean mIsRegistered;
    
    private WakeLock partialLock,screenLock;
    
    DevicePolicyManager deviceManager;
    ComponentName mDeviceAdmin;
    
    private boolean isActiveAdmin() {
        return deviceManager.isAdminActive(mDeviceAdmin);
    }
    
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	if(intent==null)
    		return super.onStartCommand(intent, flags, startId);
    	
    	int action = intent.getIntExtra(ConstantValues.SERVICEACTION, -1);
    	if(action == ConstantValues.SERVICEACTION_TOGGLE)
    	{
    		// do the toggle first
    		togglePreference();
    		
    		if(mIsRegistered)
    		{
    			unregisterSensor();
    		}
    		else
    		{
    			registerSensor();
    		}
    	}
		return super.onStartCommand(intent, flags, startId);
	}

	//
    // life cycle
    //
	public SensorMonitorService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();


		deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, TurnOffReceiver.class);

		mPowerManager = ((PowerManager)getSystemService(POWER_SERVICE));
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		partialLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TAG");
		screenLock = mPowerManager.newWakeLock(
				PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "TAG");
	}

	@Override
	public void onDestroy() {
		unregisterSensor();
		super.onDestroy();
	}

	// to return service class
    public class LocalBinder extends Binder {
        SensorMonitorService getService() {
            return SensorMonitorService.this;
        }
    }
    
    //
    // pubilc API for client
    //
    public void registerSensor(){
    	ConstantValues.logv("registerSensor");
    	if(mIsRegistered){
    		Toast.makeText(SensorMonitorService.this, "Auto Screen On/off is already turned on", Toast.LENGTH_LONG ).show();
    		return;
    	}

    	mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    	mIsRegistered = true;
    	
    	if(partialLock!=null)
    		partialLock.acquire();

    	Toast.makeText(SensorMonitorService.this, "Turn on Auto Screen On/off", Toast.LENGTH_LONG ).show();
    }
    
    public void unregisterSensor(){
    	ConstantValues.logv("unregisterSensor");
        if(mIsRegistered){
        	mSensorManager.unregisterListener(this);
        	Toast.makeText(SensorMonitorService.this, "Turn off Auto Screen On/off", Toast.LENGTH_LONG ).show();
        }
        
        if(partialLock!=null) partialLock.release();
    	mIsRegistered = false;
    	
    }
    
    public boolean isRegistered(){ return mIsRegistered; }
    
    //
    // listener
    //
    
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    	ConstantValues.logv("onAccuracyChanged");
    }

    @SuppressLint("Wakelock")
	@Override
    public final void onSensorChanged(SensorEvent event) {
      // The light sensor returns a single value.
      // Many sensors return 3 values, one for each axis.
      float lux = event.values[0];
      // Do something with this sensor value.
      ConstantValues.logv("onSensorChanged:%f",lux);
      if(isActiveAdmin())
      {
    	  // should turn off
    	  if(lux == 0f) {
    		  if(mPowerManager.isScreenOn()){
    			  deviceManager.lockNow();
    			  ConstantValues.logv("turn off");
    		  }
    	  }
    	  // should turn on
    	  else
    	  {
    		  if(!mPowerManager.isScreenOn())
    		  {
    			  ConstantValues.logv("turn on");
    			  if(!screenLock.isHeld())
    			  {
    				  screenLock.acquire();
    				  
    				  new Thread(new Runnable() {
    					  public void run() {
    						  try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
    						  screenLock.release();
    					  }
    				  }).start();
    			  }
    		  }
    	  }
      }
    }
    
    private void togglePreference(){
    	SharedPreferences sp = getSharedPreferences(ConstantValues.PREF, Activity.MODE_PRIVATE);
    	boolean IsAutoOn = sp.getBoolean(ConstantValues.IS_AUTO_ON, false);
    	Editor editor = sp.edit();
    	editor.putBoolean(ConstantValues.IS_AUTO_ON, !IsAutoOn);
    	editor.commit();
    }
}
