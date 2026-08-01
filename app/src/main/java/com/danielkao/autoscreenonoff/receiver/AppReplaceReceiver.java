package com.danielkao.autoscreenonoff.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.danielkao.autoscreenonoff.util.CV;

/**
 * Restarts monitoring after the app itself is updated
 * (MY_PACKAGE_REPLACED is only ever delivered to the updated app).
 */
public class AppReplaceReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        CV.logv("AppReplaceReceiver app updated");

        // auto pref is on
        if(CV.getPrefAutoOnoff(context)){
            Intent i = CV.serviceIntent(context, CV.SERVICEACTION_TOGGLE);
            i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_SETTING);
            CV.startService(context, i);
        }// check whether pref charging is on, and is under charging
        else if(CV.getPrefChargingOn(context) && CV.isPlugged(context)){
            Intent i = CV.serviceIntent(context, CV.SERVICEACTION_TURNON);
            i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_CHARGING);
            CV.startService(context, i);
        }
        else if(CV.getPrefChargingOn(context) || CV.getPrefShowNotification(context)){
            CV.startService(context, CV.serviceIntent(context, -1));
        }
    }
}
