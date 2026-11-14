/* =====================================================================
 *  Hud.java  -  the strip on top of the folder
 * ---------------------------------------------------------------------
 *  [EVIDENCE: FOOD] [phase stamp] [timer] [عربي] [SFX] [agent badge]
 *  The first three only show during a case (round).
 *  The language button switches English <-> Arabic.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Hud extends JPanel {

	private final Chip category = new Chip();
	private final Stamp phase = new Stamp("", Theme.INK, 16f, -2);
	private final TimerBox timer = new TimerBox();
	private final Badge badge = new Badge();
	private final DossierButton sound;
	private final DossierButton language;
	private String phaseKey = "";
	private Color phaseColor = Theme.INK;

	public Hud() {
		setOpaque(false);
		setLayout(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		sound = new DossierButton("", DossierButton.DESK, 12f, 0);
		sound.setPreferredSize(new Dimension(110, 40));
		sound.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Theme.soundOn = !Theme.soundOn;
				sound.setText(Lang.t(Theme.soundOn ? "sfx_on" : "sfx_off"));
			}
		});
		language = new DossierButton("", DossierButton.DESK, 14f, 0);
		language.setPreferredSize(new Dimension(70, 40));
		phase.setFill(Theme.PAPER);
		badge.setVisible(false);
		add(category);
		add(phase);
		add(timer);
		add(language);
		add(sound);
		add(badge);
		clearRound();
		applyLanguage();
	}

	// the language button: SpyClient decides what happens on click
	public DossierButton getLanguageButton() {
		return language;
	}

	public void applyLanguage() {
		sound.setText(Lang.t(Theme.soundOn ? "sfx_on" : "sfx_off"));
		language.setText(Lang.t("lang_button"));
		if (phaseKey.length() > 0) {
			phase.setStamp(Lang.t(phaseKey), phaseColor);
		}
		category.revalidate();
		badge.revalidate();
		repaint();
	}

	public void setCategory(String text) {
		category.text = text.trim();
		category.setVisible(true);
		category.revalidate();
		category.repaint();
	}

	// shows a new phase stamp, key = "ph_interrogation", "ph_accusation", ...
	public void setPhase(String key, Color color) {
		if (key.equals(phaseKey) && phase.isVisible()) {
			return;
		}
		phaseKey = key;
		phaseColor = color;
		phase.setStamp(Lang.t(key), color);
		phase.slam();
		revalidate();
	}

	public void setTime(int seconds) {
		timer.setSeconds(seconds);
		timer.setVisible(true);
	}

	public void hideTimer() {
		timer.setVisible(false);
	}

	public void clearRound() {
		category.setVisible(false);
		phase.setVisible(false);
		phase.setStamp("", Theme.INK);
		phaseKey = "";
		timer.setVisible(false);
	}

	public void hideBadge() {
		badge.setVisible(false);
	}

	public void setName(String name) {
		badge.setVisible(true);
		badge.name = name;
		badge.revalidate();
		badge.repaint();
	}

	public void setProfile(String name, String rank, int level, int xp) {
		badge.setVisible(true);
		badge.name = name;
		badge.rank = rank;
		badge.level = level;
		badge.xp = xp;
		badge.revalidate();
		badge.repaint();
	}

	// ---------- "EVIDENCE: FOOD" ----------
	private static class Chip extends JComponent {
		String text = "";

		private String label() { return Lang.t("evidence"); }
		private String value() { return Lang.category(text).toUpperCase(); }

		public Dimension getPreferredSize() {
			Font f = Theme.safe(Theme.monoBold(15f), label() + value());
			FontMetrics a = getFontMetrics(f);
			return new Dimension(a.stringWidth(label() + value()) + 34, 44);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int h = 38;
			g2.setColor(Theme.SHADOW);
			g2.fillRoundRect(0, 3, getWidth(), h, 8, 8);
			g2.setColor(Theme.PAPER);
			g2.fillRoundRect(0, 0, getWidth(), h, 8, 8);
			String label = label();
			String value = value();
			g2.setFont(Theme.safe(Theme.monoBold(15f), label + value));
			FontMetrics fm = g2.getFontMetrics();
			int y = (h - fm.getHeight()) / 2 + fm.getAscent();
			int labelW = fm.stringWidth(label);
			int valueW = fm.stringWidth(value);
			int labelX = Lang.rtl() ? getWidth() - 14 - labelW : 14;
			int valueX = Lang.rtl() ? labelX - valueW : 14 + labelW;
			g2.setColor(Theme.MUTED);
			g2.drawString(label, labelX, y);
			g2.setColor(Theme.INK);
			g2.drawString(value, valueX, y);
			g2.dispose();
		}
	}

	// ---------- the countdown ----------
	private static class TimerBox extends JComponent {
		private int seconds = 0;
		private float pulse = 1f;

		void setSeconds(int s) {
			seconds = s;
			if (s <= 10 && s > 0) {
				Theme.play("tick");
				Anim.run(300, new Anim.Step() {
					public void update(float t) {
						pulse = 1.18f - 0.18f * Anim.easeOut(t);
						repaint();
					}
				}, null);
			}
			repaint();
		}

		public Dimension getPreferredSize() {
			return new Dimension(130, 46);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int h = 42;
			boolean urgent = seconds <= 10;
			g2.setColor(Theme.SHADOW);
			g2.fillRoundRect(0, 3, getWidth(), h, 8, 8);
			g2.setColor(urgent ? new Color(60, 14, 12) : Theme.INK);
			g2.fillRoundRect(0, 0, getWidth(), h, 8, 8);
			Color c = urgent ? Theme.RED_LIGHT : Theme.PAPER;
			g2.setColor(c);
			// clock icon
			g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawOval(14, 12, 18, 18);
			g2.drawLine(23, 16, 23, 21);
			g2.drawLine(23, 21, 26, 24);
			g2.drawLine(19, 8, 27, 8);
			// time, scaled by the pulse
			String text = (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
			g2.setFont(Theme.stencil(26f));
			FontMetrics fm = g2.getFontMetrics();
			double cx = 42 + (getWidth() - 42) / 2.0;
			g2.translate(cx, h / 2.0);
			g2.scale(pulse, pulse);
			g2.drawString(text, -fm.stringWidth(text) / 2, fm.getAscent() / 2 - 2);
			g2.dispose();
		}
	}

	// ---------- the agent ID badge ----------
	private static class Badge extends JComponent {
		String name = "";
		String rank = "Rookie";
		int level = 1;
		int xp = 0;

		private String line1() { return Lang.f("agent", Lang.rtl() ? name : name.toUpperCase()); }
		private String line2() { return Lang.f("clearance_rank", level, Lang.t(rank).toUpperCase()); }
		private String line3() { return Lang.f("xp_of", xp % 100); }

		public Dimension getPreferredSize() {
			FontMetrics fm = getFontMetrics(Theme.safe(Theme.monoBold(13f), line2()));
			FontMetrics tm = getFontMetrics(Theme.safe(Theme.type(16f), line1()));
			int text = Math.max(fm.stringWidth(line2()), tm.stringWidth(line1()));
			return new Dimension(text + 72, 56);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int w = getWidth();
			int h = 52;
			boolean rtl = Lang.rtl();
			g2.setColor(Theme.SHADOW);
			g2.fillRoundRect(0, 3, w, h, 8, 8);
			g2.setColor(Theme.PAPER);
			g2.fillRoundRect(0, 0, w, h, 8, 8);
			// photo square: left in English, right in Arabic
			int px = rtl ? w - 46 : 6;
			g2.setColor(Theme.INK_SOFT);
			g2.fillRect(px, 6, 40, h - 12);
			g2.setColor(Theme.PAPER);
			String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
			g2.setFont(Theme.safe(Theme.stencil(20f), initial));
			Theme.centerText(g2, initial, px, 6, 40, h - 12);

			int tx = rtl ? 8 : 56;
			int tw = w - 64;
			String l1 = line1(), l2 = line2(), l3 = line3();
			g2.setColor(Theme.INK);
			g2.setFont(Theme.safe(Theme.type(16f), l1));
			g2.drawString(l1, Theme.lineX(g2, l1, tx, tw), 19);
			g2.setFont(Theme.safe(Theme.monoBold(13f), l2));
			g2.drawString(l2, Theme.lineX(g2, l2, tx, tw), 34);
			g2.setColor(Theme.MUTED);
			g2.setFont(Theme.safe(Theme.monoBold(13f), l3));
			g2.drawString(l3, Theme.lineX(g2, l3, tx, tw), 48);
			g2.dispose();
		}
	}
}
