package mzeimet.telegram_bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class App {	
	public static void main(String[] args) {
		ApiContextInitializer.init();

		TelegramBotsApi botsApi = new TelegramBotsApi();

		try {
			botsApi.registerBot(new PledgeBot());
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
