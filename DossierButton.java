/* =====================================================================
 *  DossierButton.java  -  the game's buttons, painted by hand
 * ---------------------------------------------------------------------
 *  Styles:
 *    STAMP    red outline, red letters, a little tilted  (OPEN CASE)
 *    RED      filled red                                  (FILE ACCUSATION)
 *    INK      filled black                                (SEND, NEW CASE)
 *    OUTLINE  black outline                               (LOBBY)
 *    DESK     light outline, for buttons on the dark desk (SFX ON)
 *  Hover darkens it a little, a press pushes it down 2 px and
 *  plays sounds/click.wav.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;

public class DossierButton extends JButton {

	public static final int STAMP = 0, RED = 1, INK = 2, OUTLINE = 3, DESK = 4;

	private int style;
	private final float fontSize;
	private final double angle;
	private boolean hover = false;
	private float pop = 1f;   // little scale bump after a click

	public DossierButton(String text, int style, float fontSize, double angleDegrees) {
		super(text);
		this.style = style;
		this.fontSize = fontSize;
		this.angle = angleDegrees;
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
			public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
		});
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Theme.play("click");
				Anim.run(180, new Anim.Step() {
					public void update(float t) {
						pop = 0.92f + 0.08f * Anim.easeBack(t);
						repaint();
					}
				}, null);
			}
		});
	}

	public void setStyle(int newStyle) {
		style = newStyle;
		repaint();
	}

	public Dimension getPreferredSize() {
		if (isPreferredSizeSet()) {
			return super.getPreferredSize();
		}
		FontMetrics fm = getFontMetrics(Theme.stencil(fontSize));
		int w = fm.stringWidth(getText()) + (int) (fontSize * 2.2f);
		int h = (int) (fontSize * 2.6f);
		return new Dimension(w, h);
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		boolean pressed = getModel().isArmed() && getModel().isPressed();
		if (!isEnabled()) {
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
		}

		g2.translate(w / 2.0, h / 2.0 + (pressed ? 2 : 0));
		g2.rotate(Math.toRadians(angle));
		g2.scale(pop, pop);
		// leave room for the tilt
		int bw = (int) (w - Math.abs(angle) * 4) - 6;
		int bh = h - 8 - (int) (Math.abs(angle) * 3);
		int x = -bw / 2;
		int y = -bh / 2;

		Color fill = null;
		Color line = Theme.INK;
		Color textColor = Theme.INK;
		if (style == STAMP) {
			line = Theme.RED;
			textColor = Theme.RED;
			if (hover && isEnabled()) {
				fill = new Color(179, 38, 30, 30);
			}
		} else if (style == RED) {
			line = Theme.RED;
			fill = hover && isEnabled() ? new Color(150, 30, 24) : Theme.RED;
			textColor = Theme.PAPER;
		} else if (style == INK) {
			fill = hover && isEnabled() ? Theme.INK_SOFT : Theme.INK;
			textColor = Theme.PAPER;
		} else if (style == DESK) {
			line = Theme.FOLDER;
			textColor = Theme.FOLDER;
			if (hover && isEnabled()) {
				fill = new Color(217, 196, 154, 40);
			}
		} else {
			if (hover && isEnabled()) {
				fill = new Color(31, 27, 22, 25);
			}
		}

		if (fill != null) {
			g2.setColor(fill);
			g2.fillRoundRect(x, y, bw, bh, 8, 8);
		}
		int stroke = style == STAMP ? 4 : (style == DESK ? 2 : 3);
		g2.setColor(line);
		g2.setStroke(new BasicStroke(stroke));
		g2.drawRoundRect(x + stroke / 2, y + stroke / 2, bw - stroke, bh - stroke, 8, 8);

		g2.setColor(textColor);
		g2.setFont(Theme.safe(Theme.stencil(fontSize), getText()));
		Theme.centerText(g2, getText(), x, y, bw, bh);
		g2.dispose();
	}
}
