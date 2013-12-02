package com.danielkao.autoscreenonoff.strategy;

import android.content.Context;
import android.media.AudioManager;
import android.os.PowerManager.WakeLock;
import com.danielkao.autoscreenonoff.util.CV;

/**
 * Created by plateau on 2013/09/12.
 */
public class BaseTurnOffStrategy extends BaseStrategy{

    //handle timeout function
    private int CALLBACK_EXISTS=0;

    protected AudioManager am;

    public BaseTurnOffStrategy(Context context, WakeLock screenLock) {
        super(context,screenLock);
        am = (AudioManager)context.getSystemService(context.AUDIO_SERVICE);
    }

    @Override
    public void process(float sensorValue) {
        super.process(sensorValue);

        if (sensorValue == 0f) {
            if (mPowerManager.isScreenOn()) {
                // check if it is disabled during landscape mode, and now it's really in landscape
                // --> return
                /*
                if(CV.getPrefDisableInLandscape(context) && isOrientationLandscape()){
                    return;
                }
                else{
                */
                {
                    long timeout = (long) CV.getPrefTimeoutLock(context);
                    if(timeout == 0)
                        turnOff();
                    else
                        handler.postDelayed(runnableTurnOff, timeout);
                }
            }
        }
        //  uncover
        else {
        }
    }

    protected void turnOff(){
        CV.logv("sensor: turn off thread");
        if(screenLock.isHeld())
            screenLock.release();
        deviceManager.lockNow();
        playCloseSound();

    }

    private void playCloseSound(){
        if(CV.getPrefPlayCloseSound(context)){
            float vol = 1.0f;
            am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, vol);
        }
    }

    protected void resetHandler(){
        CV.logv("reset Handler");
        handler.removeMessages(CALLBACK_EXISTS);
        handler.removeCallbacks(runnableTurnOff);
    }

    private Runnable runnableTurnOff = new Runnable() {
        @Override
        public void run() {
            turnOff();
            resetHandler();
        }
    };
}
