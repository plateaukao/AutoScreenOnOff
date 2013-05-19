package com.danielkao.autoscreenonoff;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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
			ConstantValues.logv("onUpdate in AppWidget");
			int appWidgetId = appWidgetIds[i];

			Intent intent = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
			intent.putExtra(ConstantValues.SERVICEACTION,
					ConstantValues.SERVICEACTION_TOGGLE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			PendingIntent pendingIntent = PendingIntent.getService(context, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT);

			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.toggleonoff_appwidget);
			views.setOnClickPendingIntent(R.id.imageview, pendingIntent);

			// Tell the AppWidgetManager to perform an update on the current app
			// widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

}
