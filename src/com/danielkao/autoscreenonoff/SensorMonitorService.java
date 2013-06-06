package com.danielkao.autoscreenonoff;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.widget.RemoteViews;
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

    // for notification logic
    private boolean bForeground = false;
    private int NOTIFICATION_ONGOING = 12345;
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
                // before registering, need to check whether it's in sleeping time period
                // if so, do nothing
                if(CV.getPrefSleeping(this) && CV.isInSleepTime(this))
                    return START_NOT_STICKY;
                registerSensor();
            }else if(CV.getPrefChargingOn(this)&&CV.isPlugged(this)){
                registerSensor();
            }

            return START_STICKY;
        }

        int action = intent.getIntExtra(CV.SERVICEACTION, -1);

        switch(action){
            case CV.SERVICEACTION_SHOW_NOTIFICATION:
            {
                if(CV.getPrefShowNotification(this))
                    showNotification();
                else
                    hideNotification();

                return START_NOT_STICKY;
            }
            case CV.SERVICEACTION_SCREENOFF:
            {
                CV.logi("onStartCommand: screenoff");
                // grant device management
                if(!isActiveAdmin()){
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra(CV.CLOSE_AFTER,true);
                    this.startActivity(i);
                }
                else{
                    deviceManager.lockNow();
                }
                return START_NOT_STICKY;
            }
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
                updateNotification();

                if (CV.getPrefAutoOnoff(this) == false) {
                    unregisterSensor();
                } else {
                    // before registering, need to check whether it's in sleeping time period
                    // if so, do nothing
                    if(CV.getPrefSleeping(this) && CV.isInSleepTime(this))
                        return START_NOT_STICKY;
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
                    updateNotification();
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
                return START_NOT_STICKY;
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

        // show notification if it's set
        if(CV.getPrefShowNotification(this))
            showNotification();
	}

	@Override
	public void onDestroy() {
		CV.logi("onDestroy");
        if(mIsRegistered)
		    unregisterSensor();
		super.onDestroy();
	}

	// to return service class
	public class LocalBinder extends Binder {
		SensorMonitorService getService() {
			return SensorMonitorService.this;
		}
	}

    //<editor-fold desc="sensor registration">
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
        if (mPowerManager.isScreenOn() && !bForeground) {
            String s = getString(R.string.turn_autoscreen_on);
            Toast.makeText(SensorMonitorService.this, s, Toast.LENGTH_SHORT).show();
        }
	}

	public void unregisterSensor() {
		CV.logi("unregisterSensor");
		if (mIsRegistered) {
			mSensorManager.unregisterListener(this);
            if(!bForeground)
            {
                String s = getString(R.string.turn_autoscreen_off);
			    Toast.makeText(SensorMonitorService.this, s, Toast.LENGTH_SHORT).show();
            }
		}

		if (partialLock != null && partialLock.isHeld())
			partialLock.release();
		mIsRegistered = false;

        // do not close service if the notification is shown
        if(!bForeground)
		    stopSelf();
	}

	public boolean isRegistered() {
		return mIsRegistered;
	}
    //</editor-fold>

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

    //<editor-fold desc="orientation registration">
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
    //</editor-fold>

    //<editor-fold desc="time out handler">
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
    //</editor-fold>

    private Notification createNotification(){
        // setup pending intents
        Intent intentApp = new Intent(this,ScreenOffWidgetConfigure.class);
        intentApp.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piApp = PendingIntent.getActivity(this, 0, intentApp, 0);

        Intent intentOnOff = new Intent(CV.SERVICE_INTENT_ACTION);
        intentOnOff.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_TOGGLE);
        PendingIntent piOnOff = PendingIntent.getService(this, 0, intentOnOff, 0);

        Intent intentScreenOff = new Intent(CV.SERVICE_INTENT_ACTION);
        intentScreenOff.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_SCREENOFF);
        PendingIntent piScreenOff = PendingIntent.getService(this, 1, intentScreenOff, 0);

        // setup remoteview
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification);
        remoteViews.setOnClickPendingIntent(R.id.image_logo, piApp);
        remoteViews.setOnClickPendingIntent(R.id.image_status, piOnOff);
        remoteViews.setOnClickPendingIntent(R.id.image_screenoff, piScreenOff);

        if(CV.getPrefChargingOn(this) && CV.isPlugged(this)){
            remoteViews.setImageViewResource(R.id.image_status,R.drawable.widget_charging_on);
        }else{
            if(CV.getPrefAutoOnoff(this))
                remoteViews.setImageViewResource(R.id.image_status,R.drawable.widget_on);
            else
                remoteViews.setImageViewResource(R.id.image_status,R.drawable.widget_off);
        }

        String ticker;
        if(CV.getPrefChargingOn(this)&&CV.isPlugged(this)){
            ticker = "Charging!";
        }else{
            ticker = (CV.getPrefAutoOnoff(this))?"turn on":"turn off";

        }
        // build the notification
        Notification noti = new Notification.Builder(this)
                .setContent(remoteViews)
                .setTicker(ticker)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .build();
       return noti;
    }

    private void showNotification(){
        Notification notify = createNotification();

        startForeground(NOTIFICATION_ONGOING, notify);
        bForeground = true;
    }

    private void hideNotification(){
        bForeground = false;
        stopForeground(true);

    }

    private void updateNotification(){
        if(!bForeground)
            return;

        Notification notify = createNotification();
        final NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(getApplicationContext().NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ONGOING, notify);

    }
}
