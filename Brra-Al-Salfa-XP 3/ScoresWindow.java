/* =====================================================================
 *  ScoresWindow.java  -  "Scores" (+ the "Round history" window)
 * ---------------------------------------------------------------------
 *  The server keeps the points of the game and sends them sorted:
 *     SCORES:Meryem=5=3=1,Sara=4=2=1,...      name=points=caught=wasOutsider
 *  After the last round it also sends GAME_OVER:<winner>.
 *  The "Round history" list is built by SpyClient from every RESULT.
 *  All the numbers use the same font (IBM Plex Mono), the points in bold.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ScoresWindow extends XpWindow {

	public interface Listener {
		void exit();
		void playAgain();          // host: start a new game
		void back();               // during the game: back to the result
	}

	// one line of the SCORES message
	public static class Score {
		String name;
		int points, caught, outsider;
	}

	public ScoresWindow(ArrayList<Score> scores, HashMap<String, Avatar> avatars, int roundsPlayed,
			ArrayList<String> topics, boolean gameOver, boolean iAmHost, final Listener listener) {
		super(Lang.t("scores"), Xp.pixelIcon(Xp.EVENTS, 2));
		helpMenu();
		body().setBorder(new EmptyBorder(16, 18, 14, 18));

		// ---- the big smiley + "Meryem wins!" ----
		JPanel top = Xp.clear(new BorderLayout(20, 0));
		top.add(new JLabel(Xp.pixelIcon(Xp.SMILE, 8)), BorderLayout.LINE_START);
		JPanel title = Xp.clear(new GridLayout(2, 1));
		String winner = scores.size() > 0 ? scores.get(0).name : "";
		String head = gameOver ? Lang.f("x_wins", winner) : Lang.f("scores_after", roundsPlayed);
		title.add(Xp.label(head, Xp.PIXEL, 32f, Xp.INK));
		String topicText = "";
		for (int i = 0; i < topics.size(); i++) {
			topicText = topicText + (i > 0 ? ", " : "") + Lang.name(topics.get(i));
		}
		title.add(XpParts.hint(Lang.f("rounds_played", roundsPlayed) + "  ·  " + Lang.t("topic") + ": " + topicText));
		top.add(title, BorderLayout.CENTER);
		top.setBorder(new EmptyBorder(0, 0, 16, 0));
		body().add(top, BorderLayout.NORTH);

		// ---- the table ----
		JPanel table = XpParts.listBox(null);
		table.setLayout(new BoxLayout(table, BoxLayout.Y_AXIS));
		table.add(headerRow());
		for (int i = 0; i < scores.size(); i++) {
			table.add(scoreRow(i + 1, scores.get(i), avatars.get(scores.get(i).name), i == 0 && gameOver));
		}
		JPanel tableBox = Xp.clear(new BorderLayout());
		tableBox.add(table, BorderLayout.NORTH);
		body().add(tableBox, BorderLayout.CENTER);

		// ---- buttons ----
		JPanel buttons = Xp.clear(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		buttons.setBorder(new EmptyBorder(16, 0, 0, 0));
		if (gameOver) {
			XpButton exit = new XpButton(Lang.t("exit"), XpButton.NORMAL);
			XpButton again = new XpButton(Lang.t("play_again"), XpButton.GREEN);
			again.setEnabled(iAmHost);
			exit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					listener.exit();
				}
			});
			again.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					listener.playAgain();
				}
			});
			XpButton back = new XpButton(Lang.t("back"), XpButton.NORMAL);
			back.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					listener.back();
				}
			});
			buttons.add(back);
			buttons.add(exit);
			buttons.add(again);
		} else {
			XpButton back = new XpButton(Lang.t("back"), XpButton.NORMAL);
			back.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					listener.back();
				}
			});
			buttons.add(back);
		}
		body().add(buttons, BorderLayout.SOUTH);
	}

	// the column widths: # | Player | Caught | Was outsider | Points
	private static final int[] WIDTHS = { 40, 300, 100, 140, 90 };

	private JPanel headerRow() {
		JPanel row = new JPanel(null) {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(0xEBEADB)));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(new Color(0xD6D2C2));
				g2.fillRect(0, getHeight() - 2, getWidth(), 2);
			}
		};
		row.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
		row.setBorder(new EmptyBorder(8, 12, 8, 12));
		String[] names = { "#", Lang.t("col_player"), Lang.t("col_caught"), Lang.t("col_outsider"), Lang.t("col_points") };
		for (int i = 0; i < names.length; i++) {
			JLabel l = Xp.label(names[i], Xp.MONO, 14f, new Color(0x4A4A4A));
			l.setHorizontalAlignment(i >= 2 ? JLabel.TRAILING : JLabel.LEADING);
			l.setPreferredSize(new Dimension(WIDTHS[i], 20));
			row.add(l);
		}
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
		return row;
	}

	private JPanel scoreRow(int rank, Score s, Avatar face, boolean first) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		row.setBackground(first ? Xp.ORANGE_SOFT : Color.WHITE);
		row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, Xp.LINE), new EmptyBorder(4, 12, 4, 12)));
		// all the numbers: IBM Plex Mono size 16 (points in the bold version)
		JLabel r = Xp.label("" + rank, Xp.MONO_BOLD, 16f, first ? Xp.ORANGE_DARK : new Color(0x4A4A4A));
		r.setPreferredSize(new Dimension(WIDTHS[0], 72));
		row.add(r);
		JPanel who = Xp.clear(new FlowLayout(FlowLayout.LEADING, 14, 0));
		who.add(new Avatar.Portrait(face, 52, first));
		who.add(Xp.label(s.name, Xp.PIXEL, 20f, Xp.INK));
		who.setPreferredSize(new Dimension(WIDTHS[1], 72));
		row.add(who);
		row.add(number("" + s.caught, Xp.MONO, WIDTHS[2]));
		row.add(number("" + s.outsider, Xp.MONO, WIDTHS[3]));
		row.add(number("" + s.points, Xp.MONO_BOLD, WIDTHS[4]));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
		return row;
	}

	private JLabel number(String text, Font font, int width) {
		JLabel l = Xp.label(text, font, 16f, Xp.INK);
		l.setHorizontalAlignment(JLabel.TRAILING);
		l.setPreferredSize(new Dimension(width, 72));
		return l;
	}

	// =================================================================
	// "Round history": one entry per round played
	// =================================================================
	public static class HistoryWindow extends XpWindow {
		public HistoryWindow(ArrayList<String[]> rounds, HashMap<String, Avatar> avatars) {
			super(Lang.t("round_history"), Xp.pixelIcon(Xp.PAPER, 2));
			JPanel list = XpParts.listBox(null);
			list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
			if (rounds.size() == 0) {
				JLabel none = XpParts.hint(Lang.t("no_rounds"));
				none.setBorder(new EmptyBorder(10, 10, 10, 10));
				list.add(none);
			}
			// each entry: { round number, word, outsider, "caught" / "escaped" }
			for (int i = 0; i < rounds.size(); i++) {
				String[] r = rounds.get(i);
				JPanel entry = Xp.clear(null);
				entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
				entry.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0, 0, i < rounds.size() - 1 ? 1 : 0, 0, Xp.LINE),
						new EmptyBorder(8, 10, 8, 10)));
				JPanel line1 = Xp.clear(new FlowLayout(FlowLayout.LEADING, 0, 0));
				line1.add(Xp.label(Lang.f("round_n", r[0]), Xp.PIXEL, 14f, Xp.INK));
				line1.add(Xp.label("  ·  " + Lang.name(r[1]), Xp.MONO, 14f, Xp.INK));
				entry.add(line1);
				Avatar face = avatars.get(r[2]);
				entry.add(new JLabel(face == null ? Avatar.forName(r[2]).icon(2) : face.icon(2)));
				boolean caught = r[3].equals("caught");
				JPanel line3 = Xp.clear(new FlowLayout(FlowLayout.LEADING, 0, 0));
				line3.add(XpParts.hint(Lang.f("was_outsider", r[2]) + "  ·  "));
				line3.add(Xp.label(Lang.t(r[3]), Xp.MONO, 13f, caught ? Xp.GREEN : Xp.ORANGE_DARK));
				entry.add(line3);
				XpParts.leftAlign(entry);
				list.add(entry);
			}
			body().add(list, BorderLayout.NORTH);
		}
	}
}
