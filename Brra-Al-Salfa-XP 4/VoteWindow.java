/* =====================================================================
 *  VoteWindow.java  -  "Who is برا السالفة ?"
 * ---------------------------------------------------------------------
 *  - one card per suspect: click a card to choose
 *  - the JComboBox of the brief ("or pick from the list") chooses too;
 *    the cards and the combo box always show the same choice
 *  - Confirm vote sends "VOTE:<name>" (only once, it can't be changed)
 *  - bottom: "Votes 1 of 4", a small bar, and the time left
 *  Every time someone votes, the server sends VOTED:<name> and
 *  VOTES:x/y, and SpyClient calls setVotes(x, y)  (live vote tracking).
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class VoteWindow extends XpWindow {

	public interface Listener {
		void vote(String name);
	}

	private final Vector<String> suspects;
	private final ArrayList<JPanel> cards = new ArrayList<JPanel>();
	private final JComboBox<String> combo;
	private final JLabel votes = new JLabel();
	private final XpParts.Blocks voteBar = new XpParts.Blocks(26);
	private final JLabel time = new JLabel(" ");
	private final JLabel status = new JLabel(" ");
	private final XpButton cancel = new XpButton(Lang.t("cancel"), XpButton.NORMAL);
	private final XpButton confirm = new XpButton(Lang.t("confirm_vote"), XpButton.GO);
	private String choice = null;
	private boolean open = true;           // false after voting (or when watching)

	public VoteWindow(Vector<String> players, HashMap<String, Avatar> avatars, String myName,
			boolean playing, final Listener listener) {
		super(Lang.t("vote"), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(14, 18, 14, 18));

		// you can't vote for yourself: the suspects are everybody else
		suspects = new Vector<String>();
		for (int i = 0; i < players.size(); i++) {
			if (!players.elementAt(i).equals(myName)) {
				suspects.add(players.elementAt(i));
			}
		}

		// ---- "Who is  برا السالفة ?" ----
		JPanel top = Xp.clear(null);
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		JPanel title = Xp.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
		title.add(Xp.label(Lang.t("who_is") + "  ", Xp.PIXEL, 38f, Xp.INK));
		title.add(new JLabel(Xp.pixelText("برا السالفة", 20, 3, Xp.ORANGE, Xp.ORANGE_DARK)));
		title.add(Xp.label("  ?", Xp.PIXEL, 38f, Xp.INK));
		top.add(title);
		JLabel sub = Xp.label(Lang.t(playing ? "pick_one" : "not_playing"), Xp.MONO, 16f, new Color(0x3A3A3A));
		sub.setAlignmentX(Component.CENTER_ALIGNMENT);
		top.add(sub);
		top.add(Box.createVerticalStrut(14));

		// ---- the cards ----
		int size = suspects.size() <= 3 ? 120 : suspects.size() <= 5 ? 80 : 60;
		JPanel cardRow = Xp.clear(new GridLayout(1, suspects.size(), 14, 0));
		for (int i = 0; i < suspects.size(); i++) {
			final String name = suspects.elementAt(i);
			JPanel card = new JPanel(new BorderLayout(0, 8));
			card.setBackground(Color.WHITE);
			card.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(Xp.FIELD_BORDER, 2, true), new EmptyBorder(12, 8, 12, 8)));
			card.add(new Avatar.Portrait(avatars.get(name), size, false), BorderLayout.CENTER);
			card.add(new RadioLabel(name), BorderLayout.SOUTH);
			ActionListener pick = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					choose(name);
				}
			};
			Xp.onClick(card, pick);
			Xp.onClick(card.getComponent(0), pick);     // a click on the face also chooses
			cards.add(card);
			cardRow.add(card);
		}
		cardRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		top.add(cardRow);
		top.add(Box.createVerticalStrut(12));

		// ---- the JComboBox (the brief's drop-down list) ----
		combo = new JComboBox<String>(suspects);
		combo.setFont(Xp.MONO.deriveFont(15f));
		combo.setBackground(Color.WHITE);
		combo.setSelectedIndex(-1);
		combo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (combo.getSelectedItem() != null) {
					choose((String) combo.getSelectedItem());
				}
			}
		});
		JPanel comboRow = Xp.clear(new FlowLayout(FlowLayout.CENTER, 10, 0));
		comboRow.add(Xp.label(Lang.t("or_list"), Xp.MONO, 14f, Xp.GREY));
		comboRow.add(combo);
		top.add(comboRow);
		body().add(top, BorderLayout.CENTER);

		// ---- bottom: votes, bar, time, buttons ----
		JPanel bottom = Xp.clear(new BorderLayout(12, 0));
		bottom.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD0CCBB)), new EmptyBorder(14, 0, 0, 0)));
		JPanel left = Xp.clear(new FlowLayout(FlowLayout.LEADING, 12, 6));
		votes.setForeground(Xp.INK);
		voteBar.setPreferredSize(new Dimension(150, 26));
		time.setFont(Xp.PIXEL.deriveFont(17f));
		time.setForeground(Xp.ORANGE_DARK);
		left.add(votes);
		left.add(voteBar);
		left.add(time);
		JPanel buttons = Xp.clear(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		buttons.add(cancel);
		buttons.add(confirm);
		status.setFont(Xp.MONO.deriveFont(13f));
		status.setForeground(Xp.GREEN);
		bottom.add(left, BorderLayout.LINE_START);
		bottom.add(buttons, BorderLayout.LINE_END);
		bottom.add(status, BorderLayout.SOUTH);
		body().add(bottom, BorderLayout.SOUTH);

		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				choose(null);                       // clears the choice
			}
		});
		confirm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (choice != null && open) {
					listener.vote(choice);           // "VOTE:<name>"
					close(Lang.t("voted_wait"));
				}
			}
		});

		setVotes(0, players.size());
		if (!playing) {
			close(" ");
		} else {
			choose(null);
		}
	}

	// a suspect was chosen (by a card or by the combo box); null = none
	private void choose(String name) {
		if (!open) {
			return;
		}
		choice = name;
		for (int i = 0; i < cards.size(); i++) {
			boolean on = suspects.elementAt(i).equals(name);
			JPanel card = cards.get(i);
			card.setBackground(on ? Xp.ORANGE_SOFT : Color.WHITE);
			card.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(on ? Xp.ORANGE : Xp.FIELD_BORDER, 2, true), new EmptyBorder(12, 8, 12, 8)));
			((RadioLabel) card.getComponent(1)).setOn(on);
		}
		if (name == null) {
			combo.setSelectedIndex(-1);
		} else if (!name.equals(combo.getSelectedItem())) {
			combo.setSelectedItem(name);
		}
		confirm.setEnabled(name != null);
	}

	// after voting: nothing can be changed any more
	private void close(String message) {
		open = false;
		combo.setEnabled(false);
		cancel.setEnabled(false);
		confirm.setEnabled(false);
		status.setText(message);
	}

	// live vote tracking: "Votes 2 of 4"
	public void setVotes(int done, int total) {
		String text = Lang.f("votes_of", done, total);
		votes.setText(text);
		votes.setFont(Xp.fontFor(text, Xp.PIXEL, 16f));
		voteBar.setValue(done, total);
	}

	public void setTime(int seconds) {
		time.setText(XpParts.clock(seconds));
	}

	// =================================================================
	// RadioLabel: a painted XP radio circle + the name
	// =================================================================
	static class RadioLabel extends JComponent {
		private final String name;
		private boolean on = false;

		RadioLabel(String name) {
			this.name = name;
			setPreferredSize(new Dimension(120, 30));
		}

		void setOn(boolean value) {
			on = value;
			repaint();
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Xp.smooth(g);
			g2.setFont(Xp.fontFor(name, Xp.PIXEL, 19f));
			FontMetrics fm = g2.getFontMetrics();
			int total = 20 + 10 + fm.stringWidth(name);
			int x = (getWidth() - total) / 2;
			int y = (getHeight() - 20) / 2;
			int textX = x + 30;
			if (Xp.rtl(this)) {                      // Arabic: the circle on the right
				textX = x;
				x = x + total - 20;
			}
			g2.setColor(Color.WHITE);
			g2.fillOval(x, y, 20, 20);
			g2.setColor(Xp.BLUE_TEXT);
			g2.setStroke(new BasicStroke(2f));
			g2.drawOval(x, y, 20, 20);
			if (on) {
				g2.setColor(Xp.GREEN);
				g2.fillOval(x + 6, y + 6, 9, 9);   // the green dot
			}
			g2.setColor(Xp.INK);
			g2.drawString(name, textX, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
			g2.dispose();
		}
	}
}
