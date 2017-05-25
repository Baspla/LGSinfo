package de.baspla.planbot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;
import org.telegram.telegrambots.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.vdurmont.emoji.EmojiParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mew on 22.05.2017.
 */
public class TheBot extends TelegramLongPollingBot {
	private final String name;
	private final String token;
	private final static long interval = 300000;
	final IndexTreeList<Eintrag> eintraege;
	private Timer t;
	IndexTreeList<Long> map;
	private DB db;
	private long ADMIN = 67025299;
	private String uname, pw;
	private String klasse;
	private static Log LOG = LogFactory.getLog(TheBot.class.getName());

	@SuppressWarnings("unchecked")
	public TheBot(String name, String token, String uname, String pw, String klasse) {
		LOG.info("Bot gestartet");
		this.name = name;
		this.token = token;
		this.pw = pw;
		this.uname = uname;
		this.klasse = klasse;
		db = DBMaker.fileDB("daten.bank").closeOnJvmShutdown().checksumHeaderBypass().make();
		map = db.indexTreeList("schueler", Serializer.LONG).createOrOpen();
		eintraege = (IndexTreeList<Eintrag>) db.indexTreeList("eintraege", Serializer.JAVA).createOrOpen();
	}

	public void start() {
		t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				LOG.info("Beginne Update");
				update();
				LOG.info("Updates an " + map.size() + " User gesendet.");

			}
		}, 4000, interval);
	}

	public void onUpdateReceived(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {

			if (!update.getMessage().isCommand()) {
				if (update.getMessage().getText().equalsIgnoreCase("Anmelden")) {
					if (isuser(update.getMessage().getChatId())) {
						send(update.getMessage().getChatId(), "<b>Moin!</b> Du bist schon dabei.");
						return;
					}
					addUser(update.getMessage().getChatId());
					send(update.getMessage().getChatId(), "<b>Moin!</b> Alles klar.");
					return;
				}
				send(update.getMessage().getChatId(), "Es gibt:\n<i>/moin\n/bye\n/alles</i>");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/moin")) {
				if (isuser(update.getMessage().getChatId())) {
					send(update.getMessage().getChatId(), "<b>Moin!</b> Du bist schon dabei.");
					return;
				}
				addUser(update.getMessage().getChatId());
				send(update.getMessage().getChatId(), "<b>Moin!</b> Alles klar.");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/bye")) {
				if (isuser(update.getMessage().getChatId())) {

					send(update.getMessage().getChatId(), "Wenn du mich wirklich verlassen willst benutze /byebye");
					return;
				}
				send(update.getMessage().getChatId(), "<i>Wer bist du?</i>");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/byebye")) {
				if (isuser(update.getMessage().getChatId())) {

					removeUser(update.getMessage().getChatId());
					send(update.getMessage().getChatId(), "<b>Bye!</b> Schön dich dabei gehabt zu haben.");

					return;
				}
				send(update.getMessage().getChatId(), "<i>Wer bist du?</i>");
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/start")) {
				if (!isuser(update.getMessage().getChatId())) {
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
				allon(update.getMessage().getChatId());
				return;
			}
			if (update.getMessage().getText().equalsIgnoreCase("/reload")) {
				if (update.getMessage().getChatId() == ADMIN) {
					eintraege.clear();
					for (Long id : map) {
						send(id, "Aus technischen Gründen gibt es ab <b>HIER</b> die aktuellen Vertretungen doppelt. Bei Fragen wenden sie sich bitte an @TimMorgner.");
					}
					return;
				}

			}
			if (update.getMessage().getText().equalsIgnoreCase("/refresh")) {
				if (update.getMessage().getChatId() == ADMIN) {
					update();
					send(update.getMessage().getChatId(), "Update...");
					return;
				}

			}
			if (update.getMessage().getText().equalsIgnoreCase("/stop")) {
				if (update.getMessage().getChatId() == ADMIN) {
					send(update.getMessage().getChatId(), "Goodbye!");
					close();
					System.exit(0);
					return;
				}
			}
			if (update.getMessage().getText().startsWith("/msg")) {
				if (update.getMessage().getChatId() == ADMIN) {
					String args = update.getMessage().getText().substring(5);
					String id = args.substring(0, args.indexOf(" "));
					String text = args.substring(1 + args.indexOf(" "));
					try {
						send(new Long(id), text);
					} catch (NumberFormatException e) {
						send(update.getMessage().getChatId(), e.toString());
					}
				}
			}
			if (update.getMessage().getText().equalsIgnoreCase("/list")) {
				if (update.getMessage().getChatId() == ADMIN) {
					for (Long lo : map) {
						Chat c = null;
						try {
							c = sendApiMethod(new GetChat().setChatId(lo));
							send(update.getMessage().getChatId(), lo + "  " + c.getFirstName() + " " + c.getLastName()
									+ " @" + c.getUserName() + " Gruppe:" + c.getTitle());
						} catch (TelegramApiException e) {
						}

					}
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
		s = EmojiParser.parseToUnicode(s);
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

	void update() {
		ArrayList<Eintrag> plan = getPlan();
		if (plan == null) {
			LOG.error("Update hat keinen Plan erhalten");
		}
		for (Eintrag e : plan) {
			if (!inside(eintraege, e)) {
				eintraege.add(e);
				LOG.info("Neuer Eintrag");
				for (Long id : map) {
					send(id, e.toString());
				}
			}

		}
	}

	void allon(long id) {
		for (Eintrag e : getPlan()) {
			{
				send(id, e.toString());

			}
		}

	}

	private boolean inside(IndexTreeList<Eintrag> eintraege, Eintrag e) {

		for (Eintrag ei : eintraege) {

			if (ei.eq(e))
				return true;
		}
		return false;
	}

	private ArrayList<Eintrag> getPlan() {
		ArrayList<Eintrag> out = new ArrayList<Eintrag>();
		String url = getPlanUrl();
		if (url == null || url == "") {
			LOG.error("URL ist leer");
			return null;
		}

		Document doc = Jsoup.parse(Main.connect("https://lgsit.de/plan/vertretungsplan/" + url, uname, pw));
		Elements elements = doc.getElementsByTag("tbody");
		if (elements.isEmpty()) {
			LOG.error("Keine Elemente");
			return null;
		}
		Elements eintraege = elements.get(0).getElementsByTag("tr");
		for (Element ein : eintraege) {
			Elements attr = ein.getElementsByTag("td");
			Eintrag leiste = new Eintrag();
			for (Element ele : attr) {
				leiste.fill(ele.text());
			}
			out.add(leiste);
		}
		return out;
	}

	private String getPlanUrl() {
		String site = Main.connect("https://lgsit.de/plan/vertretungsplan", uname, pw);
		Document doc = Jsoup.parse(site);
		Elements elements = doc.getElementsContainingOwnText(klasse);
		if (elements.isEmpty()) {
			LOG.error("Kein Element mit " + klasse);
			return null;
		}
		return elements.get(0).attr("href");
	}

	public void close() {
		t.cancel();
		db.close();
	}

	public void addUser(Long chatId) {

		LOG.info(chatId + " hinzugefügt.");
		map.add(chatId);
	}

	public boolean isuser(Long chatId) {
		return (map.contains(chatId));
	}

	public void removeUser(Long chatId) {
		map.remove(map.indexOf(chatId));
	}

	public String getBotUsername() {
		return name;
	}

	public String getBotToken() {
		return token;
	}
}
