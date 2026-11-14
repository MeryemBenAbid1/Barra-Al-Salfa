/* =====================================================================
 *  Stamp.java  -  a rubber stamp ("TOP SECRET", "CASE CLOSED", ...)
 * ---------------------------------------------------------------------
 *  A tilted box with a thick border and stencil letters.
 *  slam() = the signature animation: it drops in big, shrinks to its
 *  size, shakes a little and plays sounds/stamp.wav.
 * ===================================================================== */

import java.awt.*;
import javax.swing.JComponent;

public class Stamp extends JComponent {

	private String text;
	private Color color;
	private final float fontSize;
	private final double angle;     // tilt in degrees

	private float scale = 1f;       // used by the animation
	private float alpha = 1f;
	private int shakeX = 0;
	private Color fill = null;      // optional background (for stamps on the dark desk)

	public Stamp(String text, Color color, float fontSize, double angleDegrees) {
		this.text = text;
		this.color = color;
		this.fontSize = fontSize;
		this.angle = angleDegrees;
		setOpaque(false);
	}

	public void setFill(Color fill) {
		this.fill = fill;
	}

	public void setStamp(String newText, Color newColor) {
		text = newText;
		color = newColor;
		revalidate();
		repaint();
	}

	public String getText() {
		return text;
	}

	// show the stamp with the slam animation
	public void slam() {
		setVisible(true);
		Theme.play("stamp");
		Anim.run(240, new Anim.Step() {
			public void update(float t) {
				float e = Anim.easeOut(t);
				scale = 1.9f - 0.9f * e;
				alpha = Math.min(1f, t * 3f);
				repaint();
			}
		}, new Runnable() {
			public void run() {
				Anim.run(160, new Anim.Step() {
					public void update(float t) {
						shakeX = (int) Math.round(Math.sin(t * Math.PI * 6) * 4 * (1 - t));
						repaint();
					}
				}, null);
			}
		});
	}

	private Font font() {
		return Theme.safe(Theme.stencil(fontSize), text);
	}

	private int border() {
		return Math.max(3, Math.round(fontSize / 7f));
	}

	public Dimension getPreferredSize() {
		FontMetrics fm = getFontMetrics(font());
		int w = fm.stringWidth(text) + (int) (fontSize * 0.9f) + border() * 2;
		int h = fm.getAscent() + (int) (fontSize * 0.45f) + border() * 2;
		double rad = Math.toRadians(Math.abs(angle));
		int rw = (int) (w * Math.cos(rad) + h * Math.sin(rad)) + 8;
		int rh = (int) (w * Math.sin(rad) + h * Math.cos(rad)) + 8;
		return new Dimension(rw, rh);
	}

	protected void paintComponent(Graphics g) {
		if (text == null || text.length() == 0) {
			return;
		}
		Graphics2D g2 = Theme.smooth(g);
		Font f = font();
		g2.setFont(f);
		FontMetrics fm = g2.getFontMetrics();
		int w = fm.stringWidth(text) + (int) (fontSize * 0.9f) + border() * 2;
		int h = fm.getAscent() + (int) (fontSize * 0.45f) + border() * 2;

		g2.translate(getWidth() / 2.0 + shakeX, getHeight() / 2.0);
		g2.rotate(Math.toRadians(angle));
		g2.scale(scale, scale);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.9f));

		if (fill != null) {
			g2.setColor(fill);
			g2.fillRect(-w / 2, -h / 2, w, h);
		}
		g2.setColor(color);
		g2.setStroke(new BasicStroke(border()));
		g2.drawRect(-w / 2 + border() / 2, -h / 2 + border() / 2, w - border(), h - border());
		int tx = -fm.stringWidth(text) / 2;
		int ty = fm.getAscent() / 2 - 1;
		g2.drawString(text, tx, ty);
		g2.dispose();
	}
}
