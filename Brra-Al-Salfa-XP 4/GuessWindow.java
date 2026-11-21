/* =====================================================================
 *  GuessWindow.java  -  "Last chance"  (bonus: the outsider's last guess)
 * ---------------------------------------------------------------------
 *  When the outsider is caught, the server sends to everyone:
 *      CAUGHT:<name>
 *      GUESS_OPTIONS:Shawarma,Falafel,...   (the real word + 7 others, mixed)
 *  and to the outsider only:  GUESS_TURN
 *  The outsider clicks one of the 8 words and presses Submit guess
 *  -> "GUESS:<word>". Everybody else sees the same window, turned off.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GuessWindow extends XpWindow {

	public interface Listener {
		void guess(String word);
	}

	private final ArrayList<XpButton> buttons = new ArrayList<XpButton>();
	private final XpParts.Blocks bar = new XpParts.Blocks(26);
	private final JLabel time = new JLabel(" ");
	private final XpButton submit = new XpButton(Lang.t("submit_guess"), XpButton.GO);
	private String choice = null;
	private final int total;

	public GuessWindow(String outsiderName, Avatar outsiderFace, String[] options, boolean myTurn,
			int seconds, final Listener listener) {
		super(Lang.t("last_chance"), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(14, 18, 14, 18));
		total = seconds;

		// ---- top: the outsider's face + the title ----
		JPanel top = Xp.clear(new BorderLayout(22, 0));
		Avatar.Portrait face = new Avatar.Portrait(outsiderFace, 90, true);
		top.add(face, BorderLayout.LINE_START);
		JPanel text = Xp.clear(new GridLayout(2, 1));
		String title = myTurn ? Lang.t("caught_you") : Lang.f("x_caught", outsiderName);
		String line = myTurn ? Lang.t("guess_text") : Lang.f("x_guessing", outsiderName);
		text.add(Xp.label(title, Xp.PIXEL, 30f, Xp.INK));
		text.add(Xp.label(line, Xp.MONO, 15f, new Color(0x3A3A3A)));
		JPanel textBox = Xp.clear(new BorderLayout());
		textBox.add(text, BorderLayout.CENTER);
		textBox.setBorder(new EmptyBorder(12, 0, 12, 0));
		top.add(textBox, BorderLayout.CENTER);
		top.setBorder(new EmptyBorder(0, 0, 18, 0));
		body().add(top, BorderLayout.NORTH);

		// ---- the 8 words (2 rows of 4) ----
		JPanel grid = Xp.clear(new GridLayout(2, 4, 12, 12));
		for (int i = 0; i < options.length; i++) {
			final String word = options[i];
			XpButton b = new XpButton(Lang.name(word), XpButton.NORMAL);
			b.setTextSize(17f);
			b.setPreferredSize(new Dimension(150, 52));
			b.setEnabled(myTurn);
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					choose(word);
				}
			});
			b.setName(word);                      // remember the English word
			buttons.add(b);
			grid.add(b);
		}
		body().add(grid, BorderLayout.CENTER);

		// ---- bottom: time + submit ----
		JPanel bottom = Xp.clear(new BorderLayout(12, 0));
		bottom.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD0CCBB)), new EmptyBorder(14, 0, 0, 0)));
		JPanel left = Xp.clear(new FlowLayout(FlowLayout.LEADING, 12, 6));
		left.add(Xp.label(Lang.t("time"), Xp.PIXEL, 16f, Xp.INK));
		bar.setPreferredSize(new Dimension(260, 26));
		left.add(bar);
		time.setFont(Xp.PIXEL.deriveFont(17f));
		time.setForeground(Xp.ORANGE_DARK);
		left.add(time);
		bottom.add(left, BorderLayout.LINE_START);
		JPanel right = Xp.clear(new FlowLayout(FlowLayout.TRAILING, 0, 0));
		right.add(submit);
		bottom.add(right, BorderLayout.LINE_END);
		JPanel south = Xp.clear(new BorderLayout());
		south.setBorder(new EmptyBorder(18, 0, 0, 0));
		south.add(bottom, BorderLayout.CENTER);
		body().add(south, BorderLayout.SOUTH);

		submit.setEnabled(false);
		submit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (choice != null) {
					listener.guess(choice);           // "GUESS:<word>"
					for (int i = 0; i < buttons.size(); i++) {
						buttons.get(i).setEnabled(false);
					}
					submit.setEnabled(false);
				}
			}
		});
		setTime(seconds);
	}

	// one word was clicked: it turns orange
	private void choose(String word) {
		choice = word;
		for (int i = 0; i < buttons.size(); i++) {
			buttons.get(i).setSelectedLook(buttons.get(i).getName().equals(word));
		}
		submit.setEnabled(true);
	}

	public void setTime(int seconds) {
		time.setText(XpParts.clock(seconds));
		bar.setValue(seconds, total);
	}
}
