package de.baspla.planbot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mew on 22.05.2017.
 */
public class Notify {
	private final TheBot bot;
	private final static long interval = 300000;
	final IndexTreeList<Eintrag> eintraege;
	private Timer t;
	IndexTreeList<Long> map;
	private DB db;
	private String name, pw;
	private String klasse;

	@SuppressWarnings("unchecked")
	public Notify(TheBot bot, String name, String pw, String klasse) {
		this.bot = bot;
		this.pw = pw;
		this.name = name;
		this.klasse = klasse;
		db = DBMaker.fileDB("daten.bank").closeOnJvmShutdown().checksumHeaderBypass().make();
		map = db.indexTreeList("schueler", Serializer.LONG).createOrOpen();
		eintraege = (IndexTreeList<Eintrag>) db.indexTreeList("eintraege", Serializer.JAVA).createOrOpen();
		t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				update();
				System.out.println("Benachrichtige " + map.size() + " Personen");

			}
		}, 4000, interval);
	}

	void update() {
		for (Eintrag e : getPlan()) {
			if (!inside(eintraege, e)) {
				eintraege.add(e);
				System.out.println("Neuer Eintrag");
				for (Long id : map) {
					bot.send(id, e.toString());
				}
			}

		}
	}

	void allon(long id) {
		for (Eintrag e : getPlan()) {
			{
				bot.send(id, e.toString());

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
			return null;
		}

		Document doc = Jsoup.parse(Main.connect("https://lgsit.de/plan/vertretungsplan/" + url, name, pw));
		Elements elements = doc.getElementsByTag("tbody");
		if (elements.isEmpty()) {
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
		Document doc = Jsoup.parse(Main.connect("https://lgsit.de/plan/vertretungsplan", name, pw));
		Elements elements = doc.getElementsContainingOwnText(klasse);
		if (elements.isEmpty()) {
			return null;
		}
		return elements.get(0).attr("href");
	}

	public void close() {
		t.cancel();
		db.close();
	}

	public void addUser(Long chatId) {

		System.out.println(chatId + " hinzugefuegt.");
		map.add(chatId);
	}

	public boolean isuser(Long chatId) {
		return (map.contains(chatId));
	}

	public void removeUser(Long chatId) {
		map.remove(map.indexOf(chatId));
	}
}
