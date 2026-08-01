package com.danielkao.autoscreenonoff.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;
import com.danielkao.autoscreenonoff.R;
import com.danielkao.autoscreenonoff.util.CV;

public class ScreenOffAppWidgetProvider extends AppWidgetProvider {

    // must not collide with the other service PendingIntents: the intents
    // differ only in extras, which PendingIntent ignores when matching
    private static final int RC_WIDGET_SCREENOFF = 21;

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

        PendingIntent pendingIntent = CV.servicePendingIntent(context, RC_WIDGET_SCREENOFF,
                CV.serviceIntent(context, CV.SERVICEACTION_SCREENOFF));

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.screenoff_appwidget);
        views.setOnClickPendingIntent(R.id.imageview, pendingIntent);


        // Tell the AppWidgetManager to perform an update on the current app widget
        awm.updateAppWidget(appWidgetId, views);
    }
}
