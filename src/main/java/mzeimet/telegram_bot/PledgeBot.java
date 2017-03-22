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
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class PledgeBot extends TelegramLongPollingBot {

	private static final String SHOW_TODAY_PLEDGES_COMMAND = "/showtoday";
	private static final String SHOW_RESERVED_PLEDGES_COMMAND = "/showreserved";
	private static final String RESERVE_PLEDGE_COMMAND = "/reserve";
	private static final String WHERE_COMMAND = "/where";
	private static final String DELETE_COMMAND = "/delete";
	private static final String url = "https://esoleaderboards.com/widgets/pledgeometer.php/";
	private static final String ERROR_INTERPRET_COMMAND = "Beim Parsen des Befehls ist ein Fehler aufgetreten. Wadafaq.";
	private static final String FILE_PATH = "./reservation.txt";
	private List<String> pledges;
	private LocalDateTime refreshTimestamp;

	private static enum Command {
		READ, NONE, SHOW_TODAY, SHOW_RESERVED, WHERE, CANCEL
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
			if (txtIn.contains("@" + getBotUsername()))
				txtIn = txtIn.replace("@" + getBotUsername(), "");
			Command command = convertCommand(txtIn);
			SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId());
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
			case CANCEL:
				try {
					showCancelButtons(message);
				} catch (IOException e1) {
					message.setText("Sorry, hab beim Lesen der Gelöbnisse verkackt :(");
					e1.printStackTrace();
				}
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
		if (update.hasCallbackQuery()) {
			String data = update.getCallbackQuery().getData();
			SendMessage message = new SendMessage().setChatId(update.getCallbackQuery().getMessage().getChatId());
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
				q.setCallbackQueryId(update.getCallbackQuery().getId());
				answerCallbackQuery(q);
			} catch (TelegramApiException e) {
				e.printStackTrace();
			}

		}
	}

	private void deleteReservation(SendMessage message, String pledgeName) {
		try {
			File inputFile = new File(FILE_PATH);
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
			try {
				FileUtils.copyFile(tempFile, inputFile);
				FileUtils.forceDelete(tempFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			message.setText("Sorry, hab beim Löschen aus der Datei verkackt :(");
		}
		message.setText("Jo habs gelöscht!");

	}

	private void reserveCallback(SendMessage message, String data) {
		data = data.replace("/reserve", "");
		if (data.length() == 1) {
			getAvailableUserButtons(message, data);
		}
		if (data.length() == 2) {
			try {
				reserve(data);
				String pledge = pledges.get(Integer.valueOf(data.substring(0, 1)));
				String user = Users.values()[Integer.valueOf(data.substring(1, 2))].toString();
				message.setText("Okay, hab " + pledge + " für " + user + " reserviert!");
			} catch (Exception e) {
				e.printStackTrace();
				message.setText("Sorry, hab beim Reservieren verkackt");
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

	private void setReadText(SendMessage message) {
		String txt = "Die heutigen Gelöbnisse sind: \n1. " + pledges.get(0);
		List<PledgeReservation> av = new ArrayList<PledgeReservation>();
		try {
			av = getReservedPledges();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	private void showAvailablePledges(SendMessage message) {
		List<PledgeReservation> availablePledges;
		try {
			availablePledges = getReservedPledges();
		} catch (IOException e) {
			message.setText("Sorry hab beim auslesen der Datei verkackt! :(");
			return;
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

	private void reserve(String data) throws IOException {
		int pledgeOrderNr = Integer.valueOf(data.substring(0, 1));
		String name = pledges.get(pledgeOrderNr);
		int userNr = Integer.valueOf(data.substring(1, 2));
		PledgeReservation pR = new PledgeReservation(name, userNr, pledgeOrderNr);
		writeReservation(pR);
	}

	private void writeReservation(PledgeReservation pR) throws IOException {
		File f = new File(FILE_PATH);
		if (!f.exists())
			f.createNewFile();
		String txt = pR.toString() + "\n";
		Files.write(Paths.get(FILE_PATH), txt.getBytes(), StandardOpenOption.APPEND);
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

	private void getAvailablePledgesButtons(SendMessage message) {
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
		}

	}

	private Map<String, Boolean> getAvailablePledges() throws IOException {
		Map<String, Boolean> notReservedPledges = new HashMap<String, Boolean>();
		for (String s : pledges) {
			notReservedPledges.put(s, true);
		}
		List<PledgeReservation> reserved = getReservedPledges();
		for (int j = notReservedPledges.size() - 1; j >= 0; j--) {
			for (PledgeReservation p : reserved) {
				if (p.getName().equals(pledges.get(j))) {
					notReservedPledges.remove(pledges.get(j));
					notReservedPledges.put(pledges.get(j), false);
				}
			}
		}
		return notReservedPledges;
	}

	private List<PledgeReservation> getReservedPledges() throws IOException {
		List<PledgeReservation> ret = new ArrayList<PledgeReservation>();
		File f = new File(FILE_PATH);
		if (!f.exists())
			return ret;
		Stream<String> stream = Files.lines(Paths.get(FILE_PATH));
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
		if (str.contains(SHOW_TODAY_PLEDGES_COMMAND))
			return Command.READ;
		if (str.contains(SHOW_RESERVED_PLEDGES_COMMAND))
			return Command.SHOW_TODAY;
		if (str.contains(RESERVE_PLEDGE_COMMAND))
			return Command.SHOW_RESERVED;
		if (str.contains(WHERE_COMMAND))
			return Command.WHERE;
		if (str.contains(DELETE_COMMAND))
			return Command.CANCEL;
		return Command.NONE;
	}

	private void refreshPledges() {
		LocalDateTime now = LocalDateTime.now();
		if (now.getDayOfMonth() != refreshTimestamp.getDayOfMonth()
				|| now.getHour() > 8 && refreshTimestamp.getHour() <= 8) {
			pledges = WebsiteReader.getPledges(url);
			refreshTimestamp = now;
		}
	}
}