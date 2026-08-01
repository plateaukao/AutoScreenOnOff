package com.danielkao.autoscreenonoff.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.danielkao.autoscreenonoff.util.CV;

/**
 * Created by plateau on 2013/06/03.
 */
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        CV.logv("boot receiver");

        // auto pref is on
        if(CV.getPrefAutoOnoff(context)){
            CV.logv("start service by boot receiver");
            Intent i = CV.serviceIntent(context, CV.SERVICEACTION_TOGGLE);
            i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_SETTING);
            CV.startService(context, i);

            // re-invoke alarmManager
            if(CV.getPrefSleeping(context)){
                CV.startService(context,
                        CV.serviceIntent(context, CV.SERVICEACTION_SET_SCHEDULE));
            }

        }// check whether pref charging is on, and is under charging
        else if(CV.getPrefChargingOn(context) && CV.isPlugged(context)){
            Intent i = CV.serviceIntent(context, CV.SERVICEACTION_TURNON);
            i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_CHARGING);
            CV.startService(context, i);
        }
        // charging mode while unplugged (or sticky notification): the service
        // has to idle in the foreground to observe power events, since
        // manifest receivers no longer get ACTION_POWER_CONNECTED
        else if(CV.getPrefChargingOn(context) || CV.getPrefShowNotification(context)){
            CV.startService(context, CV.serviceIntent(context, -1));
        }
    }
}
