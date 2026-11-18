/* =====================================================================
 *  WordWindow.java  -  "Your secret word"  = the ROLE LABEL of the brief
 * ---------------------------------------------------------------------
 *  The server sends ROLE:<text> to every player:
 *    insider  : "ROLE:The secret word is: Shawarma"
 *               ->  [food icon]  Topic: Food
 *                                SHAWARMA        (big pixel letters)
 *                                شاورما          (Arabic, if known)
 *    outsider : "ROLE:You are Brra Al-Salfa!"
 *               ->  [?]  Topic: Food
 *                        You are  برا السالفة !
 *  A player who joined during a round only watches (no role).
 * ===================================================================== */

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class WordWindow extends XpWindow {

	public static final int INSIDER = 0, OUTSIDER = 1, WATCHER = 2;

	public WordWindow(int role, String word, String wordArabic, String topic) {
		super(Lang.t("secret_word"), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(16, 18, 14, 18));

		// ---- the picture on the left ----
		JComponent picture;
		if (role == OUTSIDER) {
			picture = new Avatar.Portrait(null, 100, true);       // null = the "?" face
		} else {
			// the topic icon in a small white square
			JLabel icon = new JLabel(Xp.pixelIcon(Xp.topicIcon(topic), 5), JLabel.CENTER);
			icon.setOpaque(true);
			icon.setBackground(Color.WHITE);
			icon.setBorder(BorderFactory.createLineBorder(new Color(0xA9BCE3), 2));
			icon.setPreferredSize(new Dimension(72, 72));
			picture = icon;
		}
		JPanel left = Xp.clear(new BorderLayout());
		left.add(picture, BorderLayout.NORTH);
		left.setBorder(Lang.arabic() ? new EmptyBorder(0, 22, 0, 0) : new EmptyBorder(0, 0, 0, 22));   // the gap faces the text
		body().add(left, BorderLayout.LINE_START);

		// ---- the text on the right ----
		JPanel text = Xp.clear(null);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(Xp.label(Lang.f("topic_is", Lang.name(topic)), Xp.PIXEL, 15f, Xp.BLUE_TEXT));

		if (role == INSIDER) {
			// the word in big pixel letters; a long word gets a smaller size
			String upper = word.toUpperCase();
			float size = 44f;
			JLabel big = Xp.label(upper, Xp.WORD, size, Xp.INK);
			while (size > 16f && big.getFontMetrics(big.getFont()).stringWidth(upper) > 540) {
				size = size - 1f;                  // smaller until it fits next to the picture
				big.setFont(Xp.fontFor(upper, Xp.WORD, size));
			}
			big.setBorder(new EmptyBorder(6, 0, 4, 0));
			text.add(big);
			if (wordArabic.length() > 0) {
				text.add(Xp.label(wordArabic, Xp.ARABIC, 28f, Xp.ORANGE_DARK));
			}
			text.add(Box.createVerticalStrut(10));
			text.add(Xp.label(Lang.t("dont_say"), Xp.MONO, 15f, new Color(0x3A3A3A)));
		} else if (role == OUTSIDER) {
			// "You are  برا السالفة !"
			JPanel line = Xp.clear(new FlowLayout(FlowLayout.LEADING, 0, 0));
			line.add(Xp.label(Lang.t("you_are") + "  ", Xp.PIXEL, 38f, Xp.INK));
			line.add(new JLabel(Xp.pixelText("برا السالفة", 20, 3, Xp.ORANGE, Xp.ORANGE_DARK)));
			line.add(Xp.label(" !", Xp.PIXEL, 38f, Xp.INK));
			line.setAlignmentX(Xp.start());
			text.add(line);
			text.add(Box.createVerticalStrut(8));
			text.add(Xp.label(Lang.t("outsider_text"), Xp.MONO, 15f, new Color(0x3A3A3A)));
			text.add(Xp.label(Lang.t("outsider_text2"), Xp.MONO, 15f, new Color(0x3A3A3A)));
		} else {
			text.add(Box.createVerticalStrut(14));
			text.add(Xp.label(Lang.t("watching"), Xp.PIXEL, 20f, Xp.INK));
		}
		XpParts.leftAlign(text);
		body().add(text, BorderLayout.CENTER);
	}
}
