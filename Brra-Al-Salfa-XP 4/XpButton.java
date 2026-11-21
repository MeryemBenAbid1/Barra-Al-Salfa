/* =====================================================================
 *  XpButton.java  -  an old-style Windows XP button
 * ---------------------------------------------------------------------
 *  A normal JButton (so addActionListener works as usual) that paints
 *  itself like XP: a soft gradient, a dark blue outline, round corners.
 *   NORMAL = white/beige      (Cancel, Back, How to play...)
 *   GO     = orange           (the main action of a window)
 *   GREEN  = green            (Start round, Play again)
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class XpButton extends JButton {

	public static final int NORMAL = 0, GO = 1, GREEN = 2;

	private int kind;
	private boolean hover = false;
	private float size = 18f;             // the text size

	public XpButton(String text, int kind) {
		super(text);
		this.kind = kind;
		setText(text);
		setForeground(kind == NORMAL ? Xp.INK : Color.WHITE);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setBorderPainted(false);
		setOpaque(false);
		setMargin(new Insets(8, 20, 8, 20));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
			public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
		});
	}

	public void setKind(int kind) {
		this.kind = kind;
		setForeground(kind == NORMAL ? Xp.INK : Color.WHITE);
		repaint();
	}

	public void setSelectedLook(boolean on) {
		setKind(on ? GO : NORMAL);
	}

	// a new text keeps the size, and uses the Arabic font for Arabic
	public void setText(String text) {
		super.setText(text);
		setFont(Xp.fontFor(text, Xp.PIXEL, Xp.hasArabic(text) ? size - 2 : size));
	}

	// a different text size (e.g. small buttons in the server window)
	public void setTextSize(float newSize) {
		size = newSize;
		setText(getText());
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Xp.smooth(g);
		int w = getWidth(), h = getHeight();
		Color top, middle, bottom, edge;
		if (!isEnabled()) {
			top = new Color(0xF4F3EE); middle = new Color(0xEAE8DF); bottom = new Color(0xDCD8CC);
			edge = new Color(0xA8A8A8);
		} else if (kind == GO) {
			top = new Color(0xFFB27A); middle = Xp.ORANGE; bottom = Xp.ORANGE_DARK;
			edge = new Color(0x8A3A0C);
		} else if (kind == GREEN) {
			top = new Color(0x8FDC76); middle = Xp.GREEN; bottom = new Color(0x2D7D29);
			edge = new Color(0x1F5E1C);
		} else {
			top = Color.WHITE; middle = new Color(0xECEBE4); bottom = new Color(0xD6D0BD);
			edge = Xp.BLUE_TEXT;
		}
		boolean down = getModel().isPressed();
		if (down) {
			Color swap = top; top = bottom; bottom = swap;   // pressed = gradient upside down
		}
		// the gradient: top -> middle (60%) -> bottom
		GradientPaint upper = new GradientPaint(0, 0, top, 0, h * 0.6f, middle);
		g2.setPaint(upper);
		g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
		GradientPaint lower = new GradientPaint(0, h * 0.6f, middle, 0, h, bottom);
		g2.setPaint(lower);
		g2.fillRoundRect(0, (int) (h * 0.6f), w - 1, h - 1 - (int) (h * 0.6f), 8, 8);
		g2.setColor(edge);
		g2.setStroke(new BasicStroke(2f));
		g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
		if (hover && isEnabled() && !down) {
			g2.setColor(new Color(255, 200, 60, 180));      // XP's orange hover glow
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(3, 3, w - 7, h - 7, 6, 6);
		}
		g2.dispose();

		// the icon and the text, centered (white text gets a small dark shadow)
		Graphics2D gt = Xp.smooth(g);
		String text = getText();
		Font font = getFont();
		gt.setFont(font);
		FontMetrics fm = gt.getFontMetrics();
		Icon icon = getIcon();
		int gap = (icon != null && text.length() > 0) ? 8 : 0;
		int iconW = icon == null ? 0 : icon.getIconWidth();
		int total = iconW + gap + Xp.textWidth(this, text, font);
		int x = (w - total) / 2 + (down ? 1 : 0);
		int y = (h - fm.getHeight()) / 2 + fm.getAscent() + (down ? 1 : 0);
		if (icon != null) {
			icon.paintIcon(this, gt, x, (h - icon.getIconHeight()) / 2 + (down ? 1 : 0));
		}
		int tx = x + iconW + gap;
		if (kind != NORMAL && isEnabled()) {
			gt.setColor(edge);
			Xp.drawText(this, gt, text, font, tx + 1, y + 1);
		}
		gt.setColor(!isEnabled() ? new Color(0x9A9A9A) : (kind == NORMAL ? Xp.INK : Color.WHITE));
		Xp.drawText(this, gt, text, font, tx, y);
		gt.dispose();
	}

	// the size: text + icon + the margins
	public Dimension getPreferredSize() {
		FontMetrics fm = getFontMetrics(getFont());
		Icon icon = getIcon();
		int iconW = icon == null ? 0 : icon.getIconWidth() + 8;
		int iconH = icon == null ? 0 : icon.getIconHeight();
		Insets m = getMargin();
		return new Dimension(Xp.textWidth(this, getText(), getFont()) + iconW + m.left + m.right,
				Math.max(fm.getHeight(), iconH) + m.top + m.bottom);
	}
}
