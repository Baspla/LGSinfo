package de.baspla.planbot;

import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mapdb.*;
import org.telegram.telegrambots.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mew on 22.05.2017.
 */
public class TheBot extends TelegramLongPollingBot {
    private final String name;
    private final String token;
    private long interval ;
    final IndexTreeList<String> eintraege;
    private final Settings s;
    private boolean boolcaptcha , notify;
    private Timer t;
    IndexTreeList<Long> map;
    private DB db;
    private long ADMIN;
    private String uname, pw;
    private String klasse;
    private HTreeMap<Long, String> captcha;
    private static Log LOG = LogFactory.getLog(TheBot.class.getName());

    public TheBot(String name, String token, String uname, String pw, String klasse, String captcha, String admin,String interval, Settings s) {
        LOG.info("Bot gestartet");
        this.name = name;
        this.token = token;
        this.pw = pw;
        this.uname = uname;
        this.klasse = klasse;
        try{
            this.interval = new Long(interval);}catch(NumberFormatException e){LOG.error("INTERVAL ERROR");this.interval=700000;}
        try{
        this.ADMIN = new Long(admin);}catch(NumberFormatException e){}
        this.s=s;
        this.boolcaptcha = captcha.equals("true");
        db = DBMaker.fileDB("daten.bank").closeOnJvmShutdown().checksumHeaderBypass().make();
        map = db.indexTreeList("schueler", Serializer.LONG).createOrOpen();
        this.captcha = db.hashMap("captcha", Serializer.LONG, Serializer.STRING).createOrOpen();
        eintraege = db.indexTreeList("eintraege", Serializer.STRING).createOrOpen();
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
                if (captcha.containsKey(update.getMessage().getChatId())) {
                    if (update.getMessage().getText().equals(captcha.get(update.getMessage().getChatId()))) {
                        addUser(update.getMessage().getChatId());
                        captcha.remove(update.getMessage().getChatId());
                        send(update.getMessage().getChatId(), "<b>Moin!</b> Alles klar.");
                    } else {
                        send(update.getMessage().getChatId(), "<b>Sorry!</b> Das war falsch.");
                        return;
                    }
                }
                // NACHRICHTEN
                return;
            }
            String txt = update.getMessage().getText();
            if(update.getMessage().isGroupMessage()||update.getMessage().isGroupMessage()){
                if(update.getMessage().getText().toLowerCase().endsWith("@lgsbot")){
                    txt = update.getMessage().getText().toLowerCase().replace("@lgsbot","");
                }
            }
            if (txt.equalsIgnoreCase("/moin")) {
                if (isuser(update.getMessage().getChatId())) {
                    send(update.getMessage().getChatId(), "<b>Moin!</b> Du bist schon dabei.");
                    return;
                }
                addUser(update.getMessage().getChatId());
                send(update.getMessage().getChatId(), "<b>Moin!</b> Alles klar.");
                /*send(update.getMessage().getChatId(), "<b>Moin!</b> Gib bitte dieses Captcha ein.");
                try {
                    sendPhoto(new SendPhoto().setChatId(update.getMessage().getChatId()).setNewPhoto("captcha",
                            newCaptcha(update.getMessage().getChatId())));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    send(update.getMessage().getChatId(), "<b>Moin!</b> Da gabe es einen Fehler...");
                }
                CHAPTA PART
                */

                return;
            }
            if (txt.equalsIgnoreCase("/bye")) {
                if (isuser(update.getMessage().getChatId())) {

                    send(update.getMessage().getChatId(), "Wenn du mich wirklich verlassen willst benutze /byebye");
                    return;
                }
                send(update.getMessage().getChatId(), "<i>Wer bist du?</i>");
                return;
            }
            if (txt.equalsIgnoreCase("/byebye")) {
                if (isuser(update.getMessage().getChatId())) {

                    removeUser(update.getMessage().getChatId());
                    send(update.getMessage().getChatId(), "<b>Bye!</b> Schön dich dabei gehabt zu haben.");

                    return;
                }
                send(update.getMessage().getChatId(), "<i>Wer bist du?</i>");
                return;
            }
            if (txt.equalsIgnoreCase("/start")) {
                if (!isuser(update.getMessage().getChatId())) {
                    send(update.getMessage().getChatId(), "<b>Moin!</b> Melde dich mit /moin an.");
                    return;
                }
                rkey(update.getMessage().getChatId(), "<b>Moin!</b>", new ReplyKeyboardRemove());
                return;
            }
            if (txt.equalsIgnoreCase("/alles")) {
                send(update.getMessage().getChatId(), "Die aktuellen Vertretungen.");
                allon(update.getMessage().getChatId());
                return;
            }
            if (txt.equalsIgnoreCase("/reload")) {
                if (update.getMessage().getChatId() == ADMIN) {
                    eintraege.clear();
                    for (Long id : map) {
                        send(id, "Aus technischen Gründen gibt es ab <b>HIER</b> die aktuellen Vertretungen doppelt. Bei Fragen wenden sie sich bitte an @TimMorgner.");
                    }
                    return;
                }

            }
            if (txt.equalsIgnoreCase("/refresh")) {
                if (update.getMessage().getChatId() == ADMIN) {
                    update();
                    send(update.getMessage().getChatId(), "Update...");
                    return;
                }

            }if (txt.equalsIgnoreCase("/admin")) {
                if (update.getMessage().getChat().getUserName() == "TimMorgner") {
                    ADMIN=update.getMessage().getChatId();
                    s.setAdmin(ADMIN);
                    return;
                }

            }
            if (txt.equalsIgnoreCase("/notify")) {
                if (update.getMessage().getChatId() == ADMIN) {
                    notify=!notify;
                    send(update.getMessage().getChatId(), "Notify: "+notify);
                    return;
                }

            }
            if (txt.equalsIgnoreCase("/stop")) {
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
                    return;
                }
            }
            if (txt.equalsIgnoreCase("/list")) {
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

    private InputStream newCaptcha(Long chatId) {
        BufferedImage image = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random r = new Random(System.currentTimeMillis());
        String code = RandomStringUtils.random(r.nextInt(3) + 5, true, true);
        Font font = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[r
                .nextInt(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts().length)];
        LOG.info("Neues Captcha *" + code + "* für " + chatId + " mit Font " + font.getName() + ".");

        g.setFont(new Font(font.getName(), Font.ITALIC, 50 + r.nextInt(20)));
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.fillOval(r.nextInt(600), r.nextInt(300), 30, 30);
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.fillOval(r.nextInt(600), r.nextInt(300), 30, 30);
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.fillOval(r.nextInt(600), r.nextInt(300), 30, 30);
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.fillOval(r.nextInt(600), r.nextInt(300), 30, 30);
        g.setColor(Color.WHITE);
        g.drawString(code, r.nextInt(300) + 50, r.nextInt(200) + 50);
        g.copyArea(200, 0, 200, 300, 0, 25 - r.nextInt(50));
        g.copyArea(0, 0, 600, 150, 25 - r.nextInt(50), 0);
        g.setStroke(new BasicStroke(r.nextInt(10)));
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.drawLine(r.nextInt(600), 0, r.nextInt(600), 300);
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.drawLine(r.nextInt(600), 0, r.nextInt(600), 300);
        g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        g.drawLine(0, r.nextInt(300), 600, r.nextInt(300));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream fis = new ByteArrayInputStream(os.toByteArray());
        captcha.put(chatId, code);
        return fis;
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

    public void key(long id, String s, InlineKeyboardMarkup inlineKeyboardMarkup) {

        if (s == null)
            return;
        if (s == "")
            return;
        if (s.length() > 4000) {
            key(id, s.substring(0, 4000), inlineKeyboardMarkup);
            key(id, s.substring(4000), inlineKeyboardMarkup);
        }
        SendMessage message = new SendMessage().setChatId(id).setReplyMarkup(inlineKeyboardMarkup).setParseMode("HTML").setText(s);
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
        if(notify)send(ADMIN,"UPDATE!");
        ArrayList<Eintrag> plan = getPlan();
        if (plan == null) {
            LOG.error("Update hat keinen Plan erhalten");
            return;
        }
        for (Eintrag e : plan) {
            if (!inside(eintraege, e)) {
                eintraege.add(e.toString());
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

    private boolean inside(IndexTreeList<String> eintraege2, Eintrag e) {

        for (String ei : eintraege2) {

            if (ei.equals(e.toString()))
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
