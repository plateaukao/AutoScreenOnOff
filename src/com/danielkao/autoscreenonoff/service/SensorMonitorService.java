package com.danielkao.autoscreenonoff.service;

import android.app.AlarmManager;
import android.app.Notification;
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
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.OrientationEventListener;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.danielkao.autoscreenonoff.*;
import com.danielkao.autoscreenonoff.provider.ToggleAutoScreenOnOffAppWidgetProvider;
import com.danielkao.autoscreenonoff.receiver.TurnOffReceiver;
import com.danielkao.autoscreenonoff.ui.AutoScreenOnOffPreferenceActivity;
import com.danielkao.autoscreenonoff.ui.MainActivity;
import com.danielkao.autoscreenonoff.ui.TimePreference;
import com.danielkao.autoscreenonoff.util.CV;

import java.lang.reflect.Method;
import java.util.Calendar;

public class SensorMonitorService extends Service implements
		SensorEventListener {
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	private SensorManager mSensorManager;
	private PowerManager mPowerManager;
	private Sensor mProximity;
    OrientationEventListener mOrientationListener;

    // schedule
    AlarmManager am;
    private AlarmManager getAlarmManager(){
        if(am==null)
        {
            am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        return am;
    }

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

    // swipe counter
    private float currentSensorValue;
    private long tsLastChange;
    private int swipeCount=0;
    private void resetSwipeCount(){
        swipeCount = 0;
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

                String servicetype = intent.getStringExtra(CV.SERVICETYPE);
                // it's from widget or notification, need to do the toggle first
                if(servicetype!=null && !servicetype.equals(CV.SERVICETYPE_SETTING)){
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
                if(!CV.getPrefAutoOnoff(this)){
                    updateWidgetCharging(false);
                    updateNotification();
                }
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
                boolean bSleepModeStart = intent.getBooleanExtra(CV.SLEEP_MODE_START, false);
                CV.logv("Sleep Mode:%b",bSleepModeStart);

                //if(CV.isInSleepTime(this)){
                if(bSleepModeStart){
                    CV.logi("sleep mode starts: turn off sensor");
                    unregisterSensor();
                }
                else{
                    CV.logi("sleep mode stops: turn on sensor");
                    registerSensor();
                }

                break;
            }
            case CV.SERVICEACTION_PARTIALLOCK_TOGGLE:
            {
                // no need to use partial lock
                if(CV.getPrefNoPartialLock(this)  && partialLock.isHeld()){
                    partialLock.release();
                // need partial lock. make sure the sensor is registered.
                }else if (!CV.getPrefNoPartialLock(this) && isRegistered()){
                    partialLock.acquire();
                }

                break;
            }
            case CV.SERVICEACTION_SET_SCHEDULE:
            {
                setSchedule();
                break;
            }
            case CV.SERVICEACTION_CANCEL_SCHEDULE:
            {
                cancelSchedule();
                break;
            }
            default:
                CV.logi("onStartCommand: others");
        }

		return START_STICKY;
	}

    /**
     * send broadcast to update appWidget UI
     * @param b whether the Charging Icon should be shown
     */
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
				"autoscreenonoff partiallock");
		screenLock = mPowerManager.newWakeLock(
				PowerManager.ACQUIRE_CAUSES_WAKEUP
						| PowerManager.FULL_WAKE_LOCK
						| PowerManager.ON_AFTER_RELEASE, "autoscreenonoff fulllock");

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
		public SensorMonitorService getService() {
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

        // partial lock exists, and need partial lock
		if (partialLock != null && !CV.getPrefNoPartialLock(this))
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
		CV.logv("onAccuracyChanged:%d", accuracy);
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if(type == Sensor.TYPE_PROXIMITY){
            float lux = event.values[0];

            // calculate swipe count
            long tsCurrent = System.currentTimeMillis();
            if(lux != currentSensorValue){
                currentSensorValue = lux;
                if(tsCurrent - tsLastChange < 2000){
                    swipeCount +=1;
                } else{
                    swipeCount = 1;
                    tsLastChange = tsCurrent;
                }

            }
            CV.logv("log swipe count:%d", swipeCount);

            // Do something with this sensor value.
            CV.logv("onSensorChanged proximity:%f", lux);
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
                            if(timeout == 0)
                                turnOff();
                            else if(timeout == 2){
                                if(swipeCount >=4){
                                    resetSwipeCount();
                                    turnOff();
                                }
                            }
                            else if(timeout == 10){
                                // never: do nothing
                                return;
                            } else
                                handler.postDelayed(runnableTurnOff, timeout);
                        }
                    }
                }
                // should turn on
                else {
                    if (!mPowerManager.isScreenOn()) {
                        long timeout = (long) CV.getPrefTimeoutUnlock(this);
                        if(timeout==0)
                            turnOn();
                        else if(timeout == 2){
                            if(swipeCount >=4){
                                resetSwipeCount();
                                turnOn();
                            }
                        }
                        else if(timeout == 10){
                            // never: do nothing
                            return;
                        } else
                            handler.postDelayed(runnableTurnOn, timeout);
                    }
                }
            }
        }
	}

	private void togglePreference() {
        CV.logv("togglePreference");
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean IsAutoOn = sp.getBoolean(CV.PREF_AUTO_ON, false);
		Editor editor = sp.edit();
		editor.putBoolean(CV.PREF_AUTO_ON, !IsAutoOn);
        // if original value is false, it's meant to turn on pref, then we should make sure which-charging is off
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

        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
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

    private void turnOn(){
        if (!screenLock.isHeld()) {
            screenLock.acquire();
                /*
                KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                KeyguardManager.KeyguardLock mLock = mKeyGuardManager.newKeyguardLock("com.danielkao.autoscreenonoff");
                if(mKeyGuardManager.isKeyguardLocked())
                    mLock.disableKeyguard();
                mLock.reenableKeyguard();
                */
            new Thread(new Runnable() {
                public void run() {
                    try {
                        //Thread.sleep(1000);
                        // try to fix phonepad and galaxy note's issue
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(screenLock.isHeld())
                        screenLock.release();
                }
            }).start();
        }
    }

    private void turnOff(){
        CV.logv("sensor: turn off thread");
        if(screenLock.isHeld())
            screenLock.release();
        deviceManager.lockNow();
        playCloseSound();

    }


    private Runnable runnableTurnOff = new Runnable() {
        @Override
        public void run() {
            turnOff();
            resetHandler();
        }
    };

    private Runnable runnableTurnOn = new Runnable() {
        @Override
        public void run() {
            CV.logi("sensor: turn on thread");
            turnOn();
            resetHandler();
        }
    };
    //</editor-fold>

    /**
     * create notification for bringing service to foreground, also for update notification info
     * @return a notification for showing on notificaion panel
     */
    private Notification createNotification(){
        // common part
        Intent intentOnOff = new Intent(CV.SERVICE_INTENT_ACTION);
        intentOnOff.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_TOGGLE);
        intentOnOff.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_NOTIFICATION);
        PendingIntent piOnOff = PendingIntent.getService(this, 0, intentOnOff, 0);

        boolean bStatusOn = false;
        if(CV.getPrefAutoOnoff(this)
                || (CV.getPrefChargingOn(this)&&CV.isPlugged(this)))
            bStatusOn = true;

        String ticker;
        if(CV.getPrefChargingOn(this)&&CV.isPlugged(this)){
            ticker = getString(R.string.statusbar_charging);
        }else{
            ticker = getString((CV.getPrefAutoOnoff(this))?R.string.statusbar_autoscreen_on:R.string.statusbar_autoscreen_off);
        }

        // for version > 2.3.x
        if (Build.VERSION.SDK_INT > 10) {
            // setup pending intents
            Intent intentApp = new Intent(this,AutoScreenOnOffPreferenceActivity.class);
            intentApp.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent piApp = PendingIntent.getActivity(this, 0, intentApp, 0);

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

            // build the notification
        /*
        Notification noti = new Notification.Builder(this)
                .setContent(remoteViews)
                .setTicker(ticker)
                .setSmallIcon((bStatusOn)?R.drawable.statusbar_on:R.drawable.statusbar_off)
                .setOngoing(true)
                .build();
                */
            Notification noti = new Notification(
                    (bStatusOn)?R.drawable.statusbar_on:R.drawable.statusbar_off,
                    ticker,
                    System.currentTimeMillis());
            noti.contentView = remoteViews;
            noti.flags |= Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;

            return noti;
        }else{
            Notification noti = new Notification(
                    (bStatusOn)?R.drawable.statusbar_on:R.drawable.statusbar_off,
                    ticker,
                    System.currentTimeMillis());
            noti.flags |= Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
            noti.setLatestEventInfo(this, ticker,null,piOnOff);
            return noti;
        }
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

        // hack
        try{
            Object service  = getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");

            if (Build.VERSION.SDK_INT <= 16) {
                Method collapse = statusbarManager.getMethod("collapse");
                collapse.setAccessible(true);
                collapse.invoke(service);
            } else {
                Method collapse2 = statusbarManager.getMethod("collapsePanels");
                collapse2.setAccessible(true);
                collapse2.invoke(service);
            }
            //collapse.setAccessible(true);
        }catch(Exception ex){}

        Notification notify = createNotification();
        startForeground(NOTIFICATION_ONGOING, notify);
    }

    //-- for alarm settings
    private void setSchedule() {
        cancelSchedule();
        //alarm: sleep start
        int hour = TimePreference.getHour(CV.getPrefSleepStart(this));
        int minute = TimePreference.getMinute(CV.getPrefSleepStart(this));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this, SensorMonitorService.class);
        intent.setData(Uri.parse("timer://1")); // identifier for this alarm
        intent.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_MODE_SLEEP);
        intent.putExtra(CV.SLEEP_MODE_START,true);
        PendingIntent pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP
                ,calendar.getTimeInMillis()
                ,AlarmManager.INTERVAL_DAY, pi);
        //alarm: sleep stop
        hour = TimePreference.getHour(CV.getPrefSleepStop(this));
        minute = TimePreference.getMinute(CV.getPrefSleepStop(this));

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        intent = new Intent(this, SensorMonitorService.class);
        intent.setData(Uri.parse("timer://2"));
        intent.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_MODE_SLEEP);
        intent.putExtra(CV.SLEEP_MODE_START,false);
        pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP
                ,calendar.getTimeInMillis()
                ,AlarmManager.INTERVAL_DAY, pi);
    }

    private void cancelSchedule() {
        Intent intent = new Intent(this, SensorMonitorService.class);
        intent.setData(Uri.parse("timer://1"));
        PendingIntent pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        getAlarmManager().cancel(pi);

        intent.setData(Uri.parse("timer://2"));
        pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        getAlarmManager().cancel(pi);
    }

    private void playCloseSound(){
        if(CV.getPrefPlayCloseSound(this)){
            AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
            float vol = 1.0f;
            am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol);
        }
    }
}
