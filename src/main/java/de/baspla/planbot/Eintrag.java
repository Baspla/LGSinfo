package de.baspla.planbot;

import java.io.Serializable;

/**
 * Created by Mew on 22.05.2017.
 */
public class Eintrag implements Serializable {
	private static final long serialVersionUID = 1337L;

	private String datum, tag, klasse, stunde, fach, vlehrer, vraum, vfach, lehrer, info, art;
	private int i = 0;

	public void fill(String text) {
		if (text == "" || text == "" || text == null)
			text = "---";
		switch (i) {
		case 0:
			datum = text;
			break;
		case 1:
			tag = text;
			break;
		case 2:
			klasse = text;
			break;
		case 3:
			stunde = text;
			break;
		case 4:
			fach = text;
			break;
		case 5:
			vlehrer = text;
			break;
		case 6:
			vraum = text;
			break;
		case 7:
			vfach = text;
			break;
		case 8:
			lehrer = text;
			break;
		case 9:
			info = text;
			break;
		case 10:
			art = text;
			break;
		default:
			break;

		}
		i++;
	}

	public boolean eq(Eintrag o) {
		if (toString().equals(o.toString()))
			return true;
		return false;
	}

	@Override
	public String toString() {
		return "<code>" + tag + " " + datum + "</code> | " + klasse + " | <b>" + stunde + " Stunde</b> | " + fach + ", "
				+ vlehrer + ", " + vraum + " | " + vfach + ", " + lehrer + " | Info: <i>" + info + " </i>|<i> " + art
				+ "</i>";
	}
}
