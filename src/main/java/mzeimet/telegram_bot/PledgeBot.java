package mzeimet.telegram_bot;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class PledgeBot extends TelegramLongPollingBot {

	private static final String READ_PLEDGES_COMMAND = "/read";
	private static final String SHOW_PLEDGES_COMMAND = "/show";
	private static final String RESERVE_PLEDGE_COMMAND = "/reserve";
	private static final String WHERE_COMMAND = "/where";
	private static final String url = "https://esoleaderboards.com/widgets/pledgeometer.php/";
	private static final String ERROR_INTERPRET_COMMAND = "Beim Parsen des Befehls ist ein Fehler aufgetreten. Wadafaq.";
	private List<String> pledges;
	private LocalDateTime refreshTimestamp;

	private static enum Command {
		READ, NONE, SHOW, RESERVE, WHERE
	};

	private static enum Users {
		Marvin, Dennis, Patrick, Steven
	}

	public PledgeBot() {
		refreshTimestamp = LocalDateTime.now().minusDays(1); // refresht auf
																// jeden dann
	}

	public void onUpdateReceived(Update update) {
		// We check if the update has a message and the message has text
		if (update.hasMessage() && update.getMessage().hasText()) {
			String txtIn = update.getMessage().getText();
			String txtOut = "";
			if (txtIn.contains("@" + getBotUsername()))
				txtIn = txtIn.replace("@" + getBotUsername(), "");
			Command command = convertCommand(txtIn);
			SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId());
			switch (command) {
			case READ:
				refreshPledges();
				message.setText( "Die heutigen Gelöbnisse sind: \n1. " + pledges.get(0) + "\n2. " + pledges.get(1) + "\n3. "
						+ pledges.get(2));
				break;
			case SHOW:
				break;

			case RESERVE:
				txtIn = txtIn.replace(RESERVE_PLEDGE_COMMAND, "");
				refreshPledges();
//				txtOut = reserve(txtIn);
				break;

			case WHERE:
				message.setText(whichUser());
				break;
			case NONE:
				return;
			default:
				message.setText(ERROR_INTERPRET_COMMAND);
			}
			try {
				sendMessage(message); // Call method to send the message
			} catch (TelegramApiException e) {
				e.printStackTrace();
			}
		}
	}

	private String whichUser() {
		LocalDateTime date = LocalDateTime.now();
		// Or use a specific locale, or configure your own rules
		WeekFields weekFields = WeekFields.of(Locale.GERMANY);
		int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
		if(date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY))
			weekNumber += 1;
		weekNumber %= 4;
		return "Nächsten Freitag sind wir bei: " + Users.values()[weekNumber].toString();
	}

	private void reserve(String txtIn) {
		int pledgeNumber = -1;
	}

	public String getBotUsername() {
		return "DailyPledgeBot";
	}

	public String getBotToken() {
		return System.getenv("TOKEN");
	}

	private static Command convertCommand(String str) {
		if (str.equals(READ_PLEDGES_COMMAND))
			return Command.READ;
		if (str.equals(SHOW_PLEDGES_COMMAND))
			return Command.SHOW;
		if (str.equals(RESERVE_PLEDGE_COMMAND))
			return Command.RESERVE;
		if (str.equals(WHERE_COMMAND))
			return Command.WHERE;
		return Command.NONE;
	}

	private void refreshPledges() {
		LocalDateTime now = LocalDateTime.now();
		if (now.getDayOfMonth() != refreshTimestamp.getDayOfMonth()
				|| now.getHour() > 8 && refreshTimestamp.getHour() <= 8){
			pledges = WebsiteReader.getPledges(url);
			refreshTimestamp = now;
		}
	}
}