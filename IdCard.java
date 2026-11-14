/* =====================================================================
 *  IdCard.java  -  one agent's ID card (lobby slots and the round lineup)
 * ---------------------------------------------------------------------
 *  Empty slot  -> an empty dashed box
 *  New player  -> revealIn(): a black redaction bar slides off the card
 *  "YOU" stamp -> setStamp("YOU", true) slams a red stamp on the photo
 * ===================================================================== */

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public class IdCard extends JComponent {

	private String name = null;           // null = empty slot
	private String subtitle = "";
	private String status = "";
	private Color statusColor = Theme.MUTED;
	private Color edge = Theme.CARD_EDGE;

	private String stamp = "";            // Lang key of the stamp ("you")
	private float stampScale = 1f;
	private float stampAlpha = 1f;
	private float cover = 0f;             // 1 = redaction bar covers the card

	public IdCard() {
		setOpaque(false);
		setPreferredSize(new Dimension(140, 180));
	}

	public String getPlayerName() {
		return name;
	}

	public void setEmpty() {
		name = null;
		stamp = "";
		repaint();
	}

	public void setPlayer(String name, String subtitle) {
		this.name = name;
		this.subtitle = subtitle;
		repaint();
	}

	public void setStatus(String status, Color statusColor, Color edge) {
		this.status = status;
		this.statusColor = statusColor;
		this.edge = edge;
		repaint();
	}

	public void setStamp(String text, boolean animate) {
		stamp = text;
		if (!animate || text.length() == 0) {
			stampScale = 1f;
			stampAlpha = 1f;
			repaint();
			return;
		}
		Theme.play("stamp");
		Anim.run(240, new Anim.Step() {
			public void update(float t) {
				stampScale = 1.9f - 0.9f * Anim.easeOut(t);
				stampAlpha = Math.min(1f, t * 3f);
				repaint();
			}
		}, null);
	}

	// the redaction bar slides away to show the new agent
	public void revealIn(int delay) {
		cover = 1f;
		repaint();
		Anim.later(delay, new Runnable() {
			public void run() {
				Theme.play("reveal");
				Anim.run(380, new Anim.Step() {
					public void update(float t) {
						cover = 1f - Anim.easeOut(t);
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

		if (name == null) {
			// ---- empty slot ----
			g2.setColor(new Color(181, 168, 142));
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 7f, 5f }, 0f));
			g2.drawRect(1, 1, w - 3, h - 3);      // just a dashed box, no text
			g2.dispose();
			return;
		}

		// ---- card ----
		g2.setColor(Theme.CARD_EDGE);
		g2.fillRect(0, 2, w, h - 2);              // little shadow at the bottom
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, w, h - 2);
		boolean strong = edge != Theme.CARD_EDGE;
		g2.setColor(edge);
		g2.setStroke(new BasicStroke(strong ? 2.5f : 1f));
		g2.drawRect(1, 1, w - 3, h - 4);

		// ---- photo ----
		int pad = 9;
		int photoH = Math.max(60, (int) (h * 0.48));
		g2.setColor(Theme.INK_SOFT);
		g2.fillRect(pad, pad, w - pad * 2, photoH);
		BufferedImage avatar = Theme.image("avatar_" + name.toLowerCase());
		if (avatar != null) {
			Theme.cover(g2, avatar, pad, pad, w - pad * 2, photoH);
		} else {
			g2.setColor(Theme.PAPER);
			String initial = name.substring(0, 1).toUpperCase();
			g2.setFont(Theme.safe(Theme.stencil(photoH * 0.45f), initial));
			Theme.centerText(g2, initial, pad, pad, w - pad * 2, photoH);
		}

		// ---- text ----
		int y = pad + photoH + 8;
		g2.setColor(Theme.INK);
		int textW = w - pad * 2;
		g2.setFont(Theme.fit(g2, Theme.type(18f), name, textW, 11f));
		g2.drawString(name, Theme.lineX(g2, name, pad, textW), y + g2.getFontMetrics().getAscent());
		y += g2.getFontMetrics().getHeight() + 2;
		g2.setColor(Theme.MUTED);
		g2.setFont(Theme.fit(g2, Theme.monoBold(12f), subtitle, textW, 9f));
		g2.drawString(subtitle, Theme.lineX(g2, subtitle, pad, textW), y + g2.getFontMetrics().getAscent());
		y += g2.getFontMetrics().getHeight();
		if (status.length() > 0) {
			g2.setColor(statusColor);
			g2.setFont(Theme.fit(g2, Theme.monoBold(12f), status, textW, 9f));
			g2.drawString(status, Theme.lineX(g2, status, pad, textW), y + g2.getFontMetrics().getAscent());
		}

		// ---- stamp on the photo ----
		if (stamp.length() > 0) {
			String stampText = Lang.t(stamp);
			Graphics2D s = (Graphics2D) g2.create();
			s.setFont(Theme.safe(Theme.stencil(15f), stampText));
			FontMetrics fm = s.getFontMetrics();
			int sw = fm.stringWidth(stampText) + 14;
			int sh = fm.getAscent() + 8;
			double sx = Lang.rtl() ? pad + sw / 2.0 + 2 : w - pad - sw / 2.0 - 2;
			s.translate(sx, pad + sh / 2.0 + 10);
			s.rotate(Math.toRadians(12));
			s.scale(stampScale, stampScale);
			s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, stampAlpha));
			s.setColor(new Color(242, 234, 218, 220));
			s.fillRect(-sw / 2, -sh / 2, sw, sh);
			s.setColor(Theme.RED);
			s.setStroke(new BasicStroke(3f));
			s.drawRect(-sw / 2, -sh / 2, sw, sh);
			s.drawString(stampText, -fm.stringWidth(stampText) / 2, fm.getAscent() / 2 - 1);
			s.dispose();
		}

		// ---- redaction bar (slides to the right) ----
		if (cover > 0f) {
			int barX = (int) (w * (1f - cover));
			g2.setColor(Theme.INK);
			g2.fillRect(barX, 0, w - barX, h);
			if (cover > 0.6f) {
				String label = Lang.t("classified");
				g2.setColor(Theme.PAPER);
				g2.setFont(Theme.safe(Theme.stencil(15f), label));
				Theme.centerText(g2, label, barX, 0, w - barX, h);
			}
		}
		g2.dispose();
	}
}
