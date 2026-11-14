/* =====================================================================
 *  RoundScreen.java  -  screen 2: questions (and the last-chance guess)
 * ---------------------------------------------------------------------
 *   START  secret card, a one-line status, the suspects
 *   END    chat ("INTERCEPTED")
 * ===================================================================== */

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.CompoundBorder;

public class RoundScreen extends JPanel {

	private final SecretCard card = new SecretCard();
	private final JLabel statusText = new JLabel();
	private final JLabel poi = new JLabel();
	private final JPanel lineup = new JPanel();
	private final JPanel chatSlot = new JPanel(new BorderLayout());
	private String myName = "";
	private String statusKey = "st_progress";
	private boolean statusAlarm = false;
	private ArrayList<String> names = new ArrayList<String>();

	public RoundScreen() {
		setOpaque(false);
		setLayout(new BorderLayout(28, 0));

		// ================= card, status, lineup =================
		JPanel main = new JPanel(new BorderLayout(0, 18));
		main.setOpaque(false);

		JPanel cardRow = new JPanel(new BorderLayout(18, 0));
		cardRow.setOpaque(false);
		card.setPreferredSize(new Dimension(520, 236));
		cardRow.add(card, BorderLayout.CENTER);
		main.add(cardRow, BorderLayout.NORTH);

		JPanel middle = new JPanel(new BorderLayout(0, 16));
		middle.setOpaque(false);

		JPanel statusBar = new JPanel(new BorderLayout(16, 0));
		statusBar.setOpaque(false);
		statusBar.setBorder(new CompoundBorder(
				new DashedBorder(DashedBorder.TOP, false, Theme.INK, 8),
				new DashedBorder(DashedBorder.BOTTOM, false, Theme.INK, 8)));
		statusBar.add(statusText, BorderLayout.CENTER);
		middle.add(statusBar, BorderLayout.NORTH);

		JPanel lineupSection = new JPanel(new BorderLayout(0, 10));
		lineupSection.setOpaque(false);
		poi.setForeground(Theme.INK);
		lineupSection.add(poi, BorderLayout.NORTH);
		lineup.setOpaque(false);
		lineup.setPreferredSize(new Dimension(600, 172));
		lineupSection.add(lineup, BorderLayout.CENTER);
		middle.add(lineupSection, BorderLayout.CENTER);

		JPanel middleHolder = new JPanel(new BorderLayout());
		middleHolder.setOpaque(false);
		middleHolder.add(middle, BorderLayout.NORTH);
		main.add(middleHolder, BorderLayout.CENTER);
		add(main, BorderLayout.CENTER);

		// ================= chat =================
		chatSlot.setOpaque(false);
		chatSlot.setPreferredSize(new Dimension(350, 100));
		add(chatSlot, BorderLayout.LINE_END);
		applyLanguage();
	}

	public void attachChat(ChatPanel chat) {
		chatSlot.add(chat, BorderLayout.CENTER);
		chatSlot.revalidate();
		chatSlot.repaint();
	}

	public void setMyName(String name) {
		myName = name;
	}

	public SecretCard getCard() {
		return card;
	}

	// key = a Lang key like "st_interrogation"
	public void setStatus(String key, boolean alarm) {
		statusKey = key;
		statusAlarm = alarm;
		Theme.text(statusText, Theme.type(19f), Lang.t(key));
		statusText.setForeground(alarm ? Theme.RED : Theme.INK);
	}

	public void applyLanguage() {
		Theme.text(poi, Theme.stencil(18f), Lang.t("persons_of_interest"));
		setStatus(statusKey, statusAlarm);
		// the dashed line goes between the cards and the chat
		chatSlot.setBorder(new DashedBorder(Lang.rtl() ? DashedBorder.RIGHT : DashedBorder.LEFT, true, Theme.FAINT, 22));
		setPlayers(names);
		card.repaint();
	}

	public void playIntro() {
		card.repaint();
	}

	public void setPlayers(ArrayList<String> newNames) {
		names = newNames;
		lineup.removeAll();
		int count = Math.min(names.size(), 8);
		lineup.setLayout(new GridLayout(1, Math.max(5, count), 12, 0));
		for (int i = 0; i < count; i++) {
			IdCard idCard = new IdCard();
			String name = names.get(i);
			boolean me = name.equals(myName);
			idCard.setPlayer(name, me ? Lang.t("thats_you") : "");
			idCard.setStatus("", Theme.RED, me ? Theme.INK : Theme.CARD_EDGE);
			lineup.add(idCard);
		}
		// empty spaces so a few players don't get stretched-out cards
		for (int i = count; i < 5; i++) {
			JPanel space = new JPanel();
			space.setOpaque(false);
			lineup.add(space);
		}
		lineup.applyComponentOrientation(Lang.orientation());
		lineup.revalidate();
		lineup.repaint();
	}
}
