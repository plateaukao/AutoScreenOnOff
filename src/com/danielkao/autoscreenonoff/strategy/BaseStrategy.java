package com.danielkao.autoscreenonoff.strategy;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Handler;

/**
 * Created by plateau on 2013/09/12.
 */
public class BaseStrategy {

    protected Handler handler;

    protected WakeLock screenLock;
    protected DevicePolicyManager deviceManager;
    protected PowerManager mPowerManager;
    protected Context context;

    public BaseStrategy(Context context, WakeLock screenLock) {
        this.screenLock = screenLock;
        this.context = context;

        deviceManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mPowerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        handler = new Handler();
    }

    public void process(float sensorValue){};
}
