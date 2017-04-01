package mzeimet.telegram_bot;

import org.telegram.telegrambots.api.methods.send.SendMessage;

public class ShowTodayCronjob extends CronJob {

	public ShowTodayCronjob(PledgeBot bot) {
		super(bot);
	}

	@Override
	public void run() {
		SendMessage message = new SendMessage().setChatId(Config.MY_CHAT_ID);
		bot.executeCommand(message, Config.SHOW_RESERVED_PLEDGES_COMMAND);
		bot.addCronJob(new ShowTodayCronjob(bot));
	}
}
