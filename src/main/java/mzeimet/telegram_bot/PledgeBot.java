package mzeimet.telegram_bot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import mzeimet.telegram_bot.Config.Users;

public class PledgeBot extends TelegramLongPollingBot {

	private List<String> pledges;
	private LocalDateTime refreshTimestamp;

	private static enum Command {
		READ, NONE, SHOW_TODAY, SHOW_RESERVED, WHERE, DELETE
	};

	private Timer timer;

	public PledgeBot() {
		refreshTimestamp = LocalDateTime.now().minusDays(1);
		timer = new Timer();
		addStartCrons();
	}

	private void addStartCrons() {
		CronJob cronjob = new ShowTodayCronjob(this);
		addCronJob(cronjob);
	}

	public void addCronJob(CronJob cron) {
		timer.schedule(cron, cron.getSecondsTillNoon() * 1000, Config.secondsPerDay);
	}

	public void onUpdateReceived(Update update) {
		try {
			if (update.hasMessage() && update.getMessage().hasText()) {
				SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId());
				executeCommand(message, update.getMessage().getText());
			} else if (update.hasCallbackQuery()) {
				String data = update.getCallbackQuery().getData();
				SendMessage message = new SendMessage().setChatId(update.getCallbackQuery().getMessage().getChatId());
				executeCallback(message, data, update.getCallbackQuery().getId());
			}
		} catch (Exception e) {
			Logger.write(e.getStackTrace().toString());
			SendMessage message = new SendMessage().setChatId(Config.MY_CHAT_ID);
			message.setText(e.getStackTrace().toString());
			try {
				sendMessage(message);
			} catch (TelegramApiException e1) {
				Logger.write(e.getStackTrace().toString());
				e1.printStackTrace();
			}
		}
	}

	private void executeCallback(SendMessage message, String data, String callbackQueryId) throws Exception {
		if (data.contains("/reserve")) {
			reserveCallback(message, data);

		} else if (data.contains("/cancel")) {
			data = data.replace("/cancel", "");
			deleteReservation(message, data);
		} else {
			throw new IllegalArgumentException("Nicht erwartete Callback Query!");
		}
		try {
			sendMessage(message);
			AnswerCallbackQuery q = new AnswerCallbackQuery();
			q.setCallbackQueryId(callbackQueryId);
			answerCallbackQuery(q);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	public void executeCommand(SendMessage message, String txtIn) throws IOException {
		if (txtIn.contains("@" + getBotUsername()))
			txtIn = txtIn.replace("@" + getBotUsername(), "");
		Command command = convertCommand(txtIn);
		try {
			switch (command) {
			case READ:
				refreshPledges();
				setReadText(message);
				break;
			case SHOW_TODAY:
				showAvailablePledges(message);
				break;

			case SHOW_RESERVED:
				refreshPledges();
				getAvailablePledgesButtons(message);
				break;

			case WHERE:
				message.setText(whichUser());
				break;
			case DELETE:
				try {
					showCancelButtons(message);
				} catch (IOException e1) {
					message.setText("Sorry, hab beim Lesen der Gelöbnisse verkackt :(");
					e1.printStackTrace();
					throw e1;
				}
				break;
			case NONE:
				return;
			default:
				message.setText(Config.ERROR_INTERPRET_COMMAND);
			}
		} catch (Exception e) {
			message.setText(message.getText() + " \n" + e.getStackTrace());
		}
		try {
			sendMessage(message); // Call method to send the message
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	private void deleteReservation(SendMessage message, String pledgeName) throws Exception {
		try {
			File inputFile = new File(Config.FILE_PATH);
			File tempFile = new File("./tmpReservation.txt");

			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String currentLine;

			while ((currentLine = reader.readLine()) != null) {
				String trimmedLine = currentLine.trim();
				if (trimmedLine.contains(pledgeName))
					continue;
				writer.write(currentLine + System.getProperty("line.separator"));
			}
			writer.close();
			reader.close();
			FileUtils.copyFile(tempFile, inputFile);
			FileUtils.forceDelete(tempFile);
		} catch (Exception e) {
			e.printStackTrace();
			message.setText("Sorry, hab beim Löschen aus der Datei verkackt :(");
			throw e;
		}
		message.setText("Jo habs gelöscht!");

	}

	private void reserveCallback(SendMessage message, String data) throws Exception {
		data = data.replace("/reserve", "");
		if (data.length() == 1) {
			getAvailableUserButtons(message, data);
		}
		if (data.length() == 2) {
			try {
				if (reserve(data)) {
					String pledge = pledges.get(Integer.valueOf(data.substring(0, 1)));
					String user = Users.values()[Integer.valueOf(data.substring(1, 2))].toString();
					message.setText("Okay, hab " + pledge + " für " + user + " reserviert!");
				} else {
					message.setText("Was los mit dir faggot, iss doch schon reserviert");
				}

			} catch (Exception e) {
				e.printStackTrace();
				message.setText("Sorry, hab beim Reservieren verkackt");
				throw e;
			}

		}

	}

	private void showCancelButtons(SendMessage message) throws IOException {
		message.setText("Uuuh man was kannst du eigentlich?\nWelches Gelöbniss willste denn löschen?");
		List<List<InlineKeyboardButton>> buttons = new ArrayList<List<InlineKeyboardButton>>();
		List<PledgeReservation> pList = getReservedPledges();
		if (pList.size() == 0) {
			message.setText("Keine Gelöbnisse zum löschen vorhanden!");
		}
		for (int i = 0; i < pList.size(); i++) {
			InlineKeyboardButton button = new InlineKeyboardButton();
			button.setText(pList.get(i).getName());
			button.setCallbackData("/cancel" + pList.get(i).getName());
			List<InlineKeyboardButton> l = new ArrayList<InlineKeyboardButton>();
			l.add(button);
			buttons.add(l);
		}
		InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
		inlineKeyboardMarkup.setKeyboard(buttons);
		message.setReplyMarkup(inlineKeyboardMarkup);

	}

	private void setReadText(SendMessage message) throws IOException {
		String txt = "Die heutigen Gelöbnisse sind: \n1. " + pledges.get(0);
		List<PledgeReservation> av = new ArrayList<PledgeReservation>();
		av = getReservedPledges();
		for (PledgeReservation pR : av) {
			if (pR.getPledgeOrderNr() == 0)
				txt += " (reserviert von " + Users.values()[pR.getUserNr()] + " mit " + pR.getName() + ")";
		}
		txt += "\n2. " + pledges.get(1);
		for (PledgeReservation pR : av) {
			if (pR.getPledgeOrderNr() == 1)
				txt += " (reserviert von " + Users.values()[pR.getUserNr()] + " mit " + pR.getName() + ")";
		}
		;
		txt += "\n3. " + pledges.get(2);
		for (PledgeReservation pR : av) {
			if (pR.getPledgeOrderNr() == 2)
				txt += " (reserviert von " + Users.values()[pR.getUserNr()] + " mit " + pR.getName() + ")";
		}
		message.setText(txt);
	}

	private void showAvailablePledges(SendMessage message) throws IOException {
		List<PledgeReservation> availablePledges;
		try {
			availablePledges = getReservedPledges();
		} catch (IOException e) {
			message.setText("Sorry hab beim auslesen der Datei verkackt! :(");
			throw e;
		}
		String txt = "Es sind noch keine Gelöbnisse reserviert!";
		if (availablePledges.size() > 0) {
			txt = "Folgendes Gelöbnisse ist reserviert:\n";
			if (availablePledges.size() > 1)
				txt = "Folgende Gelöbnisse sind reserviert:\n";
			for (PledgeReservation p : availablePledges) {
				txt += p.getName() + " für " + Users.values()[p.getUserNr()] + "\n";
			}
		}
		message.setText(txt);

	}

	private boolean reserve(String data) throws IOException {
		int pledgeOrderNr = Integer.valueOf(data.substring(0, 1));
		String name = pledges.get(pledgeOrderNr);
		int userNr = Integer.valueOf(data.substring(1, 2));
		PledgeReservation pR = new PledgeReservation(name, userNr, pledgeOrderNr);
		for (PledgeReservation p : getReservedPledges()) {
			if (p.getName().equals(name))
				return false;
		}
		writeReservation(pR);
		return true;
	}

	private void writeReservation(PledgeReservation pR) throws IOException {
		File f = new File(Config.FILE_PATH);
		if (!f.exists())
			f.createNewFile();
		String txt = pR.toString() + "\n";
		Files.write(Paths.get(Config.FILE_PATH), txt.getBytes(), StandardOpenOption.APPEND);
	}

	private void getAvailableUserButtons(SendMessage message, String pledgeNr) {
		List<List<InlineKeyboardButton>> buttons = new ArrayList<List<InlineKeyboardButton>>();
		for (int i = 0; i < Users.values().length; i++) {
			InlineKeyboardButton button = new InlineKeyboardButton();
			button.setText(Users.values()[i].toString());
			button.setCallbackData("/reserve" + pledgeNr + i);
			List<InlineKeyboardButton> l = new ArrayList<InlineKeyboardButton>();
			l.add(button);
			buttons.add(l);
		}
		InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
		inlineKeyboardMarkup.setKeyboard(buttons);
		message.setText("Mmkay, und für wen willste reservieren?");
		message.setReplyMarkup(inlineKeyboardMarkup);
	}

	private String whichUser() {
		LocalDateTime date = LocalDateTime.now();
		WeekFields weekFields = WeekFields.of(Locale.GERMANY);
		int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
		if (date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY))
			weekNumber += 1;
		weekNumber %= 4;
		return "Nächsten Freitag sind wir bei: " + Users.values()[weekNumber].toString();
	}

	private void getAvailablePledgesButtons(SendMessage message) throws IOException {
		Map<String, Boolean> availablePledges;
		try {
			availablePledges = getAvailablePledges();
			if (!availablePledges.get(pledges.get(0)).booleanValue()
					&& !availablePledges.get(pledges.get(1)).booleanValue()
					&& !availablePledges.get(pledges.get(2)).booleanValue()) {
				message.setText("Es sind bereits 3 Gelöbnisse reserviert, es können keine weiteren reserviert werden!");
				return;
			}
			List<List<InlineKeyboardButton>> buttons = new ArrayList<List<InlineKeyboardButton>>();
			for (int i = 0; i < pledges.size(); i++) {
				if (availablePledges.get(pledges.get(i))) {
					InlineKeyboardButton button = new InlineKeyboardButton();
					button.setText(pledges.get(i));
					button.setCallbackData("/reserve" + i);
					List<InlineKeyboardButton> l = new ArrayList<InlineKeyboardButton>();
					l.add(button);
					buttons.add(l);
				}
			}
			InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
			inlineKeyboardMarkup.setKeyboard(buttons);
			message.setText("Von verfügbaren Gelöbnissen auswählen:");
			message.setReplyMarkup(inlineKeyboardMarkup);
		} catch (IOException e) {
			message.setText("Sorry hab beim auslesen der Datei verkackt! :(");
			e.printStackTrace();
			throw e;
		}

	}

	/**
	 * Gibt eine Map für die heutigen pledges zurück, ob sie verfügbar sind
	 * (true) oder nicht (false)
	 */
	private Map<String, Boolean> getAvailablePledges() throws IOException {
		Map<String, Boolean> pledgesAvailable = new HashMap<String, Boolean>();
		List<PledgeReservation> reserved = getReservedPledges();
		boolean firstReserved = false;
		boolean secondReserved = false;
		boolean thirdReserved = false;
		for (PledgeReservation reservation : reserved) {
			if (reservation.getPledgeOrderNr() == 0)
				firstReserved = true;
			else if (reservation.getPledgeOrderNr() == 1)
				secondReserved = true;
			else if (reservation.getPledgeOrderNr() == 2)
				thirdReserved = true;
		}
		if (firstReserved)
			pledgesAvailable.put(pledges.get(0), false);
		else
			pledgesAvailable.put(pledges.get(0), true);
		if (secondReserved)
			pledgesAvailable.put(pledges.get(1), false);
		else
			pledgesAvailable.put(pledges.get(1), true);
		if (thirdReserved)
			pledgesAvailable.put(pledges.get(2), false);
		else
			pledgesAvailable.put(pledges.get(2), true);
		return pledgesAvailable;
	}

	private List<PledgeReservation> getReservedPledges() throws IOException {
		List<PledgeReservation> ret = new ArrayList<PledgeReservation>();
		File f = new File(Config.FILE_PATH);
		if (!f.exists())
			return ret;
		Stream<String> stream = Files.lines(Paths.get(Config.FILE_PATH));
		Iterator<String> i = stream.iterator();
		while (i.hasNext()) {
			String fields[] = i.next().split(",");
			PledgeReservation p = new PledgeReservation(fields[0], Integer.valueOf(fields[1]),
					Integer.valueOf(fields[2]));
			ret.add(p);
		}
		return ret;
	}

	public String getBotUsername() {
		return "DailyPledgeBot";
	}

	public String getBotToken() {
		return System.getenv("TOKEN");
	}

	private static Command convertCommand(String str) {
		// READ, NONE, SHOW_TODAY, SHOW_RESERVED, WHERE, CANCEL
		if (str.contains(Config.SHOW_TODAY_PLEDGES_COMMAND))
			return Command.READ;
		if (str.contains(Config.SHOW_RESERVED_PLEDGES_COMMAND))
			return Command.SHOW_TODAY;
		if (str.contains(Config.RESERVE_PLEDGE_COMMAND))
			return Command.SHOW_RESERVED;
		if (str.contains(Config.WHERE_COMMAND))
			return Command.WHERE;
		if (str.contains(Config.DELETE_COMMAND))
			return Command.DELETE;
		return Command.NONE;
	}

	private void refreshPledges() {
		LocalDateTime now = LocalDateTime.now();
		if (now.getDayOfMonth() != refreshTimestamp.getDayOfMonth()
				|| now.getHour() > 8 && refreshTimestamp.getHour() <= 8) {
			pledges = WebsiteReader.getPledges(Config.URL);
			refreshTimestamp = now;
		}
	}
}