package com.danielkao.autoscreenonoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by plateau on 2013/06/10.
 */
public class AppReplaceReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String pkg = intent.getPackage();
        CV.logv("AppReplaceReceiver app updated:%s",pkg);
        // screen onoff is upgraded
        if(pkg != null && pkg.equals("com.danielkao.autoscreenonoff")){

            // auto pref is on
            if(CV.getPrefAutoOnoff(context)){
                CV.logv("start service by app receiver receiver");
                // send intent to service
                Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
                i.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_TOGGLE);
                i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_SETTING);
                context.startService(i);
            }// check whether pre charging is on, and is under charging
            else if(CV.getPrefChargingOn(context) && CV.isPlugged(context)){
                Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
                i.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_TURNON);
                i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_CHARGING);
                context.startService(i);
            }
        }
    }
}
