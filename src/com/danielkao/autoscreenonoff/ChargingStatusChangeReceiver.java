package com.danielkao.autoscreenonoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;

// TODO: boot receiver
// dialog for new versions: add ad here?

/**
 * Created by plateau on 2013/05/20.
 */
public class ChargingStatusChangeReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        // not enabled
        if(!CV.getPrefChargingOn(context))
            return;

        String action = intent.getAction();
        if(action.equals(ACTION_POWER_CONNECTED)) {
            CV.logv("is charging");

            Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
            i.putExtra(CV.SERVICEACTION,
                    CV.SERVICEACTION_TURNON);
            i.putExtra(CV.SERVICETYPE,
                    CV.SERVICETYPE_CHARGING);
            context.startService(i);
        }
        else if(action.equals(ACTION_POWER_DISCONNECTED)) {
            CV.logv("is not charging anymore");

            Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
            i.putExtra(CV.SERVICEACTION,
                    CV.SERVICEACTION_TURNOFF);
            i.putExtra(CV.SERVICETYPE,
                    CV.SERVICETYPE_CHARGING);
            context.startService(i);

        }
    }


}
