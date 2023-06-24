@file:OptIn(ExperimentalMaterial3Api::class)

package vn.name.leduyquang753.spacious;

import androidx.compose.foundation.rememberScrollState;
import androidx.compose.foundation.verticalScroll;
import androidx.compose.foundation.layout.Column;
import androidx.compose.foundation.layout.Row;
import androidx.compose.foundation.layout.padding;
import androidx.compose.foundation.layout.width;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.Close;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.Icon;
import androidx.compose.material3.IconButton;
import androidx.compose.material3.Scaffold;
import androidx.compose.material3.Switch;
import androidx.compose.material3.Text;
import androidx.compose.material3.TextButton;
import androidx.compose.material3.TextField;
import androidx.compose.material3.TopAppBar;
import androidx.compose.material3.TopAppBarDefaults;
import androidx.compose.material3.rememberTopAppBarState;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.LaunchedEffect;
import androidx.compose.runtime.getValue;
import androidx.compose.runtime.mutableStateOf;
import androidx.compose.runtime.rememberCoroutineScope;
import androidx.compose.runtime.saveable.rememberSaveable;
import androidx.compose.runtime.setValue;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.input.nestedscroll.nestedScroll;
import androidx.compose.ui.res.stringResource;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.compose.ui.unit.dp;
import androidx.datastore.core.DataStore;
import kotlinx.coroutines.launch;
import vn.name.leduyquang753.spacious.proto.Data;
import vn.name.leduyquang753.spacious.proto.copy;

@Composable
fun SettingsScreen(dataStore: DataStore<Data>, save: (Int?) -> Unit, goBack: () -> Unit) {
	val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState());
	
	val coroutineScope = rememberCoroutineScope();
	
	Scaffold(
		Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
		topBar = { TopAppBar(
			title = { Text(stringResource(R.string.settings_title), maxLines = 1, overflow = TextOverflow.Ellipsis); },
			navigationIcon = { IconButton({ goBack(); }) {
				Icon(Icons.Filled.Close, stringResource(R.string.settings_back));
			}; },
			scrollBehavior = topBarScrollBehavior
		); }
	) { mainPadding ->
		Column(Modifier.padding(mainPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
			var notificationsEnabled by rememberSaveable { mutableStateOf(true); };
			var notificationHour by rememberSaveable { mutableStateOf("8"); };
			var notificationMinute by rememberSaveable { mutableStateOf("00"); };
			
			LaunchedEffect(dataStore) {
				dataStore.data.collect { data ->
					notificationsEnabled = data.enableNotifications;
					notificationHour = (data.notificationTime / 100).toString();
					notificationMinute = (data.notificationTime % 100).toString().padStart(2, '0');
				};
			};
			
			Text(stringResource(R.string.settings_reminderNotifications), Modifier.padding(bottom = 8.dp));
			Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
				Switch(
					notificationsEnabled,
					{ enabled -> notificationsEnabled = enabled; },
					Modifier.padding(end = 8.dp)
				);
				Text(stringResource(
					if (notificationsEnabled) R.string.settings_enabled else R.string.settings_disabled
				));
			}
			Text(stringResource(R.string.settings_notificationTime), Modifier.padding(bottom = 8.dp));
			Row(Modifier.padding(bottom = 16.dp)) {
				TextField(
					notificationHour,
					{ hour -> notificationHour = hour; },
					Modifier.alignByBaseline().width(64.dp),
					placeholder = { Text(
						stringResource(R.string.settings_hour), maxLines = 1, overflow = TextOverflow.Clip
					); },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
				);
				Text(" h ", Modifier.alignByBaseline());
				TextField(
					notificationMinute,
					{ minute -> notificationMinute = minute; },
					Modifier.alignByBaseline().width(64.dp),
					placeholder = { Text(
						stringResource(R.string.settings_minute), maxLines = 1, overflow = TextOverflow.Clip
					); },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
				);
			}
			TextButton(
				{ coroutineScope.launch {
					val combinedNotificationTime = (
						parseTimeElement(notificationHour, 23) * 100
						+ parseTimeElement(notificationMinute, 59)
					);
					save(if (notificationsEnabled) combinedNotificationTime else null);
					dataStore.updateData { data ->
						val newData = data.copy {
							enableNotifications = notificationsEnabled;
							notificationTime = combinedNotificationTime;
						};
						
						newData
					};
					goBack();
				}; },
				enabled = parseTimeElement(notificationHour, 23) != -1 && parseTimeElement(notificationMinute, 59) != -1
			) { Text(stringResource(R.string.settings_save)); };
		}
	};
}

private fun parseTimeElement(string: String, max: Int): Int {
	return try {
		val parsed = string.toInt();
		if (parsed in 0..max) parsed else -1;
	} catch (e: NumberFormatException) {
		-1;
	}
}