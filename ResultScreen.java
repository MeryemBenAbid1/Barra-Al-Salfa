/* =====================================================================
 *  ResultScreen.java  -  screen 4: the verdict
 * ---------------------------------------------------------------------
 *   START  mugshot, the big stamp (CASE CLOSED / CASE FAILED /
 *          ESCAPED / CAUGHT), the case report typed out
 *   END    agent report (+XP counting up, LEVEL UP / PROMOTED stamp),
 *          MOST WANTED
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public class ResultScreen extends JPanel {

	private final Polaroid mugshot;
	private final Stamp verdict = new Stamp("", Theme.INK, 52f, -4);
	private final JLabel reportTitle = new JLabel();
	private final JTextPane report = new JTextPane();
	private final AgentReport agent = new AgentReport();
	private final Leaderboard board = new Leaderboard(6);
	private final DossierButton newCase;
	private final DossierButton lobby;
	private final JPanel info = new JPanel();
	private final JPanel right = new JPanel(new BorderLayout(0, 18));
	private Timer typing;

	// what the server told us (kept so the report can be rebuilt in the other language)
	private String outsider = "";
	private String word = "";
	private String winnerName = "";      // "" = the players won
	private String reason = "";
	private String raw = "";             // the whole line, if we could not read it
	private String verdictKey = "";
	private Color verdictColor = Theme.INK;

	public ResultScreen() {
		setOpaque(false);
		setLayout(new BorderLayout(28, 0));

		// ================= mugshot + report =================
		JPanel main = new JPanel(new BorderLayout(26, 0));
		main.setOpaque(false);
		mugshot = new Polaroid("mugshot", "Al-Dakheel unmasked", "", -3, true);
		mugshot.setCaptionKey("al_dakheel");
		mugshot.setPreferredSize(new Dimension(280, 380));
		JPanel photoHolder = new JPanel(new GridBagLayout());
		photoHolder.setOpaque(false);
		photoHolder.add(mugshot);
		main.add(photoHolder, BorderLayout.LINE_START);

		info.setOpaque(false);
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		// fixed-size holder, so nothing jumps when the stamp appears
		JPanel verdictHolder = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		verdictHolder.setOpaque(false);
		verdictHolder.add(verdict);
		verdictHolder.setPreferredSize(new Dimension(520, 120));
		verdictHolder.setMaximumSize(new Dimension(2000, 120));
		info.add(Box.createVerticalStrut(20));
		info.add(verdictHolder);
		info.add(Box.createVerticalStrut(16));

		reportTitle.setForeground(Theme.INK);
		reportTitle.setBorder(new DashedBorder(DashedBorder.BOTTOM, false, Theme.INK, 4));
		reportTitle.setMaximumSize(new Dimension(2000, 34));
		info.add(reportTitle);
		info.add(Box.createVerticalStrut(10));

		report.setEditable(false);
		report.setOpaque(false);
		report.setForeground(Theme.INK);
		report.setMaximumSize(new Dimension(2000, 170));
		report.setPreferredSize(new Dimension(380, 170));
		info.add(report);
		info.add(Box.createVerticalStrut(14));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 0));
		buttons.setOpaque(false);
		newCase = new DossierButton("", DossierButton.INK, 19f, 0);
		newCase.setPreferredSize(new Dimension(200, 64));
		lobby = new DossierButton("", DossierButton.OUTLINE, 16f, 0);
		lobby.setPreferredSize(new Dimension(150, 64));
		buttons.add(newCase);
		buttons.add(lobby);
		buttons.setMaximumSize(new Dimension(2000, 70));
		info.add(buttons);
		info.add(Box.createVerticalGlue());
		main.add(info, BorderLayout.CENTER);
		add(main, BorderLayout.CENTER);

		// ================= agent report + board =================
		right.setOpaque(false);
		right.setPreferredSize(new Dimension(320, 100));
		agent.setPreferredSize(new Dimension(290, 230));
		right.add(agent, BorderLayout.NORTH);
		right.add(board, BorderLayout.CENTER);
		add(right, BorderLayout.LINE_END);
		applyLanguage();
	}

	public DossierButton getNewCaseButton() { return newCase; }
	public DossierButton getLobbyButton() { return lobby; }
	public Leaderboard getBoard() { return board; }

	public void applyLanguage() {
		Theme.text(reportTitle, Theme.stencil(18f), Lang.t("case_report"));
		newCase.setText(Lang.t("new_case"));
		lobby.setText(Lang.t("lobby"));
		right.setBorder(new DashedBorder(Lang.rtl() ? DashedBorder.RIGHT : DashedBorder.LEFT, true, Theme.FAINT, 22));
		if (verdictKey.length() > 0) {
			verdict.setStamp(Lang.t(verdictKey), verdictColor);
		}
		Lang.alignColumn(info);
		if (typing == null || !typing.isRunning()) {
			writeReport(buildReport(), false);
		}
		agent.repaint();
		revalidate();
		repaint();
	}

	// RESULT message: "Outsider: X  |  Word: Y  |  Winner: Z  |  reason"
	public void showResult(String line, String myName) {
		outsider = "";
		word = "";
		winnerName = "";
		reason = "";
		raw = "";
		String[] parts = line.split("\\|");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i].trim();
			if (part.startsWith("Outsider:")) {
				outsider = part.substring(9).trim();
			} else if (part.startsWith("WordAr:")) {
				Lang.rememberWord(word, part.substring(7).trim());
			} else if (part.startsWith("Word:")) {
				word = part.substring(5).trim();
			} else if (part.startsWith("Winner:")) {
				String w = part.substring(7).trim();
				// "Al-Dakheel (Fahad)" or the old "Brra Al-Salfa (Fahad)"
				if (w.contains("(") && w.endsWith(")")) {
					winnerName = w.substring(w.indexOf('(') + 1, w.length() - 1);
				}
			} else if (part.length() > 0) {
				reason = part.replace("Brra Al-Salfa", "Al-Dakheel");
			}
		}
		if (outsider.length() == 0) {
			raw = line;                       // unknown format: show it as it is
			mugshot.setCaptionKey("al_dakheel");
		} else {
			mugshot.setCaption(outsider.toUpperCase());
		}

		mugshot.dropIn();
		verdict.setVisible(false);
		verdictKey = "";
		agent.reset(myName);
		writeReport(buildReport(), true);
	}

	private String buildReport() {
		if (raw.length() > 0) {
			return raw;
		}
		if (outsider.length() == 0) {
			return "";
		}
		String winner = winnerName.length() > 0 ? Lang.f("winner_outsider", winnerName) : Lang.t("winner_players");
		return Lang.t("rep_outsider") + "  " + outsider + "\n"
				+ Lang.t("rep_word") + "  " + Lang.word(word) + "\n"
				+ Lang.t("rep_winner") + "  " + winner + "\n\n"
				// the reason is English from the server: in Arabic mode keep it left-to-right
				+ (Lang.rtl() ? "\u202A" + reason + "\u202C" : reason);
	}

	// writes the report, letter by letter if animate is true
	private void writeReport(final String text, boolean animate) {
		if (typing != null) {
			typing.stop();
		}
		report.setText("");
		report.setFont(Theme.safe(Theme.type(20f), text));
		final SimpleAttributeSet align = new SimpleAttributeSet();
		StyleConstants.setAlignment(align, Lang.rtl() ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
		final StyledDocument doc = report.getStyledDocument();
		doc.setParagraphAttributes(0, 1, align, false);
		if (!animate) {
			try {
				doc.insertString(0, text, null);
			} catch (BadLocationException ex) { }
			doc.setParagraphAttributes(0, doc.getLength() + 1, align, false);
			return;
		}
		final int[] index = { 0 };
		typing = new Timer(22, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (index[0] >= text.length()) {
					typing.stop();
					return;
				}
				try {
					doc.insertString(doc.getLength(), text.substring(index[0], index[0] + 1), null);
				} catch (BadLocationException ex) {
					typing.stop();
				}
				index[0]++;
				doc.setParagraphAttributes(0, doc.getLength() + 1, align, false);
			}
		});
		typing.start();
	}

	// the big stamp, key = "v_case_closed", "v_caught", ...
	public void slamVerdict(String key, Color color) {
		verdictKey = key;
		verdictColor = color;
		verdict.setStamp(Lang.t(key), color);
		verdict.revalidate();
		verdict.slam();
	}

	public boolean hasVerdict() {
		return verdict.isVisible();
	}

	// XP message: "160,WIN"
	public void showXp(int xp, boolean won) {
		agent.showResult(xp, won);
	}

	public void showProfile(int level, String rank, int xpTotal, boolean levelUp, boolean newRank) {
		agent.showProfile(level, rank, xpTotal, levelUp, newRank);
	}

	// ================= "AGENT REPORT" card =================
	private static class AgentReport extends JComponent {
		private String name = "";
		private String outcomeKey = "awaiting_report";
		private boolean won = false;
		private int xpShown = 0;
		private float bar = 0f;
		private int level = 0;
		private String rank = "";
		private boolean stamp = false;
		private boolean promoted = false;   // true = new rank, false = only a new level
		private float stampScale = 1f;

		AgentReport() {
			setOpaque(false);
		}

		void reset(String myName) {
			name = myName;
			outcomeKey = "awaiting_report";
			xpShown = 0;
			bar = 0f;
			stamp = false;
			repaint();
		}

		void showResult(final int xp, boolean didWin) {
			won = didWin;
			outcomeKey = didWin ? "mission_won" : "mission_lost";
			Anim.later(500, new Runnable() {
				public void run() {
					Anim.run(900, new Anim.Step() {
						public void update(float t) {
							xpShown = Math.round(xp * Anim.easeOut(t));
							repaint();
						}
					}, null);
				}
			});
			repaint();
		}

		void showProfile(int newLevel, String newRank, int xpTotal, boolean levelUp, boolean rankChanged) {
			level = newLevel;
			rank = newRank;
			promoted = rankChanged;
			final float target = levelUp ? 1f : (xpTotal % 100) / 100f;
			Anim.later(600, new Runnable() {
				public void run() {
					Anim.run(900, new Anim.Step() {
						public void update(float t) {
							bar = target * Anim.easeOut(t);
							repaint();
						}
					}, null);
				}
			});
			if (levelUp) {
				stamp = true;
				stampScale = 0f;
				Anim.later(1600, new Runnable() {
					public void run() {
						Theme.play("stamp");
						Anim.run(240, new Anim.Step() {
							public void update(float t) {
								stampScale = 1.9f - 0.9f * Anim.easeOut(t);
								repaint();
							}
						}, null);
					}
				});
			}
			repaint();
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int w = getWidth();
			int h = getHeight() - 4;
			boolean rtl = Lang.rtl();
			g2.setColor(Theme.CARD_EDGE);
			g2.fillRect(0, 3, w, h);
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, w, h);
			g2.setColor(Theme.CARD_EDGE);
			g2.drawRect(0, 0, w - 1, h - 1);

			int x = 18;
			int tw = w - 36;
			String head = Lang.f("agent_report", rtl ? name : name.toUpperCase());
			g2.setColor(Theme.MUTED);
			g2.setFont(Theme.fit(g2, Theme.stencil(15f), head, tw, 10f));
			g2.drawString(head, Theme.lineX(g2, head, x, tw), 32);

			String outcome = Lang.t(outcomeKey);
			g2.setColor(Theme.INK);
			g2.setFont(Theme.fit(g2, Theme.stencil(32f), outcome, tw, 14f));
			g2.drawString(outcome, Theme.lineX(g2, outcome, x, tw), 72);

			String xpText = "+" + xpShown + " XP";
			g2.setColor(won ? Theme.RED : Theme.INK_SOFT);
			g2.setFont(Theme.stencil(26f));
			g2.drawString(xpText, Theme.lineX(g2, xpText, x, tw), 108);

			// XP bar (fills from the right in Arabic)
			g2.setColor(Theme.INK);
			g2.setStroke(new BasicStroke(2f));
			g2.drawRect(x, 122, tw, 14);
			int fill = (int) ((tw - 4) * bar);
			g2.fillRect(rtl ? x + tw - 2 - fill : x + 2, 124, fill, 11);

			if (level > 0) {
				String clearance = Lang.f("clearance", level);
				g2.setFont(Theme.safe(Theme.monoBold(15f), clearance));
				g2.drawString(clearance, Theme.lineX(g2, clearance, x, tw), 160);
			}

			if (stamp && stampScale > 0f) {
				String text = promoted ? Lang.f("stamp_promoted", Lang.t(rank).toUpperCase()) : Lang.f("stamp_level_up", level);
				Graphics2D s = (Graphics2D) g2.create();
				s.setFont(Theme.fit(s, Theme.stencil(17f), text, w - 60, 10f));
				FontMetrics fm = s.getFontMetrics();
				int sw = fm.stringWidth(text) + 20;
				int sh = fm.getAscent() + 10;
				s.translate(rtl ? w - x - sw / 2.0 : x + sw / 2.0, 192);
				s.rotate(Math.toRadians(-3));
				s.scale(stampScale, stampScale);
				s.setColor(Theme.RED);
				s.setStroke(new BasicStroke(3f));
				s.drawRect(-sw / 2, -sh / 2, sw, sh);
				s.drawString(text, -fm.stringWidth(text) / 2, fm.getAscent() / 2 - 1);
				s.dispose();
			}
			g2.dispose();
		}
	}
}
