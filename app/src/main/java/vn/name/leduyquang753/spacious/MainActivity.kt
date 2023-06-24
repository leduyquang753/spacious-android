@file:OptIn(ExperimentalMaterial3Api::class)

package vn.name.leduyquang753.spacious;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.compose.setContent;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.compose.foundation.rememberScrollState;
import androidx.compose.foundation.verticalScroll;
import androidx.compose.foundation.layout.Column;
import androidx.compose.foundation.layout.Row;
import androidx.compose.foundation.layout.Spacer;
import androidx.compose.foundation.layout.fillMaxSize;
import androidx.compose.foundation.layout.fillMaxWidth;
import androidx.compose.foundation.layout.height;
import androidx.compose.foundation.layout.padding;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.Add;
import androidx.compose.material.icons.filled.DateRange;
import androidx.compose.material.icons.filled.Settings;
import androidx.compose.material3.AlertDialog;
import androidx.compose.material3.Card;
import androidx.compose.material3.DatePicker;
import androidx.compose.material3.DatePickerDialog;
import androidx.compose.material3.DatePickerFormatter;
import androidx.compose.material3.DismissValue;
import androidx.compose.material3.DropdownMenuItem;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.ExposedDropdownMenuBox;
import androidx.compose.material3.ExposedDropdownMenuDefaults;
import androidx.compose.material3.FloatingActionButton;
import androidx.compose.material3.Icon;
import androidx.compose.material3.IconButton;
import androidx.compose.material3.LocalTextStyle;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.Scaffold;
import androidx.compose.material3.Surface;
import androidx.compose.material3.SwipeToDismiss;
import androidx.compose.material3.Switch;
import androidx.compose.material3.Text;
import androidx.compose.material3.TextButton;
import androidx.compose.material3.TextField;
import androidx.compose.material3.TopAppBar;
import androidx.compose.material3.TopAppBarDefaults;
import androidx.compose.material3.rememberDatePickerState;
import androidx.compose.material3.rememberDismissState;
import androidx.compose.material3.rememberTopAppBarState;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.LaunchedEffect;
import androidx.compose.runtime.getValue;
import androidx.compose.runtime.mutableLongStateOf;
import androidx.compose.runtime.mutableStateOf;
import androidx.compose.runtime.remember;
import androidx.compose.runtime.rememberCoroutineScope;
import androidx.compose.runtime.saveable.rememberSaveable;
import androidx.compose.runtime.setValue;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.input.nestedscroll.nestedScroll;
import androidx.compose.ui.platform.LocalContext;
import androidx.compose.ui.res.pluralStringResource;
import androidx.compose.ui.res.stringResource;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.compose.ui.unit.dp;
import androidx.datastore.dataStore;
import androidx.datastore.core.DataStore;
import androidx.lifecycle.compose.collectAsStateWithLifecycle;
import androidx.navigation.compose.composable;
import androidx.navigation.compose.NavHost;
import androidx.navigation.compose.rememberNavController;
import java.util.Locale;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.flow.first;
import vn.name.leduyquang753.spacious.proto.Data;
import vn.name.leduyquang753.spacious.proto.RecurringUnit;
import vn.name.leduyquang753.spacious.proto.Reminder;
import vn.name.leduyquang753.spacious.proto.copy;
import vn.name.leduyquang753.spacious.proto.reminder;
import vn.name.leduyquang753.spacious.serializer.DataSerializer;
import vn.name.leduyquang753.spacious.ui.theme.SpaciousTheme;

internal val Context.dataStore by dataStore(fileName = "data.pb", serializer = DataSerializer);

const val NOTIFICATION_CHANNEL_ID = "vn.name.leduyquang753.spacious.reminders";

