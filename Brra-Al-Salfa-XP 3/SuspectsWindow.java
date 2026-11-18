/* =====================================================================
 *  SuspectsWindow.java  -  "Suspects": every player of the round
 * ---------------------------------------------------------------------
 *  Turn order (bonus): the server sends ORDER:Meryem,Nouri,Fahad,Sara.
 *  The question time is shared equally, so SpyClient can work out from
 *  the TIMER who is asking now, and calls setAsking(name):
 *  that player gets the orange frame and "asking now" under the name.
 * ===================================================================== */

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SuspectsWindow extends XpWindow {

	private final HashMap<String, XpParts.PlayerCard> cards = new HashMap<String, XpParts.PlayerCard>();

	public SuspectsWindow(Vector<String> players, HashMap<String, Avatar> avatars, String myName) {
		super(Lang.t("suspects"), Xp.pixelIcon(Xp.JOBS, 2));
		JPanel box = XpParts.listBox(new GridLayout(1, players.size(), 8, 0));
		box.setBorder(BorderFactory.createCompoundBorder(Xp.whiteBox(), new EmptyBorder(14, 10, 10, 10)));
		// a smaller face when there are many players
		int size = players.size() <= 4 ? 90 : players.size() <= 6 ? 70 : 56;
		for (int i = 0; i < players.size(); i++) {
			String name = players.elementAt(i);
			String shown = name.equals(myName) ? name + " (" + Lang.t("you") + ")" : name;
			XpParts.PlayerCard card = new XpParts.PlayerCard(avatars.get(name), shown, size);
			cards.put(name, card);
			box.add(card);
		}
		body().add(box, BorderLayout.CENTER);
	}

	// orange frame + "asking now" for this player only
	public void setAsking(String asker) {
		for (String name : cards.keySet()) {
			XpParts.PlayerCard card = cards.get(name);
			boolean on = name.equals(asker);
			card.setHighlight(on);
			card.setSub(on ? Lang.t("asking_now") : "");
		}
	}
}
