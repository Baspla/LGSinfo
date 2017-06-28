package de.baspla.planbot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Settings {
	Properties properties;
	Properties defaultProperties;

	public Settings() {
		properties = new Properties(defaultProperties);
		properties.setProperty("botname", "");
		properties.setProperty("bottoken", "");
		properties.setProperty("name", "");
		properties.setProperty("passwort", "");
		properties.setProperty("captcha", "true");
        properties.setProperty("admin", "");
        properties.setProperty("interval", "1800000");
		properties.setProperty("klasse", "13BG");
		try {
			InputStream input = new FileInputStream("config.properties");
			properties.load(input);
		} catch (IOException e) {
			try {
				OutputStream outputStream = new FileOutputStream("config.properties");
				properties.store(outputStream, "LGSBot");
			} catch (IOException e1) {
				System.exit(-1);
			}
		}
	}

	public String getBotname() {
		return properties.getProperty("botname");
	}

	public String getBottoken() {
		return properties.getProperty("bottoken");
	}

	public String getKlasse() {
		return properties.getProperty("klasse");
	}

	public String getName() {
		return properties.getProperty("name");
	}

	public String getPasswort() {
        return properties.getProperty("passwort");
    }
    public String getAdmin() {
        return properties.getProperty("admin");
    }
    public String getCaptcha() {
        return properties.getProperty("captcha");
    }
    public String getInterval() {
        return properties.getProperty("captcha");
    }
	public Properties getProperties() {
		return properties;
	}

    public void setAdmin(long admin) {
	    properties.setProperty("admin",new Long(admin).toString());
        try {
            InputStream input = new FileInputStream("config.properties");
            properties.load(input);
        } catch (IOException e) {
            try {
                OutputStream outputStream = new FileOutputStream("config.properties");
                properties.store(outputStream, "LGSBot");
            } catch (IOException e1) {
                System.exit(-1);
            }
        }
    }
}
