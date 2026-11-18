/* =====================================================================
 *  TvWindow.java  -  "TV screen" (bonus): one computer shows the game
 *                    on a big screen / projector for everybody
 * ---------------------------------------------------------------------
 *  SpyClient starts in TV mode when "TV screen" is clicked on the
 *  desktop. It connects with "#HOST" as its first line: the server sends
 *  it every broadcast, but never a ROLE, so NO secret word is shown.
 *  LEFT : big topic, big clock, the time bar, who is asking who
 *  RIGHT: every player, with "asking now" / "answering" / "listening"
 * ===================================================================== */

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TvWindow extends XpWindow {

	private final JLabel line1 = new JLabel(" ", JLabel.CENTER);     // "Round 1 of 3 · Topic"
	private final JLabel line2 = new JLabel(" ", JLabel.CENTER);     // [icon] FOOD
	private final JLabel line3 = new JLabel(" ", JLabel.CENTER);     // 1:14
	private final XpParts.Blocks bar = new XpParts.Blocks(36);
	private final JLabel line4 = new JLabel(" ", JLabel.CENTER);     // "Nouri is asking Fahad..."
	private final JPanel playerList = Xp.clear(null);
	private Vector<String> players = new Vector<String>();
	private HashMap<String, Avatar> avatars = new HashMap<String, Avatar>();

	public TvWindow() {
		super(Lang.t("tv_title"), Xp.pixelIcon(Xp.TV, 2));
		body().setLayout(new BorderLayout(20, 0));
		body().setBorder(new EmptyBorder(16, 18, 16, 18));

		// ---- LEFT: the big white screen ----
		JPanel screen = XpParts.listBox(new GridBagLayout());
		JPanel center = Xp.clear(null);
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		line1.setForeground(Xp.BLUE_TEXT);
		line2.setForeground(Xp.INK);
		line2.setIconTextGap(20);
		line3.setForeground(Xp.ORANGE_DARK);
		line4.setForeground(Xp.INK);
		bar.setPreferredSize(new Dimension(620, 36));
		bar.setMaximumSize(new Dimension(620, 36));
		JLabel[] lines = { line1, line2, line3 };
		for (int i = 0; i < lines.length; i++) {
			lines[i].setAlignmentX(Component.CENTER_ALIGNMENT);
			center.add(lines[i]);
			center.add(Box.createVerticalStrut(10));
		}
		bar.setAlignmentX(Component.CENTER_ALIGNMENT);
		center.add(bar);
		center.add(Box.createVerticalStrut(26));
		line4.setAlignmentX(Component.CENTER_ALIGNMENT);
		center.add(line4);
		screen.add(center);
		body().add(screen, BorderLayout.CENTER);

		// ---- RIGHT: the players ----
		JPanel side = Xp.clear(new BorderLayout());
		side.setPreferredSize(new Dimension(330, 10));
		playerList.setLayout(new BoxLayout(playerList, BoxLayout.Y_AXIS));
		side.add(playerList, BorderLayout.NORTH);
		JLabel note = new JLabel(XpParts.wrap(Lang.t("tv_no_word"), 280), JLabel.CENTER);
		note.setFont(Xp.fontFor(Lang.t("tv_no_word"), Xp.MONO, 14f));
		note.setForeground(Xp.GREY);
		side.add(note, BorderLayout.SOUTH);
		body().add(side, BorderLayout.LINE_END);
	}

	// ---- small helper: set a line's text and font ----
	private void set(JLabel label, String text, Font font, float size) {
		label.setText(text);
		label.setFont(Xp.fontFor(text, font, size));
	}

	// =================================================================
	// the different moments of the game
	// =================================================================
	public void showLobby(String address) {
		set(line1, Lang.t("tagline"), Xp.PIXEL, 24f);
		line2.setText("");
		line2.setIcon(Xp.pixelText("برا السالفة", 34, 5, Xp.ORANGE, Xp.ORANGE_DARK));
		set(line3, " ", Xp.WORD, 20f);
		bar.setVisible(false);
		set(line4, Xp.rtlLine(Lang.f("tv_lobby", players.size()) + "   ·   " + Lang.f("tv_join", address)), Xp.PIXEL, 22f);
		setTurn(null, null);
	}

	public void showRound(int round, int rounds, String topic, int left, int total, String asker, String answerer) {
		set(line1, Lang.f("round_of", round, rounds) + " · " + Lang.t("topic"), Xp.PIXEL, 24f);
		set(line2, Lang.name(topic).toUpperCase(), Xp.WORD, 64f);
		line2.setIcon(Xp.pixelIcon(Xp.topicIcon(topic), 9));
		set(line3, XpParts.clock(left), Xp.WORD, 110f);
		bar.setVisible(true);
		bar.setValue(left, total);
		if (asker != null && answerer != null) {
			set(line4, Lang.f("tv_asking", asker, answerer), Xp.PIXEL, 24f);
		}
		setTurn(asker, answerer);
	}

	public void showVote(int round, int rounds, int left, int total, int voted, int voters) {
		set(line1, Lang.f("round_of", round, rounds), Xp.PIXEL, 24f);
		set(line2, Lang.t("vote").toUpperCase(), Xp.WORD, 64f);
		line2.setIcon(Xp.pixelIcon(Xp.SMILE, 9));
		set(line3, XpParts.clock(left), Xp.WORD, 110f);
		bar.setVisible(true);
		bar.setValue(voted, Math.max(1, voters));
		set(line4, Lang.t("tv_vote") + "   " + Lang.f("tv_vote_count", voted, voters), Xp.PIXEL, 24f);
		setTurn(null, null);
	}

	public void showGuess(String outsider, int left, int total) {
		set(line1, Lang.f("x_caught", outsider), Xp.PIXEL, 24f);
		set(line2, Lang.t("last_chance").toUpperCase(), Xp.WORD, 48f);
		line2.setIcon(null);
		set(line3, XpParts.clock(left), Xp.WORD, 110f);
		bar.setVisible(true);
		bar.setValue(left, total);
		set(line4, Lang.f("tv_guess", outsider), Xp.PIXEL, 24f);
	}

	public void showResult(String outsider, String word, String verdict) {
		set(line1, Lang.t("the_word_was"), Xp.PIXEL, 24f);
		set(line2, Lang.name(word).toUpperCase(), Xp.WORD, Math.min(64f, 700f / Math.max(1, word.length())));
		line2.setIcon(null);
		set(line3, " ", Xp.WORD, 40f);
		bar.setVisible(false);
		set(line4, Lang.f("tv_caught", outsider) + "  " + verdict, Xp.PIXEL, 24f);
		setTurn(null, null);
	}

	public void showWinner(String winner) {
		set(line1, Lang.t("final_scores"), Xp.PIXEL, 24f);
		set(line2, Lang.f("x_wins", winner), Xp.PIXEL, 64f);
		line2.setIcon(Xp.pixelIcon(Xp.SMILE, 9));
		set(line3, " ", Xp.WORD, 40f);
		bar.setVisible(false);
		set(line4, " ", Xp.PIXEL, 24f);
	}

	// =================================================================
	// the players on the right
	// =================================================================
	public void setPlayers(Vector<String> list, HashMap<String, Avatar> faces) {
		players = new Vector<String>(list);
		avatars = faces;
		setTurn(null, null);
	}

	// draws the player cards again: the asker is orange
	public void setTurn(String asker, String answerer) {
		playerList.removeAll();
		int size = players.size() <= 5 ? 90 : 56;
		for (int i = 0; i < players.size(); i++) {
			String name = players.elementAt(i);
			boolean asking = name.equals(asker);
			String status = asking ? Lang.t("asking_now") : name.equals(answerer) ? Lang.t("answering") : Lang.t("listening");
			JPanel card = new JPanel(new BorderLayout(16, 0));
			card.setBackground(asking ? Xp.ORANGE_SOFT : Color.WHITE);
			card.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(asking ? Xp.ORANGE : Xp.FIELD_BORDER, 2),
					new EmptyBorder(8, 14, 8, 14)));
			card.add(new Avatar.Portrait(avatars.get(name), size, asking), BorderLayout.LINE_START);
			JPanel text = Xp.clear(new GridLayout(2, 1));
			text.add(Xp.label(name, Xp.PIXEL, 26f, Xp.INK));
			text.add(Xp.label(asker == null ? " " : status, Xp.MONO, 16f, asking ? Xp.ORANGE_DARK : Xp.GREY));
			JPanel textBox = Xp.clear(new GridBagLayout());
			textBox.add(text, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			card.add(textBox, BorderLayout.CENTER);
			card.setMaximumSize(new Dimension(Integer.MAX_VALUE, size + 40));
			playerList.add(card);
			playerList.add(Box.createVerticalStrut(12));
		}
		playerList.revalidate();
		playerList.repaint();
	}
}