class MainActivity: ComponentActivity() {
	private lateinit var alarmManager: AlarmManager;
	private lateinit var notificationIntent: PendingIntent;

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState);

		val notificationChannel = NotificationChannel(
			NOTIFICATION_CHANNEL_ID, getString(R.string.notificationChannel_name), NotificationManager.IMPORTANCE_HIGH
		);
		notificationChannel.description = getString(R.string.notificationChannel_description);
		(getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
		.createNotificationChannel(notificationChannel);
		
		alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager;
		notificationIntent = PendingIntent.getBroadcast(
			this, 0, Intent(this, NotificationWorker::class.java), PendingIntent.FLAG_IMMUTABLE
		);

		registerForActivityResult(RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS);
		
		setContent { SpaciousTheme { Surface(
			modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
		) {
			val navigationController = rememberNavController();
			NavHost(navigationController, "main") {
				composable("main") {
					MainScreen(dataStore, intent) { navigationController.navigate("settings"); };
				};
				composable("settings") {
					SettingsScreen(dataStore, { time ->
						if (time == null) {
							alarmManager.cancel(notificationIntent);
						} else {
							setupNotification(time / 100, time % 100);
						}
					}) { navigationController.popBackStack(); };
				};
			};
		}; }; };
	}

	private fun setupNotification(hour: Int, minute: Int) {
		if (alarmManager.nextAlarmClock != null) alarmManager.cancel(notificationIntent);
		alarmManager.setRepeating(
			AlarmManager.RTC_WAKEUP,
			run {
				val timezone = ZoneId.systemDefault();
				val now = ZonedDateTime.ofInstant(Instant.now(), timezone);
				var alarmTime = ZonedDateTime.of(
					now.year, now.monthValue, now.dayOfMonth, hour, minute, 0, 0, timezone
				).toEpochSecond();
				if (now.toEpochSecond() > alarmTime) alarmTime += 24L * 60L * 60L;
				
				alarmTime * 1000;
			},
			24L * 60L * 60L * 1000L,
			notificationIntent
		);
	}
}

