/* =====================================================================
 *  VoteScreen.java  -  screen 3: "WHO IS AL-DAKHEEL?"
 * ---------------------------------------------------------------------
 *  ACCUSE marks a suspect (a red marker circle draws itself around
 *  the photo). FILE ACCUSATION sends "VOTE:<name>" once per round.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class VoteScreen extends JPanel {

	public interface VoteListener {
		void vote(String name);
	}

	private final JPanel grid = new JPanel();
	private final ArrayList<VoteCard> suspects = new ArrayList<VoteCard>();
	private final Tally tally = new Tally();
	private final JLabel tallyText;
	private final JLabel subtitle;
	private final JLabel title = new JLabel();
	private final JLabel otherTitle = new JLabel();
	private final JLabel filed = new JLabel();
	private final DossierButton fileButton;
	private final Stamp filedStamp = new Stamp("", Theme.RED, 20f, -4);
	private VoteListener listener;

	private String myName = "";
	private VoteCard picked = null;
	private boolean canVote = false;

	public VoteScreen() {
		setOpaque(false);
		setLayout(new BorderLayout(0, 18));

		// ---------- header ----------
		JPanel header = new JPanel(new BorderLayout(20, 0));
		header.setOpaque(false);
		JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
		titles.setOpaque(false);
		title.setForeground(Theme.INK);
		subtitle = new JLabel();
		subtitle.setForeground(Theme.INK_SOFT);
		titles.add(title);
		titles.add(subtitle);
		otherTitle.setForeground(Theme.RED);
		header.add(titles, BorderLayout.CENTER);
		// (the title in the other language was removed: less text on screen)
		add(header, BorderLayout.NORTH);

		// ---------- suspects ----------
		grid.setOpaque(false);
		JPanel gridHolder = new JPanel(new BorderLayout());
		gridHolder.setOpaque(false);
		gridHolder.setBorder(new EmptyBorder(6, 6, 0, 6));
		gridHolder.add(grid, BorderLayout.NORTH);
		add(gridHolder, BorderLayout.CENTER);

		// ---------- footer ----------
		JPanel footer = new JPanel(new BorderLayout(18, 0));
		footer.setOpaque(false);
		footer.setBorder(new DashedBorder(DashedBorder.TOP, true, Theme.FAINT, 12));
		JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEADING, 14, 18));
		leftSide.setOpaque(false);
		filed.setForeground(Theme.INK);
		tallyText = new JLabel("0");
		tallyText.setFont(Theme.monoBold(18f));
		tallyText.setForeground(Theme.INK);
		leftSide.add(filed);
		leftSide.add(tally);
		leftSide.add(tallyText);
		JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		rightSide.setOpaque(false);
		fileButton = new DossierButton("", DossierButton.RED, 21f, -1.5);
		fileButton.setPreferredSize(new Dimension(300, 74));
		filedStamp.setVisible(false);
		rightSide.add(filedStamp);
		rightSide.add(fileButton);
		footer.add(leftSide, BorderLayout.LINE_START);
		footer.add(rightSide, BorderLayout.LINE_END);
		add(footer, BorderLayout.SOUTH);

		fileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (picked != null && canVote && listener != null) {
					listener.vote(picked.name);
					lock();
					filedStamp.slam();
				}
			}
		});
		applyLanguage();
	}

	public void setVoteListener(VoteListener listener) { this.listener = listener; }

	public void applyLanguage() {
		Theme.text(title, Theme.stencil(48f), Lang.t("vote_title"));
		otherTitle.setText(Lang.t("vote_title_other"));
		otherTitle.setFont(Lang.rtl() ? Theme.stencil(30f) : Theme.arabic(46f));
		Theme.text(subtitle, Theme.type(17f), Lang.t(canVote || suspects.isEmpty() ? "vote_sub" : "vote_observer"));
		Theme.text(filed, Theme.stencil(16f), Lang.t("accusations_filed"));
		fileButton.setText(Lang.t("file_accusation"));
		filedStamp.setStamp(Lang.t("accusation_filed"), Theme.RED);
		for (VoteCard c : suspects) {
			c.accuse.setText(Lang.t(c.marked ? "marked" : "accuse"));
			c.repaint();
		}
		revalidate();
		repaint();
	}
	public void setMyName(String name) { myName = name; }

	// new voting phase: one card for every OTHER player
	public void open(ArrayList<String> players, boolean inRound) {
		canVote = inRound;
		picked = null;
		suspects.clear();
		grid.removeAll();
		int number = 1;
		for (String name : players) {
			if (!name.equals(myName)) {
				final VoteCard c = new VoteCard(name, number++);
				c.accuse.setEnabled(canVote);
				c.accuse.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						pick(c);
					}
				});
				suspects.add(c);
				grid.add(c);
			}
		}
		int cols = Math.max(3, Math.min(4, suspects.size()));
		int rows = Math.max(1, (suspects.size() + cols - 1) / cols);
		grid.setLayout(new GridLayout(rows, cols, 20, 16));
		// empty spaces so 1-2 suspects don't get huge cards
		for (int i = suspects.size(); i < cols; i++) {
			JPanel space = new JPanel();
			space.setOpaque(false);
			grid.add(space);
		}
		int cardH = rows > 1 ? 210 : 300;
		for (VoteCard c : suspects) {
			c.setPreferredSize(new Dimension(220, cardH));
		}
		Theme.text(subtitle, Theme.type(17f), Lang.t(canVote ? "vote_sub" : "vote_observer"));
		grid.applyComponentOrientation(Lang.orientation());
		fileButton.setEnabled(false);
		fileButton.setVisible(true);
		filedStamp.setVisible(false);
		setVotes("0");
		grid.revalidate();
		grid.repaint();
		for (int i = 0; i < suspects.size(); i++) {
			suspects.get(i).dealIn(i * 90);
		}
	}

	private void pick(VoteCard c) {
		if (!canVote) {
			return;
		}
		if (picked != null) {
			picked.setMarked(false);
		}
		picked = c;
		c.setMarked(true);
		fileButton.setEnabled(true);
	}

	public void lock() {
		canVote = false;
		for (VoteCard c : suspects) {
			c.accuse.setEnabled(false);
		}
		fileButton.setVisible(false);
	}

	// VOTES message, e.g. "3" or "3/5"
	public void setVotes(String data) {
		int done = 0;
		int total = Math.max(1, suspects.size() + 1);
		try {
			String[] parts = data.trim().split("/");
			done = Integer.parseInt(parts[0].trim());
			if (parts.length > 1) {
				total = Integer.parseInt(parts[1].trim());
			}
		} catch (Exception ex) {
			// unknown format: keep the defaults
		}
		if (done > tally.done) {
			Theme.play("scratch");
		}
		tally.set(done, total);
		tallyText.setText(done + " / " + total);
	}

	// ================= one suspect =================
	private static class VoteCard extends JPanel {
		final String name;
		final int number;
		final DossierButton accuse;
		private float circle = 0f;    // 0..1 how much of the red circle is drawn
		boolean marked = false;
		private float drop = 1f;      // deal-in animation

		VoteCard(String name, int number) {
			this.name = name;
			this.number = number;
			setOpaque(false);
			setLayout(new BorderLayout());
			setBorder(new EmptyBorder(0, 12, 12, 12));
			accuse = new DossierButton(Lang.t("accuse"), DossierButton.STAMP, 16f, 0);
			accuse.setPreferredSize(new Dimension(160, 52));
			add(accuse, BorderLayout.SOUTH);
		}

		void setMarked(boolean on) {
			marked = on;
			accuse.setText(Lang.t(on ? "marked" : "accuse"));
			if (on) {
				Theme.play("marker");
				Anim.run(420, new Anim.Step() {
					public void update(float t) {
						circle = Anim.easeOut(t);
						repaint();
					}
				}, null);
			} else {
				circle = 0f;
				repaint();
			}
		}

		void dealIn(int delay) {
			drop = 0f;
			repaint();
			Anim.later(delay, new Runnable() {
				public void run() {
					Anim.run(320, new Anim.Step() {
						public void update(float t) {
							drop = Anim.easeBack(t);
							repaint();
						}
					}, null);
				}
			});
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int w = getWidth();
			int h = getHeight();
			g2.translate(0, (1f - Math.min(drop, 1f)) * -30);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, drop))));

			// white card
			g2.setColor(new Color(0, 0, 0, 40));
			g2.fillRect(8, 8, w - 12, h - 8);
			g2.setColor(Color.WHITE);
			g2.fillRect(6, 4, w - 12, h - 8);
			g2.setColor(Theme.CARD_EDGE);
			g2.drawRect(6, 4, w - 13, h - 9);

			// mugshot with height lines
			int px = 18, py = 16, pw = w - 36;
			int ph = h - 16 - 52 - 12 - 48;
			g2.setColor(new Color(74, 62, 49));
			g2.fillRect(px, py, pw, ph);
			g2.setColor(new Color(96, 82, 66));
			for (int ly = py + 24; ly < py + ph; ly += 24) {
				g2.fillRect(px, ly, pw, 1);
			}
			g2.setColor(Theme.PAPER);
			String initial = name.substring(0, 1).toUpperCase();
			g2.setFont(Theme.safe(Theme.stencil(Math.min(72f, ph * 0.5f)), initial));
			Theme.centerText(g2, initial, px, py, pw, ph);
			g2.setColor(Theme.INK);

			// name
			g2.setFont(Theme.fit(g2, Theme.type(22f), name, pw, 12f));
			g2.drawString(name, Theme.lineX(g2, name, px, pw), py + ph + 34);

			// red marker circle (drawn as it animates)
			if (circle > 0f) {
				g2.setColor(new Color(179, 38, 30, 220));
				g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.rotate(Math.toRadians(-4), w / 2.0, py + ph / 2.0);
				g2.drawArc(4, py - 12, w - 8, ph + 56, 100, (int) (-370 * circle));
			}
			g2.dispose();
		}
	}

	// ================= little squares: filled = voted =================
	private static class Tally extends JComponent {
		int done = 0;
		int total = 1;

		void set(int done, int total) {
			this.done = done;
			this.total = total;
			revalidate();
			repaint();
		}

		public Dimension getPreferredSize() {
			return new Dimension(Math.min(total, 12) * 34, 28);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			for (int i = 0; i < Math.min(total, 12); i++) {
				int x = i * 34;
				g2.setColor(Theme.INK);
				if (i < done) {
					g2.fillRect(x, 1, 26, 26);
				} else {
					g2.setStroke(new BasicStroke(2f));
					g2.drawRect(x + 1, 2, 24, 24);
				}
			}
			g2.dispose();
		}
	}
}
