package vn.name.leduyquang753.spacious;

import vn.name.leduyquang753.spacious.proto.RecurringUnit;
import vn.name.leduyquang753.spacious.proto.Reminder;

fun getNextNotification(
	recurring: Boolean, reminderDate: Date, recurringAmount: Int, recurringUnit: RecurringUnit, today: Date
): Date {
	if (!recurring || today.index < reminderDate.index) return reminderDate;
	when (recurringUnit) {
		RecurringUnit.DAYS -> {
			return Date(
				reminderDate.index
				+ (today.index-reminderDate.index+recurringAmount) / recurringAmount * recurringAmount
			);
		}
		RecurringUnit.MONTHS -> {
			val startMonthIndex = getMonthIndex(reminderDate);
			val currentMonthIndex = getMonthIndex(today);
			var nextMonthIndex = (
				startMonthIndex
				+ (currentMonthIndex-startMonthIndex) / recurringAmount * recurringAmount
			);
			if (
				currentMonthIndex > nextMonthIndex
				|| (currentMonthIndex == nextMonthIndex && today.day >= reminderDate.day)
			) nextMonthIndex += recurringAmount;
			return Date(reminderDate.day, nextMonthIndex % 12 + 1, nextMonthIndex / 12);
		}
		RecurringUnit.YEARS -> {
			var nextYear = (
				reminderDate.year
				+ (today.year-reminderDate.year) / recurringAmount * recurringAmount
			);
			if (
				today.year > nextYear
				|| (today.year == nextYear && (
					today.month > reminderDate.month
					|| (today.month == reminderDate.month && today.day >= reminderDate.day)
				))
			) nextYear += recurringAmount;
			return Date(reminderDate.day, reminderDate.month, nextYear);
		}
		else -> {
			return reminderDate;
		}
	}
}

private fun getMonthIndex(date: Date): Int = date.year*12 + date.month - 1;

fun Reminder.hasNotification(previousDate: Date, date: Date): Boolean {
	val reminderDate = Date(this.date);
	if (date.index < reminderDate.index) return false;
	if (recurring) {
		when (recurringUnit) {
			RecurringUnit.YEARS -> {
				val mod = reminderDate.year % recurringAmount;
				val divDifference = (
					(date.year-mod) / recurringAmount
					- (previousDate.year-mod) / recurringAmount
				);
				return divDifference >= 0 && (
					if (divDifference == 0) (
						date.year == previousDate.year
						&& (date.year-mod) % recurringAmount == 0
						&& (reminderDate.month > previousDate.month || (
							reminderDate.month == previousDate.month && reminderDate.day > previousDate.day
						))
						&& (reminderDate.month < date.month || (
							reminderDate.month == date.month && reminderDate.day <= date.day
						))
					) else (
						divDifference != 1
						|| (date.year-mod) % recurringAmount != 0
						|| date.month > reminderDate.month
						|| (date.month == reminderDate.month && date.day >= reminderDate.day)
					)
				);
			}
			RecurringUnit.MONTHS -> {
				val mod = getMonthIndex(reminderDate) % recurringAmount;
				val previousDateMonthIndex = getMonthIndex(previousDate);
				val dateMonthIndex = getMonthIndex(date);
				val divDifference = (
					(dateMonthIndex-mod) / recurringAmount
					- (previousDateMonthIndex-mod) / recurringAmount
				);
				return divDifference >= 0 && (
					if (divDifference == 0) (
						dateMonthIndex == previousDateMonthIndex
						&& (dateMonthIndex-mod) % recurringAmount == 0
						&& reminderDate.day > previousDate.day
						&& reminderDate.day <= date.day
					) else (
						divDifference != 1
						|| (dateMonthIndex-mod) % recurringAmount != 0
						|| date.day >= reminderDate.day
					)
				);
			}
			else -> {
				val mod = reminderDate.index % recurringAmount;
				return (previousDate.index-mod) / recurringAmount < (date.index-mod) / recurringAmount;
			}
		}
	} else {
		return reminderDate.index > previousDate.index;
	}
}