@Composable
fun MainScreen(dataStore: DataStore<Data>, intent: Intent, goToSettings: () -> Unit) {
	val coroutineScope = rememberCoroutineScope();
	
	val data by dataStore.data.collectAsStateWithLifecycle(DataSerializer.defaultValue);
	var editingReminder by rememberSaveable { mutableStateOf<Reminder?>(null); };
	var deletingReminder by rememberSaveable { mutableLongStateOf(-1L); };
	var deletingReminderName by rememberSaveable { mutableStateOf(""); };
	
	val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState());
	
	Scaffold(
		Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
		topBar = { TopAppBar(
			title = { Text(stringResource(R.string.app_name), maxLines = 1, overflow = TextOverflow.Ellipsis); },
			actions = { IconButton({ goToSettings(); }) {
				Icon(Icons.Filled.Settings, stringResource(R.string.main_settings));
			}; },
			scrollBehavior = topBarScrollBehavior
		); },
		floatingActionButton = { FloatingActionButton({
			editingReminder = reminder {
				id = 0;
				name = "";
				recurring = false;
				date = Date.today().index;
				recurringAmount = 1;
				recurringUnit = RecurringUnit.DAYS;
				notificationText = "";
			};
		}) {
			Icon(Icons.Filled.Add, stringResource(R.string.main_add));
		}; }
	) { mainPadding ->
		Column(Modifier.padding(mainPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
			val today = Date.today();
			for (reminder in data.remindersList) SwipeToDismiss(
				rememberDismissState(confirmValueChange = { state ->
					if (state != DismissValue.Default) {
						deletingReminder = reminder.id;
						deletingReminderName = reminder.name;
					}
					false;
				}),
				{},
				{ Card(
					{ editingReminder = reminder; },
					Modifier.fillMaxWidth().padding(bottom = 8.dp)
				) { Column(Modifier.padding(16.dp)) {
					Text(
						reminder.name,
						style = MaterialTheme.typography.titleSmall,
						modifier = Modifier.padding(bottom = 8.dp)
					);
					if (reminder.notificationText.isNotEmpty()) Text(
						reminder.notificationText,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.padding(bottom = 8.dp)
					);
					Text(run {
						val nextNotification = getNextNotification(
							reminder.recurring, Date(reminder.date), reminder.recurringAmount, reminder.recurringUnit, today
						);
						if (today.index >= nextNotification.index) stringResource(R.string.main_notificationDatePassed)
						else stringResource(
							R.string.main_nextNotification,
							"${nextNotification.day}/${nextNotification.month}/${nextNotification.year}"
						);
					}, style = MaterialTheme.typography.bodySmall);
				}; }; }
			);
			Spacer(Modifier.height(80.dp)); // Avoid the floating action button.
		};
	};
	val localEditingReminder = editingReminder;
	if (localEditingReminder != null) EditReminderDialog(
		localEditingReminder
	) { editedReminder -> coroutineScope.launch {
		editingReminder = null;
		if (editedReminder == null) return@launch;
		dataStore.updateData { data -> data.copy outer@ {
			if (editedReminder.id == 0L) {
				reminders += editedReminder.copy {
					id = this@outer.nextId;
				};
				nextId += 1;
			} else {
				val newList = reminders.map { reminder ->
					if (reminder.id == editedReminder.id) editedReminder else reminder
				};
				reminders.clear();
				reminders.addAll(newList);
			}
		}; };
	}; };
	if (deletingReminder != -1L) AlertDialog(
		onDismissRequest = { deletingReminder = -1L; },
		confirmButton = { TextButton({ coroutineScope.launch {
			dataStore.updateData { data -> data.copy {
				val newList = reminders.filter { reminder -> reminder.id != deletingReminder };
				reminders.clear();
				reminders.addAll(newList);
			}; };
			deletingReminder = -1L;
		}; }) { Text(stringResource(R.string.delete_delete)); }; },
		dismissButton = { TextButton({ deletingReminder = -1L; }) { Text(stringResource(R.string.delete_cancel)); }; },
		title = { Text(stringResource(R.string.delete_title)); },
		text = { Text(stringResource(R.string.delete_description, deletingReminderName)); }
	);

	LaunchedEffect(intent) {
		val tappedId = intent.getLongExtra("id", -1L);
		for (reminder in dataStore.data.first().remindersList) if (reminder.id == tappedId) {
			editingReminder = reminder;
			break;
		}
	};
}

