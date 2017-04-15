package mzeimet.telegram_bot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;

import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import mzeimet.telegram_bot.commands.Available;
import mzeimet.telegram_bot.commands.Reserve;
import mzeimet.telegram_bot.commands.User;

public class PledgeBot extends TelegramLongPollingBot {

	private List<String> pledges;
	private LocalDateTime refreshTimestamp;

	private static enum Command {
		SHOW_AVAILABLE, NONE, SHOW_RESERVED, RESERVE, WHERE, DELETE
	};

	private Timer timer;

	public PledgeBot() {
		refreshTimestamp = LocalDateTime.now().minusDays(1);
		timer = new Timer();
		addCronJobs();
	}

	private void addCronJobs() {
		CronJob cronjob = new ShowTodayCronjob(this);
		timer.schedule(cronjob, cronjob.getSecondsTillNoon() * 1000, Config.secondsPerDay);
	}

	public void onUpdateReceived(Update update) {
		try {
			if (update.hasMessage() && update.getMessage().hasText()) {
				SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId());
				executeCommand(message, update.getMessage().getText());
			} else if (update.hasCallbackQuery()) {
				executeCallback(update.getCallbackQuery());
			}
		} catch (Exception e) {
			sendErrorToMe(e);
		}
	}

	public void executeCommand(SendMessage message, String txtIn) throws IOException {
		if (txtIn.contains("@" + getBotUsername()))
			txtIn = txtIn.replace("@" + getBotUsername(), "");
		Command command = convertCommand(txtIn);
		try {
			switch (command) {
			case SHOW_RESERVED:
				Reserve.showReservedPledges(message);
				break;
			case SHOW_AVAILABLE:
				refreshPledges();
				Available.showAvailablePledges(message, pledges);
				break;
			case RESERVE:
				refreshPledges();
				Available.getAvailablePledgesButtons(message, pledges);
				break;
			case WHERE:
				message.setText(User.whichUser());
				break;
			case DELETE:
				try {
					Reserve.showCancelButtons(message);
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

	private void executeCallback(CallbackQuery query) throws Exception {
		String data = query.getData();
		EditMessageText editText = new EditMessageText();
		editText.setChatId(query.getMessage().getChatId());
		editText.setMessageId(query.getMessage().getMessageId());
		if (data.contains("/reserve")) {
			Reserve.reserveCallback(query, editText, pledges);
		} else if (data.contains("/cancel")) {
			data = data.replace("/cancel", "");
			Reserve.deleteReservation(editText, data);
		} else {
			throw new IllegalArgumentException("Nicht erwartete Callback Query!");
		}
		editMessageText(editText);
		AnswerCallbackQuery q = new AnswerCallbackQuery();
		q.setCallbackQueryId(query.getId());
		answerCallbackQuery(q);
	}

	public String getBotUsername() {
		return "DailyPledgeBot";
	}

	public String getBotToken() {
		return System.getenv("TOKEN");
	}

	private static Command convertCommand(String str) {
		if (str.contains(Config.SHOW_AVAILABLE_PLEDGES_COMMAND))
			return Command.SHOW_AVAILABLE;
		if (str.contains(Config.SHOW_RESERVED_PLEDGES_COMMAND))
			return Command.SHOW_RESERVED;
		if (str.contains(Config.RESERVE_PLEDGE_COMMAND))
			return Command.RESERVE;
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

	private void sendErrorToMe(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String error = LocalDateTime.now() + " :\n" + sw.toString();
		Logger.write(error);
		SendMessage message = new SendMessage().setChatId(Config.MY_CHAT_ID);
		message.setText(error);
		try {
			sendMessage(message);
		} catch (TelegramApiException e1) {
			Logger.write(e.getStackTrace().toString());
			e1.printStackTrace();
		}

	}
}