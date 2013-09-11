package com.danielkao.autoscreenonoff.strategy;

import android.content.Context;
import android.os.PowerManager.WakeLock;
import com.danielkao.autoscreenonoff.util.CV;

/**
 * Created by plateau on 2013/09/12.
 */
public class BaseTurnOnStrategy extends BaseStrategy{

    //handle timeout function
    private int CALLBACK_EXISTS=1;
    //private Timer timer;

    protected Context context;

    public BaseTurnOnStrategy(Context context, WakeLock screenLock) {
        super(context,screenLock);
    }

    @Override
    public void process(float sensorValue) {
        // cover
        if (sensorValue == 0f) {
            //
        }
        // uncover
        else {
            if (!mPowerManager.isScreenOn()) {
                long timeout = (long) CV.getPrefTimeoutUnlock(context);
                if(timeout==0)
                    turnOn();
                else
                    handler.postDelayed(runnableTurnOn, timeout);
            }
        }
    }

    private Runnable runnableTurnOn = new Runnable() {
        @Override
        public void run() {
            CV.logi("sensor: turn on thread");
            turnOn();
            resetHandler();
        }
    };

    private void resetHandler(){
        CV.logv("reset Handler");
        handler.removeMessages(CALLBACK_EXISTS);
        handler.removeCallbacks(runnableTurnOn);
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
}
