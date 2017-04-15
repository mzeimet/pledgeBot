package mzeimet.telegram_bot.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import mzeimet.telegram_bot.PledgeReservation;
import mzeimet.telegram_bot.Config.Users;

public class Available {
	
	public static void showAvailablePledges(SendMessage message, List<String> pledges) throws IOException {
		String txt = "Die heutigen Gelöbnisse sind: \n1. " + pledges.get(0);
		List<PledgeReservation> av = new ArrayList<PledgeReservation>();
		av = Reserve.getReservedPledges();
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

	public static void getAvailablePledgesButtons(SendMessage message, List<String> pledges) throws IOException {
		Map<String, Boolean> availablePledges;
		try {
			availablePledges = getAvailablePledges(pledges);
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
	private static Map<String, Boolean> getAvailablePledges(List<String> pledges) throws IOException {
		Map<String, Boolean> pledgesAvailable = new HashMap<String, Boolean>();
		List<PledgeReservation> reserved = Reserve.getReservedPledges();
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
}