@Composable
fun EditReminderDialog(editingReminder: Reminder, onDismiss: (Reminder?) -> Unit) {
	val context = LocalContext.current;
	
	var reminderName by rememberSaveable { mutableStateOf(editingReminder.name); };
	var reminderNotificationText by rememberSaveable { mutableStateOf(editingReminder.notificationText); };
	var reminderDate by rememberSaveable {
		val date = Date(editingReminder.date);
		mutableLongStateOf(
			ZonedDateTime.of(date.year, date.month, date.day, 0, 0, 0, 0, ZoneId.of("Z")).toEpochSecond() * 1000
		);
	};
	val datePickerValue = rememberDatePickerState(reminderDate);
	var datePickerVisible by rememberSaveable { mutableStateOf(false); };
	var reminderRecurring by rememberSaveable { mutableStateOf(editingReminder.recurring); };
	var reminderRecurringAmount by rememberSaveable { mutableStateOf(editingReminder.recurringAmount.toString()); };
	var reminderRecurringUnit by rememberSaveable { mutableStateOf(run {
		when (editingReminder.recurringUnit) {
			RecurringUnit.YEARS -> context.getString(R.string.edit_recurringUnit_years);
			RecurringUnit.MONTHS -> context.getString(R.string.edit_recurringUnit_months);
			else -> context.getString(R.string.edit_recurringUnit_days);
		}
	}); };
	var reminderRecurringUnitDropdownExpanded by remember { mutableStateOf(false); };

	val parsedReminderRecurringAmount = run {
		try {
			reminderRecurringAmount.toInt();
		} catch (e: NumberFormatException) {
			-1
		}
	};
	
	val nameError = reminderName.isEmpty();
	val recurringAmountError = parsedReminderRecurringAmount <= 0;
	
	AlertDialog(
		onDismissRequest = { onDismiss(null); },
		title = { Text(stringResource(
			if (editingReminder.id == 0L) R.string.edit_addReminder else R.string.edit_editReminder
		)); },
		text = { Column {
			TextField(
				reminderName,
				{ newName -> reminderName = newName; },
				Modifier.fillMaxWidth().padding(bottom = 16.dp),
				placeholder = { Text(stringResource(R.string.edit_reminderName)); },
				singleLine = true,
				textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
				isError = nameError
			);
			Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
				Switch(
					reminderRecurring,
					{ recurring ->
						reminderRecurring = recurring;
						if (!recurring && parsedReminderRecurringAmount <= 0) reminderRecurringAmount = "1";
					},
					Modifier.padding(end = 8.dp)
				);
				Text(
					stringResource(R.string.edit_recurring),
					style = MaterialTheme.typography.labelMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				);
			};
			Text(
				stringResource(if (reminderRecurring) R.string.edit_startDate else R.string.edit_date),
				Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.labelMedium
			);
			Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
				Text(run {
					val date = ZonedDateTime.ofInstant(
						Instant.ofEpochMilli(reminderDate), ZoneId.of("Z")
					);
					
					"${date.dayOfMonth}/${date.monthValue}/${date.year}";
				}, style = MaterialTheme.typography.bodyLarge);
				IconButton({
					datePickerValue.selectedDateMillis = reminderDate;
					datePickerVisible = true;
				}) { Icon(Icons.Filled.DateRange, stringResource(R.string.edit_pickDate)); };
			};
			Text(
				stringResource(R.string.edit_recurringPeriod),
				Modifier.padding(bottom = 8.dp),
				style = (
					if (reminderRecurring) MaterialTheme.typography.labelMedium
					else MaterialTheme.typography.labelMedium.copy(
						color = MaterialTheme.typography.labelMedium.color.copy(alpha = 0.38f)
					)
				)
			);
			Row(Modifier.padding(bottom = 16.dp)) {
				TextField(
					reminderRecurringAmount,
					{ newAmount -> reminderRecurringAmount = newAmount; },
					Modifier.padding(end = 4.dp).weight(1.0f),
					enabled = reminderRecurring,
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
				);
				ExposedDropdownMenuBox(
					reminderRecurringUnitDropdownExpanded,
					{
						if (reminderRecurring)
							reminderRecurringUnitDropdownExpanded = !reminderRecurringUnitDropdownExpanded;
					},
					Modifier.padding(start = 4.dp).weight(1.0f)
				) {
					TextField(
						reminderRecurringUnit, {}, Modifier.menuAnchor(),
						enabled = reminderRecurring,
						readOnly = true,
						trailingIcon = {
							ExposedDropdownMenuDefaults.TrailingIcon(reminderRecurringUnitDropdownExpanded);
						},
						colors = ExposedDropdownMenuDefaults.textFieldColors()
					);
					ExposedDropdownMenu(
						reminderRecurringUnitDropdownExpanded,
						{ reminderRecurringUnitDropdownExpanded = false; }
					) {
						for (unit in listOf(
							stringResource(R.string.edit_recurringUnit_days),
							stringResource(R.string.edit_recurringUnit_months),
							stringResource(R.string.edit_recurringUnit_years)
						)) DropdownMenuItem(
							{ Text(unit); },
							{ reminderRecurringUnit = unit; reminderRecurringUnitDropdownExpanded = false; },
							contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
						);
					};
				};
			};
			TextField(
				reminderNotificationText,
				{ newText -> reminderNotificationText = newText; },
				Modifier.fillMaxWidth().padding(bottom = 16.dp),
				placeholder = { Text(reminderName); },
				label = { Text(stringResource(R.string.edit_notificationText)); }
			);
			Text(
				if (nameError) stringResource(R.string.edit_nameError)
				else if (recurringAmountError) stringResource(R.string.edit_recurringAmountError)
				else {
					val today = Date.today();
					val nextNotification = getNextNotification(
						reminderRecurring,
						getDateFromMillis(reminderDate),
						parsedReminderRecurringAmount,
						when (reminderRecurringUnit) {
							stringResource(R.string.edit_recurringUnit_years) -> RecurringUnit.YEARS;
							stringResource(R.string.edit_recurringUnit_months) -> RecurringUnit.MONTHS;
							else -> RecurringUnit.DAYS;
						},
						today
					);
					val days = nextNotification.index - today.index;
					if (days <= 0) stringResource(R.string.edit_notificationDatePassed)
					else pluralStringResource(
						R.plurals.edit_nextNotification, days,
						days, "${nextNotification.day}/${nextNotification.month}/${nextNotification.year}"
					);
				},
				color = if (nameError || recurringAmountError) MaterialTheme.colorScheme.error else Color.Unspecified
			);

			if (datePickerVisible) {
				DatePickerDialog(
					onDismissRequest = { datePickerVisible = false; },
					confirmButton = {
						TextButton({
							val dateMillis = datePickerValue.selectedDateMillis;
							if (dateMillis != null) reminderDate = dateMillis;
							datePickerVisible = false;
						}) { Text(stringResource(R.string.edit_datePicker_set)); };
					},
					dismissButton = { TextButton({ datePickerVisible = false; }) {
						Text(stringResource(R.string.edit_cancel));
					}; }
				) { DatePicker(
					datePickerValue,
					dateFormatter = object: DatePickerFormatter {
						private val context = LocalContext.current;
						override fun formatDate(
							dateMillis: Long?, locale: Locale, forContentDescription: Boolean
						): String {
							if (dateMillis == null) return context.getString(R.string.edit_datePicker_none);
							val date = ZonedDateTime.ofInstant(
								Instant.ofEpochMilli(dateMillis), ZoneId.of("Z")
							);
							return "${date.dayOfMonth}/${date.monthValue}/${date.year}";
						}

						override fun formatMonthYear(monthMillis: Long?, locale: Locale): String {
							if (monthMillis == null) return context.getString(R.string.edit_datePicker_none);
							val date = ZonedDateTime.ofInstant(
								Instant.ofEpochMilli(monthMillis), ZoneId.of("Z")
							);
							return "${date.monthValue}/${date.year}";
						}
					}
				); };
			}
		}; },
		confirmButton = { TextButton(
			{ onDismiss(reminder {
				id = editingReminder.id;
				name = reminderName;
				date = getDateFromMillis(reminderDate).index;
				recurring = reminderRecurring;
				recurringAmount = parsedReminderRecurringAmount;
				recurringUnit = when (reminderRecurringUnit) {
					context.getString(R.string.edit_recurringUnit_years) -> RecurringUnit.YEARS;
					context.getString(R.string.edit_recurringUnit_months) -> RecurringUnit.MONTHS;
					else -> RecurringUnit.DAYS;
				};
				notificationText = reminderNotificationText;
			}); },
			enabled = !nameError && !recurringAmountError
		) { Text(stringResource(R.string.edit_save)); }; },
		dismissButton = { TextButton({ onDismiss(null); }) { Text(stringResource(R.string.edit_cancel)); }; }
	);
}

private fun getDateFromMillis(millis: Long): Date {
	val dateTime = ZonedDateTime.ofInstant(
		Instant.ofEpochMilli(millis), ZoneId.of("Z")
	);
	return Date(dateTime.dayOfMonth, dateTime.monthValue, dateTime.year);
}