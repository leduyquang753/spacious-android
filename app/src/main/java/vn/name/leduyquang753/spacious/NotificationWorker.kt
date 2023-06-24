package vn.name.leduyquang753.spacious;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import kotlin.math.min;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.SupervisorJob;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.flow.first;
import vn.name.leduyquang753.spacious.proto.copy;

class NotificationWorker: BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (
			ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
			!= PackageManager.PERMISSION_GRANTED
		) return;
		
		val result = goAsync();
		CoroutineScope(SupervisorJob()).launch(EmptyCoroutineContext) {
			try {
				val dataStore = context.dataStore;
				val data = dataStore.data.first();
				val today = Date.today();
				val previousDate = Date(min(data.lastNotificationDate, today.index - 1));

				val notificationManager = NotificationManagerCompat.from(context);
				for (reminder in data.remindersList) if (reminder.hasNotification(previousDate, today)) {
					val notificationIntent = Intent(context, MainActivity::class.java);
					notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK;
					notificationIntent.putExtra("id", reminder.id);
					notificationManager.notify(
						System.currentTimeMillis().toInt(),
						NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
						.setSmallIcon(R.drawable.app_icon_foreground)
						.setContentTitle(context.getString(R.string.notification_title))
						.setContentText(reminder.notificationText.ifEmpty{reminder.name})
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setContentIntent(
							PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
						)
						.setAutoCancel(true)
						.build()
					);
				}

				dataStore.updateData { oldData -> oldData.copy { lastNotificationDate = today.index; }; };
			} finally {
				result.finish();
			}
		};
	}
}