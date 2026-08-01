package com.danielkao.autoscreenonoff.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.danielkao.autoscreenonoff.R;
import com.danielkao.autoscreenonoff.util.CV;

public class ToggleAutoScreenOnOffAppWidgetProvider extends AppWidgetProvider {

    // must not collide with the other service PendingIntents: the intents
    // differ only in extras, which PendingIntent ignores when matching
    private static final int RC_WIDGET_TOGGLE = 20;

	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int i = 0; i < N; i++) {
			CV.logv("onUpdate in AppWidget");
			int appWidgetId = appWidgetIds[i];
            updateRemoteViews(context, appWidgetManager, appWidgetId,false);
		}
	}

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String strAction = intent.getAction();
        if (CV.UPDATE_WIDGET_ACTION.equals(strAction)){
            boolean b = intent.getBooleanExtra(CV.PREF_CHARGING_ON, false);
            CV.logv("update widget action is received:%b", b);

            ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                                                            getClass().getName());
            AppWidgetManager awm  = AppWidgetManager.getInstance(context);

            int ids[] = awm.getAppWidgetIds(thisAppWidget);

            // no widgets yet, return
            if (ids.length == 0)return;

            // widget exists, update all
            for(int appWidgetID : ids)
                updateRemoteViews(context, awm, appWidgetID,b);

        }
    }

    private void updateRemoteViews(Context context, AppWidgetManager awm, int appWidgetId, boolean isCharging){
        CV.logv("onUpdate in AppWidget");

        Intent intent = CV.serviceIntent(context, CV.SERVICEACTION_TOGGLE);
        intent.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_WIDGET);

        PendingIntent pendingIntent = CV.servicePendingIntent(context, RC_WIDGET_TOGGLE, intent);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.toggleonoff_appwidget);
        views.setOnClickPendingIntent(R.id.imageview, pendingIntent);

        // ------ change images!!
        boolean autoOn = CV.getPrefAutoOnoff(context);
        if (autoOn) {
            // set icon to on
            views.setImageViewResource(R.id.imageview, R.drawable.widget_on);
        } else {
            // check whether charging_on is on and it's under charging state
            if((isCharging || CV.isPlugged(context))
                    && CV.getPrefChargingOn(context)) {
                views.setImageViewResource(R.id.imageview, R.drawable.widget_charging_on);
            } else{
                // set icon to off
                views.setImageViewResource(R.id.imageview, R.drawable.widget_off);
            }
        }
        // ------ change images!! end

        // Tell the AppWidgetManager to perform an update on the current app widget
        awm.updateAppWidget(appWidgetId, views);
    }
}
