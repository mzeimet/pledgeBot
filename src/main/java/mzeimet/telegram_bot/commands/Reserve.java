package mzeimet.telegram_bot.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import mzeimet.telegram_bot.Config;
import mzeimet.telegram_bot.PledgeReservation;
import mzeimet.telegram_bot.Config.Users;

public class Reserve {

	public static void deleteReservation(EditMessageText editText, String data) throws Exception {
		try {
			File inputFile = new File(Config.FILE_PATH);
			File tempFile = new File("./tmpReservation.txt");

			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String currentLine;

			while ((currentLine = reader.readLine()) != null) {
				String trimmedLine = currentLine.trim();
				if (trimmedLine.contains(data))
					continue;
				writer.write(currentLine + System.getProperty("line.separator"));
			}
			writer.close();
			reader.close();
			FileUtils.copyFile(tempFile, inputFile);
			FileUtils.forceDelete(tempFile);
		} catch (Exception e) {
			e.printStackTrace();
			editText.setText("Sorry, hab beim Löschen aus der Datei verkackt :(");
			editText.setReplyMarkup(null);
			throw e;
		}
		editText.setText("Jo hab " + data + " gelöscht!");
		editText.setReplyMarkup(null);

	}

	public static void showCancelButtons(SendMessage message) throws IOException {
		List<PledgeReservation> reservedPledges = getReservedPledges();
		message.setText("Uuuh man was kannst du eigentlich?\nWelches Gelöbniss willste denn löschen?");
		List<List<InlineKeyboardButton>> buttons = new ArrayList<List<InlineKeyboardButton>>();
		if (reservedPledges.size() == 0) {
			message.setText("Keine Gelöbnisse zum löschen vorhanden!");
		}
		for (int i = 0; i < reservedPledges.size(); i++) {
			InlineKeyboardButton button = new InlineKeyboardButton();
			button.setText(reservedPledges.get(i).getName());
			button.setCallbackData("/cancel" + reservedPledges.get(i).getName());
			List<InlineKeyboardButton> l = new ArrayList<InlineKeyboardButton>();
			l.add(button);
			buttons.add(l);
		}
		InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
		inlineKeyboardMarkup.setKeyboard(buttons);
		message.setReplyMarkup(inlineKeyboardMarkup);

	}

	public static void reserveCallback(CallbackQuery query, EditMessageText editText, List<String> pledges) throws Exception {
		String data = query.getData().replace("/reserve", "");
		if (data.length() == 1) {
			getAvailableUserButtons(editText, data, pledges);
		}
		if (data.length() == 2) {
			try {
				if (reserve(data, pledges)) {
					String pledge = pledges.get(Integer.valueOf(data.substring(0, 1)));
					String user = Users.values()[Integer.valueOf(data.substring(1, 2))].toString();
					editText.setText("Okay, hab " + pledge + " für " + user + " reserviert!");
				} else {
					editText.setText("Was los mit dir faggot, iss doch schon reserviert");
				}

			} catch (Exception e) {
				e.printStackTrace();
				editText.setText("Sorry, hab beim Reservieren verkackt");
				throw e;
			}

		}

	}

	private static boolean reserve(String data, List<String> pledges) throws IOException {
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

	private static void writeReservation(PledgeReservation pR) throws IOException {
		File f = new File(Config.FILE_PATH);
		if (!f.exists())
			f.createNewFile();
		String txt = pR.toString() + "\n";
		Files.write(Paths.get(Config.FILE_PATH), txt.getBytes(), StandardOpenOption.APPEND);
	}

	public static List<PledgeReservation> getReservedPledges() throws IOException {
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
		stream.close();
		return ret;
	}

	private static void getAvailableUserButtons(EditMessageText editText, String pledgeNr, List<String> pledges) {
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
		editText.setText("Mmkay, und für wen willste " + pledges.get(Integer.valueOf(pledgeNr)) + " reservieren?");
		editText.setReplyMarkup(inlineKeyboardMarkup);
	}
	
	public static void showReservedPledges(SendMessage message) throws IOException {
		List<PledgeReservation> reservedPledges;
		try {
			reservedPledges = Reserve.getReservedPledges();
		} catch (IOException e) {
			message.setText("Sorry hab beim auslesen der Datei verkackt! :(");
			throw e;
		}
		String txt = "Es sind noch keine Gelöbnisse reserviert!";
		if (reservedPledges.size() > 0) {
			txt = "Folgendes Gelöbnisse ist reserviert:\n";
			if (reservedPledges.size() > 1)
				txt = "Folgende Gelöbnisse sind reserviert:\n";
			for (PledgeReservation p : reservedPledges) {
				txt += p.getName() + " für " + Users.values()[p.getUserNr()] + "\n";
			}
		}
		message.setText(txt);
	}
	

}
