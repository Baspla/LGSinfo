package de.baspla.planbot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mew on 22.05.2017.
 */
public class TheBot extends TelegramLongPollingBot {
	private final String name;
	private final String token;
	private final Notify notify;

	public TheBot(String name, String token, String uname, String pw, String klasse) {
		this.name = name;
		this.token = token;
		this.notify = new Notify(this, uname, pw, klasse);

	}
	
	public void onUpdateReceived(Update update) {
		// We check if the update has a message and the message has text
		if (update.hasMessage() && update.getMessage().hasText()) {
			if (!update.getMessage().isCommand()) {
				if (update.getMessage().getText().equalsIgnoreCase("Anmelden")) {
					if (notify.isuser(update.getMessage().getChatId())) {
						send(update.getMessage().getChatId(), "<b>Moin!</b> Du bist schon dabei.");
						return;
					}
					notify.addUser(update.getMessage().getChatId());
					send(update.getMessage().getChatId(), "<b>Moin!</b> Alles klar.");
					return;
				}
				send(update.getMessage().getChatId(), "Es gibt:\n<i>/moin\n/bye\n/alles</i>");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/moin")) {
				if (notify.isuser(update.getMessage().getChatId())) {
					send(update.getMessage().getChatId(), "<b>Moin!</b> Du bist schon dabei.");
					return;
				}
				notify.addUser(update.getMessage().getChatId());
				send(update.getMessage().getChatId(), "<b>Moin!</b> Alles klar.");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/bye")) {
				if (notify.isuser(update.getMessage().getChatId())) {

					send(update.getMessage().getChatId(), "Wenn du mich wirklich verlassen willst benutze /byebye");
					return;
				}
				send(update.getMessage().getChatId(), "<i>Wer bist du?</i>");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/byebye")) {
				if (notify.isuser(update.getMessage().getChatId())) {

					notify.removeUser(update.getMessage().getChatId());
					send(update.getMessage().getChatId(), "<b>Bye!</b> Schoen dich dabei gehabt zu haben.");

					return;
				}
				send(update.getMessage().getChatId(), "<i>Wer bist du?</i>");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/start")) {
				if (!notify.isuser(update.getMessage().getChatId())) {
					List<KeyboardRow> rows = new ArrayList<KeyboardRow>();
					KeyboardRow row = new KeyboardRow();
					row.add("Anmelden");
					rows.add(row);
					ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup().setOneTimeKeyboard(true).setKeyboard(rows);
					key(update.getMessage().getChatId(), "<b>Moin!</b> Melde dich mit /moin oder dem Knopf an.",
							markup);
					return;
				}
				rkey(update.getMessage().getChatId(), "<b>Moin!</b>", new ReplyKeyboardRemove());
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/alles")) {
				send(update.getMessage().getChatId(), "Die aktuellen Vertretungen.");
				notify.allon(update.getMessage().getChatId());
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/reload")) {
				if (update.getMessage().getChatId() == 67025299) {
					notify.eintraege.clear();
					for (Long id : notify.map) {
						send(id, "Aus technischen Gruenden gibt es ab <b>HIER</b> die aktuellen Vertretungen doppelt. Bei Fragen wenden sie sich bitte an @TimMorgner.");
					}
					return;
				}

			}
			if (update.getMessage().getText().equalsIgnoreCase("/refresh")) {
				if (update.getMessage().getChatId() == 67025299) {
					notify.update();
					send(update.getMessage().getChatId(), "Update...");
					return;
				}

			}
			if (update.getMessage().getText().equalsIgnoreCase("/stop")) {
				if (update.getMessage().getChatId() == 67025299) {
					send(update.getMessage().getChatId(), "Goodbye!");
					notify.close();
					System.exit(0);
					return;
				}
			}
			send(update.getMessage().getChatId(), "Diesen Befehl gibt es nicht.");
			return;
		}
	}

	public void send(long id, String s) {
		if (s == null)
			return;
		if (s == "")
			return;
		if (s.length() > 4000) {
			send(id, s.substring(0, 4000));
			send(id, s.substring(4000));
		}
		SendMessage message = new SendMessage().setChatId(id).setParseMode("HTML").setText(s);
		try {
			sendMessage(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	public void key(long id, String s, ReplyKeyboardMarkup markup) {

		if (s == null)
			return;
		if (s == "")
			return;
		if (s.length() > 4000) {
			key(id, s.substring(0, 4000), markup);
			key(id, s.substring(4000), markup);
		}
		SendMessage message = new SendMessage().setChatId(id).setReplyMarkup(markup).setParseMode("HTML").setText(s);
		try {
			sendMessage(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	public void rkey(long id, String s, ReplyKeyboardRemove markup) {

		if (s == null)
			return;
		if (s == "")
			return;
		if (s.length() > 4000) {
			rkey(id, s.substring(0, 4000), markup);
			rkey(id, s.substring(4000), markup);
		}
		SendMessage message = new SendMessage().setChatId(id).setReplyMarkup(markup).setParseMode("HTML").setText(s);
		try {
			sendMessage(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	public String getBotUsername() {
		return name;
	}

	public String getBotToken() {
		return token;
	}
}
