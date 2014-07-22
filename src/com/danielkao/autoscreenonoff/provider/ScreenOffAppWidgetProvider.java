package com.danielkao.autoscreenonoff.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.danielkao.autoscreenonoff.R;
import com.danielkao.autoscreenonoff.util.CV;

public class ScreenOffAppWidgetProvider extends AppWidgetProvider {

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

    private void updateRemoteViews(Context context, AppWidgetManager awm, int appWidgetId, boolean isCharging){
        CV.logv("onUpdate in AppWidget");

        Intent intentScreenOff = new Intent(CV.SERVICE_INTENT_ACTION);
        intentScreenOff.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_SCREENOFF);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                intentScreenOff, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.screenoff_appwidget);
        views.setOnClickPendingIntent(R.id.imageview, pendingIntent);


        // Tell the AppWidgetManager to perform an update on the current app widget
        awm.updateAppWidget(appWidgetId, views);
    }
}
