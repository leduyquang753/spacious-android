package vn.name.leduyquang753.spacious;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
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
						.setSmallIcon(R.drawable.notification_icon)
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

				val notificationTime = data.notificationTime;
				setupNotification(
					context, notificationTime / 100, notificationTime % 100, NotificationScheduleType.FORCE_TOMORROW
				);

				dataStore.updateData { oldData -> oldData.copy { lastNotificationDate = today.index; }; };
			} finally {
				result.finish();
			}
		};
	}
}

internal fun setupNotification(context: Context, hour: Int, minute: Int, scheduleType: NotificationScheduleType) {
	val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
	val notificationIntent = PendingIntent.getBroadcast(
		context, 0, Intent(context, NotificationWorker::class.java), PendingIntent.FLAG_IMMUTABLE
	);
	if (alarmManager.nextAlarmClock != null) alarmManager.cancel(notificationIntent);
	alarmManager.setWindow(
		AlarmManager.RTC_WAKEUP,
		run {
			val timezone = ZoneId.systemDefault();
			val now = ZonedDateTime.ofInstant(Instant.now(), timezone);
			var alarmTime = ZonedDateTime.of(
				now.year, now.monthValue, now.dayOfMonth, hour, minute, 0, 0, timezone
			).toEpochSecond();
			if (
				scheduleType != NotificationScheduleType.FORCE_TODAY
				&& (scheduleType == NotificationScheduleType.FORCE_TOMORROW || now.toEpochSecond() > alarmTime)
			) alarmTime += 24L * 60L * 60L;
			
			alarmTime * 1000;
		},
		10L * 60L * 1000L,
		notificationIntent
	);
}

internal enum class NotificationScheduleType {
	FORCE_TODAY, TODAY_OR_TOMORROW, FORCE_TOMORROW
}