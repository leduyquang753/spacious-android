package vn.name.leduyquang753.spacious;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.SupervisorJob;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.flow.first;

/*
	This handles setting up the alarm when the device has just started up, for all alarms are not kept when the device
	shuts down.
*/
class StartupHandler: BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != "android.intent.action.BOOT_COMPLETED") return;
		val result = goAsync();
		CoroutineScope(SupervisorJob()).launch(EmptyCoroutineContext) {
			try {
				val dataStore = context.dataStore;
				val data = dataStore.data.first();
				if (!data.enableNotifications) return@launch;
				val notificationTime = data.notificationTime;
				setupNotification(
					context, notificationTime / 100, notificationTime % 100,
					if (Date.today().index > data.lastNotificationDate) NotificationScheduleType.FORCE_TODAY
					else NotificationScheduleType.TODAY_OR_TOMORROW
				);
			} finally {
				result.finish();
			}
		}
	}
}