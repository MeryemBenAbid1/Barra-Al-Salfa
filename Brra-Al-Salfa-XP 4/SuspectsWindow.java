/* =====================================================================
 *  SuspectsWindow.java  -  "Suspects": every player of the round
 * ---------------------------------------------------------------------
 *  Question turns (bonus): one question at a time, around a circle.
 *  The server sends  TURN:3/8|Meryem|Nouri  = question 3 of 8,
 *  Meryem asks Nouri. SpyClient then calls setTurn(...):
 *   - the line on top says what to do now ("Ask Nouri one question")
 *   - the asker gets the orange frame and "asking",
 *     the answerer gets "answering"
 * ===================================================================== */

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SuspectsWindow extends XpWindow {

	private final HashMap<String, XpParts.PlayerCard> cards = new HashMap<String, XpParts.PlayerCard>();
	private final JLabel headline = new JLabel(" ", JLabel.CENTER);

	public SuspectsWindow(Vector<String> players, HashMap<String, Avatar> avatars, String myName) {
		super(Lang.t("suspects"), Xp.pixelIcon(Xp.JOBS, 2));
		// the line on top: what to do now
		headline.setBorder(new EmptyBorder(0, 0, 4, 0));
		body().add(headline, BorderLayout.NORTH);

		JPanel box = XpParts.listBox(new GridLayout(1, players.size(), 8, 0));
		box.setBorder(BorderFactory.createCompoundBorder(Xp.whiteBox(), new EmptyBorder(6, 10, 4, 10)));
		// a smaller face when there are many players
		int size = players.size() <= 4 ? 72 : players.size() <= 6 ? 60 : 50;
		for (int i = 0; i < players.size(); i++) {
			String name = players.elementAt(i);
			String shown = name.equals(myName) ? name + " (" + Lang.t("you") + ")" : name;
			XpParts.PlayerCard card = new XpParts.PlayerCard(avatars.get(name), shown, size);
			cards.put(name, card);
			box.add(card);
		}
		body().add(box, BorderLayout.CENTER);
		setTurn(null, null, myName, 0, 0);
	}

	// asker asks answerer: question q of total (asker = null: no question yet)
	public void setTurn(String asker, String answerer, String myName, int q, int total) {
		// 1. the line on top: what do I do now?
		String text;
		boolean me = false;
		if (asker == null) {
			text = " ";
		} else if (asker.equals(myName)) {
			text = Lang.f("turn_ask", answerer);          // "Ask Nouri one question in the chat."
			me = true;
		} else if (answerer.equals(myName)) {
			text = Lang.f("turn_answer", asker);          // "Meryem is asking you. Answer in the chat!"
			me = true;
		} else {
			text = Lang.f("tv_asking", asker, answerer);  // "Meryem is asking Nouri..."
		}
		if (q > 0) {
			text = Lang.f("question_of", q, total) + "  ·  " + text;
		}
		headline.setText(Xp.rtlLine(text));
		headline.setFont(Xp.fontFor(text, Xp.PIXEL, 16f));
		headline.setForeground(me ? Xp.ORANGE_DARK : Xp.INK);

		// 2. the faces: orange frame = asking, "answering" under the other one
		for (String name : cards.keySet()) {
			XpParts.PlayerCard card = cards.get(name);
			card.setHighlight(name.equals(asker));
			if (name.equals(asker)) {
				card.setSub(Lang.t("asking"));
			} else if (name.equals(answerer)) {
				card.setSub(Lang.t("answering"));
			} else {
				card.setSub("");
			}
		}
	}
}
