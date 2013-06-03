package com.danielkao.autoscreenonoff;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ToggleAutoScreenOnOffAppWidgetProvider extends AppWidgetProvider {

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

            //Toast.makeText(context,"onReceive", Toast.LENGTH_SHORT);

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

        Intent intent = new Intent(CV.SERVICE_INTENT_ACTION);
        intent.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_TOGGLE);
        intent.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_WIDGET);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
