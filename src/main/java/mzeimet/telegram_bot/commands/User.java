package mzeimet.telegram_bot.commands;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

import mzeimet.telegram_bot.Config.Users;

public class User {
	public static String whichUser() {
		LocalDateTime date = LocalDateTime.now();
		WeekFields weekFields = WeekFields.of(Locale.GERMANY);
		int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
		if (date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY))
			weekNumber += 1;
		weekNumber %= 4;
		return "Nächsten Freitag sind wir bei: " + Users.values()[weekNumber].toString();
	}
}
