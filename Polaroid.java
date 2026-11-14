/* =====================================================================
 *  Polaroid.java  -  a tilted photo pinned to the file
 * ---------------------------------------------------------------------
 *  Shows images/<imageName>.png. If that file does not exist yet, it
 *  shows a dark photo with a "?" silhouette (see README for the names).
 *  mugshot = true uses the brown police-photo color.
 *  dropIn() makes the photo fall onto the paper and settle.
 * ===================================================================== */

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public class Polaroid extends JComponent {

	private final String imageName;
	private final String hint;
	private String caption;
	private final double angle;
	private final boolean mugshot;
	private boolean captionIsKey = false; // true = caption is a Lang key ("exhibit_a")
	private String overlayStamp = "";   // Lang key of a stamp across the photo ("top_secret")

	private float scale = 1f;
	private float alpha = 1f;

	public Polaroid(String imageName, String hint, String caption, double angleDegrees, boolean mugshot) {
		this.imageName = imageName;
		this.hint = hint;
		this.caption = caption;
		this.angle = angleDegrees;
		this.mugshot = mugshot;
		setOpaque(false);
	}

	// a player's name (shown as it is)
	public void setCaption(String caption) {
		this.caption = caption;
		captionIsKey = false;
		repaint();
	}

	// a translated caption, e.g. setCaptionKey("exhibit_a")
	public void setCaptionKey(String key) {
		this.caption = key;
		captionIsKey = true;
		repaint();
	}

	public void setOverlayStamp(String text) {
		overlayStamp = text;
		repaint();
	}

	public void dropIn() {
		Anim.run(420, new Anim.Step() {
			public void update(float t) {
				scale = 1.35f - 0.35f * Anim.easeBack(t);
				alpha = Math.min(1f, t * 2.5f);
				repaint();
			}
		}, null);
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		// the photo is a bit smaller than the component so the tilt fits
		int pw = (int) (w * 0.9);
		int ph = (int) (h * 0.92);
		int bottom = caption.length() > 0 ? 40 : 26;

		g2.translate(w / 2.0, h / 2.0);
		g2.rotate(Math.toRadians(angle));
		g2.scale(scale, scale);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		int x = -pw / 2;
		int y = -ph / 2;

		// shadow + white frame
		g2.setColor(new Color(0, 0, 0, 50));
		g2.fillRect(x + 3, y + 5, pw, ph);
		g2.setColor(Color.WHITE);
		g2.fillRect(x, y, pw, ph);

		int ix = x + 11;
		int iy = y + 11;
		int iw = pw - 22;
		int ih = ph - 11 - bottom;
		BufferedImage img = Theme.image(imageName);
		if (img != null) {
			Theme.cover(g2, img, ix, iy, iw, ih);
		} else {
			// no image file: a dark photo with height lines and a silhouette
			// (no text, so the screen stays clean)
			g2.setColor(mugshot ? new Color(74, 62, 49) : Theme.INK_SOFT);
			g2.fillRect(ix, iy, iw, ih);
			g2.setColor(new Color(96, 82, 66));
			for (int ly = iy + 26; ly < iy + ih; ly += 26) {
				g2.fillRect(ix, ly, iw, 1);
			}
			drawSilhouette(g2, ix, iy, iw, ih);
		}

		if (caption.length() > 0) {
			String text = captionIsKey ? Lang.t(caption) : caption;
			g2.setColor(Theme.INK);
			g2.setFont(Theme.fit(g2, Theme.type(18f), text, pw - 20, 11f));
			Theme.centerText(g2, text, x, y + ph - bottom, pw, bottom);
		}

		if (overlayStamp.length() > 0) {
			g2.translate(0, ih * 0.22);
			g2.rotate(Math.toRadians(-12));
			String stamp = Lang.t(overlayStamp);
			g2.setFont(Theme.safe(Theme.stencil(24f), stamp));
			FontMetrics fm = g2.getFontMetrics();
			int sw = fm.stringWidth(stamp) + 24;
			int sh = fm.getAscent() + 12;
			g2.setColor(new Color(179, 38, 30, 190));
			g2.setStroke(new BasicStroke(4f));
			g2.drawRect(-sw / 2, -sh / 2, sw, sh);
			g2.drawString(stamp, -fm.stringWidth(stamp) / 2, fm.getAscent() / 2 - 2);
		}
		g2.dispose();
	}

	// a simple "unknown person": a round head and the shoulders
	private void drawSilhouette(Graphics2D g2, int x, int y, int w, int h) {
		int size = Math.min(w, h);
		int head = size * 38 / 100;
		int cx = x + w / 2;
		int headY = y + h - size * 88 / 100;
		g2.setColor(new Color(31, 27, 22, 170));
		g2.fillOval(cx - head / 2, headY, head, head);
		int bodyW = size * 78 / 100;
		g2.fillRoundRect(cx - bodyW / 2, headY + head + 4, bodyW, y + h - (headY + head + 4) + 30, bodyW / 2, bodyW / 2);
		g2.setColor(Theme.PAPER);
		g2.setFont(Theme.stencil(head * 0.55f));
		Theme.centerText(g2, "?", cx - head / 2, headY, head, head);
	}
}
