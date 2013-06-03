package com.danielkao.autoscreenonoff;

import android.annotation.SuppressLint;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.OrientationEventListener;
import android.widget.Toast;

public class SensorMonitorService extends Service implements
		SensorEventListener {
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	private SensorManager mSensorManager;
	private PowerManager mPowerManager;
	private Sensor mProximity;
    OrientationEventListener mOrientationListener;

	private boolean mIsRegistered;

	private WakeLock partialLock, screenLock;

	DevicePolicyManager deviceManager;
	ComponentName mDeviceAdmin;

    private int mRotationAngle = 360;

    //handle timeout function
    private int CALLBACK_EXISTS=0;
    //private Timer timer;
    private Handler handler = new Handler();

	private boolean isActiveAdmin() {
		return deviceManager.isAdminActive(mDeviceAdmin);
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ConstantValues.logi("onStartCommand");
        // being restarted
        if (intent == null) {
            ConstantValues.logi("onStartCommand: no intent");
            if (ConstantValues.getPrefAutoOnoff(this) == false) {
                unregisterSensor();
            } else {
                registerSensor();
            }

            return START_STICKY;
        }

        int action = intent.getIntExtra(ConstantValues.SERVICEACTION, -1);

        switch(action){
            // from widget or setting
            case ConstantValues.SERVICEACTION_TOGGLE:
            {
                ConstantValues.logi("onStartCommand: toggle");

                // it's from widget, need to do the toggle first
                if(!intent.getStringExtra(ConstantValues.SERVICETYPE).equals(ConstantValues.SERVICETYPE_SETTING)){
                    togglePreference();
                }

                updateWidgetCharging(false);

                if (ConstantValues.getPrefAutoOnoff(this) == false) {
                    unregisterSensor();
                } else {
                    registerSensor();
                }
                break;
            }
            case ConstantValues.SERVICEACTION_TURNON:
            {
                ConstantValues.logi("onStartCommand: turnon");
                // from charging receiver
                if(!isRegistered()){
                    registerSensor();

                    updateWidgetCharging(ConstantValues.isPlugged(this));
                }
                break;
            }
            case ConstantValues.SERVICEACTION_TURNOFF:
            {
                ConstantValues.logi("onStartCommand: turnoff");
                // from charging receiver
                if(isRegistered())
                    unregisterSensor();
                if(!ConstantValues.getPrefAutoOnoff(this))
                    updateWidgetCharging(false);
                break;
            }
            case ConstantValues.SERVICEACTION_UPDATE_DISABLE_IN_LANDSCAPE:
            {
                //if(ConstantValues.getPrefAutoOnoff(this) ||
                //        (ConstantValues.getPrefChargingOn(this)&& isPlugged())){
                if(mIsRegistered){
                    if(ConstantValues.getPrefDisableInLandscape(this) == true)
                        registerOrientationChange();
                    else
                        unregisterOrientationChange();
                }
                break;
            }
            default:
                ConstantValues.logi("onStartCommand: others");
        }

		return START_STICKY;
	}

    private void updateWidgetCharging(boolean b) {
        Intent i = new Intent(this, ToggleAutoScreenOnOffAppWidgetProvider.class);
        i.setAction(ConstantValues.UPDATE_WIDGET_ACTION);
        i.putExtra(ConstantValues.PREF_CHARGING_ON, b);
        this.sendBroadcast(i);
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

		mPowerManager = ((PowerManager) getSystemService(POWER_SERVICE));
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		partialLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"TAG");
		screenLock = mPowerManager.newWakeLock(
				PowerManager.ACQUIRE_CAUSES_WAKEUP
						| PowerManager.FULL_WAKE_LOCK
						| PowerManager.ON_AFTER_RELEASE, "TAG");
	}

	@Override
	public void onDestroy() {
		ConstantValues.logi("onDestroy");
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
	public void registerSensor() {
		ConstantValues.logi("registerSensor");
		if (mIsRegistered) {
			Toast.makeText(SensorMonitorService.this,
					"Auto Screen On/off is already turned on",
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		// grant device management
		if(!isActiveAdmin()){
			Intent i = new Intent(this, MainActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(i);
		}

		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        // listen to orientation change
        if(ConstantValues.getPrefDisableInLandscape(getBaseContext())){
            registerOrientationChange();
        }

		mIsRegistered = true;

		if (partialLock != null)
			partialLock.acquire();

        // show hint text if the screen is on
        if (mPowerManager.isScreenOn()) {
            String s = getString(R.string.turn_autoscreen_on);
            Toast.makeText(SensorMonitorService.this, s, Toast.LENGTH_SHORT).show();
        }
	}

	public void unregisterSensor() {
		ConstantValues.logi("unregisterSensor");
		if (mIsRegistered) {
			mSensorManager.unregisterListener(this);
			String s = getString(R.string.turn_autoscreen_off);
			Toast.makeText(SensorMonitorService.this, s, Toast.LENGTH_SHORT).show();
		}

		if (partialLock != null && partialLock.isHeld())
			partialLock.release();
		mIsRegistered = false;

		stopSelf();
	}

	public boolean isRegistered() {
		return mIsRegistered;
	}

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
        int type = event.sensor.getType();
        if(type == Sensor.TYPE_PROXIMITY){
            float lux = event.values[0];

            // Do something with this sensor value.
            ConstantValues.logv("onSensorChanged:%f", lux);
            if (isActiveAdmin()) {
                // reset handler if there's already one
                if(handler.hasMessages(CALLBACK_EXISTS)){
                    ConstantValues.logv("timer is on; exit");
                    resetHandler();
                    return;
                }

                // value == 0; should turn screen off
                if (lux == 0f) {
                    if (mPowerManager.isScreenOn()) {
                        // check if it is disabled during landscape mode, and now it's really in landscape
                        // --> return
                        if(ConstantValues.getPrefDisableInLandscape(this) && isOrientationLandscape()){
                            return;
                        }
                        else{
                            long timeout = (long)ConstantValues.getPrefTimeoutLock(this);
                            handler.postDelayed(runnableTurnOff,timeout);
                        }
                    }
                }
                // should turn on
                else {
                    if (!mPowerManager.isScreenOn()) {
                        long timeout = (long)ConstantValues.getPrefTimeoutUnlock(this);
                        handler.postDelayed(runnableTurnOn,timeout);
                    }
                }
            }
        }
	}

	private void togglePreference() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean IsAutoOn = sp.getBoolean(ConstantValues.PREF_AUTO_ON, false);
		Editor editor = sp.edit();
		editor.putBoolean(ConstantValues.PREF_AUTO_ON, !IsAutoOn);
		editor.commit();

	}

    private boolean isOrientationLandscape(){
        if(((mRotationAngle > 90 - ConstantValues.ROTATION_THRESHOLD) && (mRotationAngle < 90 + ConstantValues.ROTATION_THRESHOLD))
        || ((mRotationAngle > 270 - ConstantValues.ROTATION_THRESHOLD) && (mRotationAngle < 270 + ConstantValues.ROTATION_THRESHOLD))){
            return true;
        }
        else return false;
    }

    private void registerOrientationChange(){
        if(null != mOrientationListener && mOrientationListener.canDetectOrientation())
            return;

        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            public void onOrientationChanged (int orientation) {
                mRotationAngle = orientation;
                //ConstantValues.logv("onOrientationChanged:%d",orientation);
            }
        };
        mOrientationListener.enable ();
    }

    private void unregisterOrientationChange(){
        if(null != mOrientationListener){
            mOrientationListener.disable();
            mOrientationListener = null;
        }
    }

    private void resetHandler(){
        ConstantValues.logv("reset Handler");
        handler.removeMessages(CALLBACK_EXISTS);
        handler.removeCallbacks(runnableTurnOn);
        handler.removeCallbacks(runnableTurnOff);
    }

    private Runnable runnableTurnOff = new Runnable() {
        @Override
        public void run() {
            ConstantValues.logv("sensor: turn off thread");
            deviceManager.lockNow();
            resetHandler();
        }
    };

    //timeout
    private Runnable runnableTurnOn = new Runnable() {
        @Override
        public void run() {
            ConstantValues.logi("sensor: turn on thread");
            if (!screenLock.isHeld()) {
                screenLock.acquire();
                resetHandler();

                // screenLock.release();
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
    };
}
