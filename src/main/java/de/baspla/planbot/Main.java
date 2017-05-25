package de.baspla.planbot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;

/**
 * Created by Mew on 22.05.2017.
 */
public class Main {

	private static Log LOG = LogFactory.getLog(Main.class.getName());

	public static void main(String[] args) {

		ApiContextInitializer.init();

		TelegramBotsApi botsApi = new TelegramBotsApi();

		Settings s = new Settings();

		try {
			TheBot bot = new TheBot(s.getBotname(), s.getBottoken(), s.getName(), s.getPasswort(), s.getKlasse());
			botsApi.registerBot(bot);
			bot.start();
		} catch (TelegramApiException e) {
			LOG.error(e);
		}
	}

	public static String connect(String url, String name, String pw) {
		
		try {
			UnsafeSSLHelp unsafeSSLHelp = new UnsafeSSLHelp();
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(name, pw));
			@SuppressWarnings("deprecation")
			CloseableHttpClient httpclient = HttpClientBuilder.create()
					.setDefaultCredentialsProvider(credentialsProvider)
					.setSslcontext(unsafeSSLHelp.createUnsecureSSLContext())
					.setHostnameVerifier(unsafeSSLHelp.getPassiveX509HostnameVerifier()).build();
			HttpGet httpGet = new HttpGet(url);
			CloseableHttpResponse response = null;
			response = httpclient.execute(httpGet);
			String content = null;
			try {
				// System.out.println(response.getStatusLine());
				HttpEntity entity1 = response.getEntity();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				entity1.writeTo(outputStream);
				content = outputStream.toString("UTF-8");
				EntityUtils.consume(entity1);
			} finally {
				response.close();
			}
			return content;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
