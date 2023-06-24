package vn.name.leduyquang753.spacious;

import java.time.LocalDate;

class Date {
	companion object {
		private val monthDays: IntArray = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);
		private val monthDaysLeap: IntArray = intArrayOf(0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);
		private val monthPos: IntArray = intArrayOf(0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365);
		private val monthPosLeap: IntArray = intArrayOf(0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365);
		private val minIndex = Date(1, 1, 1900).index;
		private val maxIndex = Date(31, 12, 2100).index;

		private fun isLeapYear(year: Int): Boolean = year%4==0 && (year%100!=0 || year%400==0);
		
		fun isValidDate(day: Int, month: Int, year: Int): Boolean
			= year in 1900..2100
			&& month in 1..12
			&& day in 1..(if (isLeapYear(year)) monthDaysLeap else monthDays)[month];

		fun today(): Date {
			val localDate = LocalDate.now();
			return Date(localDate.dayOfMonth, localDate.monthValue, localDate.year);
		};
	}

	val day: Int;
	val month: Int;
	val year: Int;
	val index: Int;

	constructor(day: Int, month: Int, year: Int) {
		if (!isValidDate(day, month, year)) throw InvalidDateException(day, month, year);
		this.day = day;
		this.month = month;
		this.year = year;
		index = (
			(year - 1) * 365
			+ year / 4 - year / 100 + year / 400
			- (if (isLeapYear(year) && month < 3) 2 else 1) + monthPos[month] + day
		);
	}

	constructor(index: Int) {
		if (index < minIndex || index > maxIndex) throw InvalidDateException(index);
		this.index = index;
		var cycles: Int;
		val dayOfYear: Int;
		cycles = index/146097;
		var year = cycles * 400; // Gregorian calendar repeats every 146 097 days, or 400 years.
		if (index % 146097 == 146096) {
			// Handle the last day of the cycle, which is the 366th day of the 400th year.
			year += 400;
			dayOfYear = 365;
		} else {
			var temp = index - cycles*146097;
			// In each repeat cycle, it repeats every 100 years, or 36 524 days; the only irregular year is the
			// 400th year which is a leap year.
			cycles = temp / 36524;
			year += cycles * 100;
			// In that sub-cycle, it also repeats every 4 years or 1461 days, except the 100th which is not a leap year.
			temp -= cycles*36524;
			cycles = temp / 1461;
			year += cycles * 4;
			// In that sub-sub-cycle, it also repeats every year, or 365 days, except the 4th which is a leap year.
			temp -= cycles*1461;
			cycles = temp / 365;
			year += cycles;
			// Handle the last day of the 4-year cycle.
			year += if (cycles == 4) 0 else 1;
			dayOfYear = if (cycles == 4) 365 else temp - cycles*365;
		}
		this.year = year;
		val table = if (isLeapYear(year)) monthPosLeap else monthPos;
		var month = 1;
		while (month < 12) {
			if (dayOfYear < table[month+1]) break;
			++month;
		}
		this.month = month;
		day = dayOfYear - table[month] + 1;
	}

	class InvalidDateException: Exception {
		constructor(day: Int, month: Int, year: Int): super("day=$day; month=$month; year=$year");
		constructor(index: Int): super("index=$index");
	}
}