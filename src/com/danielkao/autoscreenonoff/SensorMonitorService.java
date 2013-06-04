package com.danielkao.autoscreenonoff;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
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
        CV.logi("onStartCommand");
        // being restarted
        if (intent == null) {
            CV.logi("onStartCommand: no intent");
            // start monitoring when
            // 1. autoOn is on
            // 2. charging is on and is plugged in
            if (CV.getPrefAutoOnoff(this)){
                registerSensor();
            }else if(CV.getPrefChargingOn(this)&&CV.isPlugged(this)){
                registerSensor();
            }

            return START_STICKY;
        }

        int action = intent.getIntExtra(CV.SERVICEACTION, -1);

        switch(action){
            // from widget or setting
            case CV.SERVICEACTION_TOGGLE:
            {
                CV.logi("onStartCommand: toggle");

                // it's from widget, need to do the toggle first
                if(!intent.getStringExtra(CV.SERVICETYPE).equals(CV.SERVICETYPE_SETTING)){
                    // in charging state and pref charging on is turned on
                    if(CV.isPlugged(this)&&CV.getPrefChargingOn(this)){
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                        Editor editor = sp.edit();
                        editor.putBoolean(CV.PREF_CHARGING_ON, false);
                        editor.commit();
                    }else{
                        togglePreference();
                    }
                }

                updateWidgetCharging(false);

                if (CV.getPrefAutoOnoff(this) == false) {
                    unregisterSensor();
                } else {
                    registerSensor();
                }
                break;
            }
            case CV.SERVICEACTION_TURNON:
            {
                CV.logi("onStartCommand: turnon");
                // from charging receiver
                if(!isRegistered()){
                    registerSensor();

                    updateWidgetCharging(CV.isPlugged(this));
                }
                break;
            }
            case CV.SERVICEACTION_TURNOFF:
            {
                CV.logi("onStartCommand: turnoff");
                // from charging receiver
                if(isRegistered())
                    unregisterSensor();
                if(!CV.getPrefAutoOnoff(this))
                    updateWidgetCharging(false);
                break;
            }
            case CV.SERVICEACTION_UPDATE_DISABLE_IN_LANDSCAPE:
            {
                //if(CV.getPrefAutoOnoff(this) ||
                //        (CV.getPrefChargingOn(this)&& isPlugged())){
                if(mIsRegistered){
                    if(CV.getPrefDisableInLandscape(this) == true)
                        registerOrientationChange();
                    else
                        unregisterOrientationChange();
                }
                break;
            }
            case CV.SERVICEACTION_MODE_SLEEP:
            {
                if(CV.getPrefAutoOnoff(this)==false)
                    return START_STICKY;

                CV.logv("service:mode sleep action");
                CV.logv("Sleep Mode:%b",intent.getBooleanExtra(CV.SLEEP_MODE_START,false));
                boolean bSleepModeStart = intent.getBooleanExtra(CV.SLEEP_MODE_START, false);

                if(CV.isInSleepTime(this)){
                    unregisterSensor();
                }
                else{
                    registerSensor();
                }

                break;
            }
            default:
                CV.logi("onStartCommand: others");
        }

		return START_STICKY;
	}

    private void updateWidgetCharging(boolean b) {
        Intent i = new Intent(this, ToggleAutoScreenOnOffAppWidgetProvider.class);
        i.setAction(CV.UPDATE_WIDGET_ACTION);
        i.putExtra(CV.PREF_CHARGING_ON, b);
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
		CV.logi("onDestroy");
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
		CV.logi("registerSensor");
		if (mIsRegistered) {
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
        if(CV.getPrefDisableInLandscape(getBaseContext())){
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
		CV.logi("unregisterSensor");
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
		CV.logv("onAccuracyChanged");
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
            CV.logv("onSensorChanged:%f", lux);
            if (isActiveAdmin()) {
                // reset handler if there's already one
                if(handler.hasMessages(CALLBACK_EXISTS)){
                    CV.logv("timer is on; exit");
                    resetHandler();
                    return;
                }

                // value == 0; should turn screen off
                if (lux == 0f) {
                    if (mPowerManager.isScreenOn()) {
                        // check if it is disabled during landscape mode, and now it's really in landscape
                        // --> return
                        if(CV.getPrefDisableInLandscape(this) && isOrientationLandscape()){
                            return;
                        }
                        else{
                            long timeout = (long) CV.getPrefTimeoutLock(this);
                            handler.postDelayed(runnableTurnOff,timeout);
                        }
                    }
                }
                // should turn on
                else {
                    if (!mPowerManager.isScreenOn()) {
                        long timeout = (long) CV.getPrefTimeoutUnlock(this);
                        handler.postDelayed(runnableTurnOn,timeout);
                    }
                }
            }
        }
	}

	private void togglePreference() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean IsAutoOn = sp.getBoolean(CV.PREF_AUTO_ON, false);
		Editor editor = sp.edit();
		editor.putBoolean(CV.PREF_AUTO_ON, !IsAutoOn);
        // if it's meant to turn on pref, then we should make sure which-charging is off
        if(!IsAutoOn)
            editor.putBoolean(CV.PREF_CHARGING_ON, false);
		editor.commit();

	}

    private boolean isOrientationLandscape(){
        if(((mRotationAngle > 90 - CV.ROTATION_THRESHOLD) && (mRotationAngle < 90 + CV.ROTATION_THRESHOLD))
        || ((mRotationAngle > 270 - CV.ROTATION_THRESHOLD) && (mRotationAngle < 270 + CV.ROTATION_THRESHOLD))){
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
                //CV.logv("onOrientationChanged:%d",orientation);
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
        CV.logv("reset Handler");
        handler.removeMessages(CALLBACK_EXISTS);
        handler.removeCallbacks(runnableTurnOn);
        handler.removeCallbacks(runnableTurnOff);
    }

    private Runnable runnableTurnOff = new Runnable() {
        @Override
        public void run() {
            CV.logv("sensor: turn off thread");
            deviceManager.lockNow();
            resetHandler();
        }
    };

    //timeout
    private Runnable runnableTurnOn = new Runnable() {
        @Override
        public void run() {
            CV.logi("sensor: turn on thread");
            if (!screenLock.isHeld()) {
                screenLock.acquire();
                /*
                KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                KeyguardManager.KeyguardLock mLock = mKeyGuardManager.newKeyguardLock("com.danielkao.autoscreenonoff");
                if(mKeyGuardManager.isKeyguardLocked())
                    mLock.disableKeyguard();
                mLock.reenableKeyguard();
                */
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
