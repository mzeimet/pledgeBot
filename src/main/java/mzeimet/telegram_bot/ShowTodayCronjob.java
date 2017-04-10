package mzeimet.telegram_bot;

import java.io.IOException;

import org.telegram.telegrambots.api.methods.send.SendMessage;

public class ShowTodayCronjob extends CronJob {

	public ShowTodayCronjob(PledgeBot bot) {
		super(bot);
	}

	@Override
	public void run() {
		SendMessage message = new SendMessage().setChatId(Config.MY_CHAT_ID);
		try {
			bot.executeCommand(message, Config.SHOW_TODAY_PLEDGES_COMMAND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
