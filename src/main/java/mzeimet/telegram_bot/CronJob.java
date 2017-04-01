package mzeimet.telegram_bot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimerTask;


public abstract class CronJob extends TimerTask {

	PledgeBot bot;

	public CronJob(PledgeBot bot) {
		this.bot = bot;
	}

	public long getSecondsTillNoon() {
		LocalDate day = LocalDate.now(ZoneId.of("Europe/Berlin"));
		if (LocalDateTime.now().getHour() > 12)
			day = day.plusDays(1);
		LocalDateTime nextNoon = LocalDateTime.of(day, LocalTime.NOON);
		long secondsTillTwelve = ChronoUnit.MINUTES.between( LocalDateTime.now(), nextNoon) * 60;
		return secondsTillTwelve;
	}


}
