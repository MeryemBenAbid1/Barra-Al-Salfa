/* =====================================================================
 *  ResultWindow.java  -  "Round 1 result"
 * ---------------------------------------------------------------------
 *  Built from the server's messages at the end of a round:
 *    RESULT:Outsider: Fahad | Word: Shawarma | WordAr: شاورما | Winner: ... | reason
 *    VOTE_LIST:Meryem>Fahad,Nouri>Fahad,Sara>Fahad,Fahad>Sara
 *    ROUND_POINTS:Meryem=1,Nouri=1,Sara=1,Fahad=0
 *    GUESSED:Falafel          (only if the outsider made a last guess)
 *  Shows: who the outsider was, the word, the last guess, who voted for
 *  who, and the points of this round.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ResultWindow extends XpWindow {

	public interface Listener {
		void seeScores();
		void nextRound();
	}

	// how the round ended
	public static final int CAUGHT = 0, ESCAPED = 1, STOLEN = 2;

	public ResultWindow(int round, int rounds, String outsider, Avatar outsiderFace, String word, String wordArabic,
			int ending, String guessed, Vector<String> players, HashMap<String, Avatar> avatars,
			HashMap<String, String> votes, HashMap<String, Integer> points, boolean iAmHost, final Listener listener) {
		super(Lang.f("round_result", round), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(16, 18, 14, 18));
		JPanel column = Xp.clear(null);
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

		// ---- "Fahad was  برا السالفة" ----
		JPanel top = Xp.clear(new BorderLayout(28, 0));
		top.add(new Avatar.Portrait(outsiderFace, 140, true), BorderLayout.LINE_START);
		JPanel who = Xp.clear(null);
		who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
		who.add(Xp.label(Lang.f("x_was", outsider), Xp.PIXEL, 28f, Xp.INK));
		who.add(new JLabel(Xp.pixelText("برا السالفة", 22, 4, Xp.ORANGE, Xp.ORANGE_DARK)));
		String verdict;
		Color verdictColor;
		if (ending == CAUGHT) {
			verdict = Lang.t("players_win");
			verdictColor = Xp.GREEN;
		} else if (ending == STOLEN) {
			verdict = Lang.t("outsider_steals");
			verdictColor = Xp.ORANGE_DARK;
		} else {
			verdict = Lang.t("outsider_wins");
			verdictColor = Xp.ORANGE_DARK;
		}
		who.add(Xp.label(verdict, Xp.PIXEL, 19f, verdictColor));
		XpParts.leftAlign(who);
		top.add(who, BorderLayout.CENTER);
		column.add(top);
		column.add(Box.createVerticalStrut(16));

		// ---- two boxes: the word / the last guess ----
		JPanel boxes = Xp.clear(new GridLayout(1, 2, 16, 0));
		JPanel wordBox = whiteCard();
		wordBox.add(XpParts.hint(Lang.t("the_word_was")));
		wordBox.add(Xp.label(word.toUpperCase(), Xp.WORD, Math.min(24f, 300f / Math.max(1, word.length())), Xp.INK));
		if (wordArabic.length() > 0) {
			wordBox.add(Xp.label(wordArabic, Xp.ARABIC, 20f, Xp.ORANGE_DARK));
		}
		boxes.add(wordBox);

		JPanel guessBox = whiteCard();
		guessBox.add(XpParts.hint(Lang.f("last_guess_of", outsider)));
		if (guessed == null) {
			guessBox.add(Xp.label(Lang.t("no_guess"), Xp.PIXEL, 20f, Xp.GREY));
		} else {
			boolean right = guessed.equalsIgnoreCase(word);
			Color color = right ? Xp.GREEN : new Color(0xC0391B);
			JPanel line = Xp.clear(new FlowLayout(FlowLayout.LEADING, 0, 0));
			line.add(Xp.label(guessed.toUpperCase(), Xp.WORD, Math.min(24f, 280f / Math.max(1, guessed.length() + 2)), color));
			line.add(Xp.label(right ? "   \u2713" : "   \u00D7", Xp.MONO_BOLD, 26f, color));   // check or cross
			guessBox.add(line);
			guessBox.add(Xp.label(Lang.t(right ? "right" : "wrong"), Xp.MONO, 14f, new Color(0x3A3A3A)));
		}
		boxes.add(guessBox);
		XpParts.leftAlign(wordBox);
		XpParts.leftAlign(guessBox);
		boxes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
		column.add(boxes);
		column.add(Box.createVerticalStrut(14));

		// ---- who voted for who, and the points ----
		JPanel table = XpParts.listBox(new GridLayout(0, 1));
		for (int i = 0; i < players.size(); i++) {
			String name = players.elementAt(i);
			JPanel row = new JPanel(new BorderLayout(14, 0));
			row.setBackground(Color.WHITE);
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, i < players.size() - 1 ? 1 : 0, 0, Xp.LINE),
					new EmptyBorder(6, 12, 6, 12)));
			Avatar face = avatars.get(name);
			JLabel nameLabel = new JLabel(name, face == null ? null : face.icon(3), JLabel.LEADING);
			nameLabel.setIconTextGap(12);
			nameLabel.setFont(Xp.fontFor(name, Xp.PIXEL, 17f));
			nameLabel.setForeground(Xp.INK);
			nameLabel.setPreferredSize(new Dimension(170, 32));
			row.add(nameLabel, BorderLayout.LINE_START);
			String target = votes.get(name);
			row.add(XpParts.hint(target == null ? Lang.t("no_vote") : Lang.f("voted_for", target)), BorderLayout.CENTER);
			Integer p = points.get(name);
			int value = p == null ? 0 : p.intValue();
			JLabel pts = Xp.label(value > 0 ? "+" + value : "0", Xp.MONO_BOLD, 16f, value > 0 ? Xp.GREEN : Xp.GREY);
			row.add(pts, BorderLayout.LINE_END);
			table.add(row);
		}
		column.add(table);
		column.add(Box.createVerticalStrut(16));
		XpParts.leftAlign(column);

		// ---- buttons ----
		boolean last = round >= rounds;
		XpButton scores = new XpButton(Lang.t(last ? "final_scores" : "see_scores"), last ? XpButton.GO : XpButton.NORMAL);
		XpButton next = new XpButton(Lang.t("next_round") + " " + Xp.nextTriangle(), XpButton.GREEN);
		next.setEnabled(iAmHost);
		JPanel buttons = Xp.clear(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		buttons.add(scores);
		if (!last) {
			buttons.add(next);
		}
		scores.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listener.seeScores();
			}
		});
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listener.nextRound();
			}
		});
		body().add(column, BorderLayout.CENTER);
		body().add(buttons, BorderLayout.SOUTH);
	}

	// a white box with a blue-grey border (the word / the last guess)
	private JPanel whiteCard() {
		// (every line starts on the left: see XpParts.leftAlign in the constructor)
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(Color.WHITE);
		card.setBorder(BorderFactory.createCompoundBorder(Xp.whiteBox(), new EmptyBorder(10, 14, 10, 14)));
		return card;
	}
}
