/* =====================================================================
 *  XpWindow.java  -  a window in the Windows XP style
 * ---------------------------------------------------------------------
 *  It is a JPanel that paints:
 *    - the blue title bar (icon, title, and the red X if the window
 *      can be closed; nothing is drawn that does not work)
 *    - an optional menu bar with "Help" (opens How to play)
 *    - the beige body, where each screen puts its own components
 *  The ACTIVE window has the bright blue title bar. The others are
 *  INACTIVE (faded blue), exactly like Windows XP.
 *
 *  Use:   XpWindow w = new XpWindow("Chat", Xp.pixelIcon(Xp.CHAT, 2));
 *         w.body().add(...);       // the content
 *         w.setBounds(x, y, width, height);
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class XpWindow extends JPanel {

	public static final int BAR = 34;          // height of the title bar

	private String title;
	private Icon icon;
	private boolean active = true;
	private final JPanel body = new JPanel();
	private JPanel menuBar = null;
	private ActionListener closeListener = null;

	// what the "Help" menu does (SpyClient sets it: open How to play)

	public XpWindow(String title, Icon icon) {
		this.title = title;
		this.icon = icon;
		setOpaque(false);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(BAR, 3, 7, 7));    // room for the title bar, the frame and the shadow

		body.setBackground(Xp.FACE);
		body.setBorder(new EmptyBorder(14, 16, 14, 16));
		body.setLayout(new BorderLayout());
		add(body, BorderLayout.CENTER);

		// a click on the X button closes the window (one click: press + release on the X)
		addMouseListener(new MouseAdapter() {
			private boolean down = false;
			public void mousePressed(MouseEvent e) {
				down = onCloseButton(e.getPoint());
			}
			public void mouseReleased(MouseEvent e) {
				if (down && onCloseButton(e.getPoint())) {
					closeListener.actionPerformed(new ActionEvent(XpWindow.this, 0, "close"));
				}
				down = false;
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				setCursor(Cursor.getPredefinedCursor(onCloseButton(e.getPoint()) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			}
		});
	}

	// the panel where the screen puts its components
	public JPanel body() {
		return body;
	}

	public void setTitle(String title) {
		this.title = title;
		repaint();
	}

	public void setActive(boolean on) {
		active = on;
		repaint();
	}

	public boolean isActive() {
		return active;
	}

	// what to do when the X button is clicked (null = no X button)
	public void setCloseListener(ActionListener listener) {
		closeListener = listener;
		repaint();
	}

	// where the X button is: top right (top left in Arabic, mirrored)
	private Rectangle closeButton() {
		int x = Xp.rtl(this) ? 7 : getWidth() - 4 - 25 - 4;
		return new Rectangle(x, 6, 23, 23);
	}

	private boolean onCloseButton(Point p) {
		return closeListener != null && closeButton().contains(p);
	}

	// the menu bar under the title: only "Help", and it works
	public void helpMenu() {
		if (menuBar != null) {
			remove(menuBar);
		}
		menuBar = new JPanel(new FlowLayout(FlowLayout.LEADING, 14, 4));
		menuBar.setBackground(Xp.FACE);
		menuBar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCFC9B4)));
		String text = Lang.t("m_help");
		JLabel item = new JLabel(text);
		item.setFont(Xp.fontFor(text, Xp.UI, 15f));
		item.setForeground(Xp.INK);
		Xp.onClick(item, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// the desktop we are on (or the desktop of our pop-up dialog)
				Desktop desktop = (Desktop) SwingUtilities.getAncestorOfClass(Desktop.class, XpWindow.this);
				Window w = SwingUtilities.getWindowAncestor(XpWindow.this);
				if (desktop == null && w instanceof XpDialog) {
					desktop = ((XpDialog) w).desktop();
				}
				if (desktop != null && desktop.helpListener != null) {
					desktop.helpListener.actionPerformed(e);
				}
			}
		});
		menuBar.add(item);
		add(menuBar, BorderLayout.NORTH);
		revalidate();
	}

	// =================================================================
	// PAINTING: frame + title bar (the body paints itself)
	// =================================================================
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Xp.smooth(g);
		int w = getWidth(), h = getHeight();

		// a soft shadow under the window
		g2.setColor(new Color(0, 0, 0, 40));
		g2.fillRoundRect(4, 6, w - 4, h - 4, 10, 10);

		// the frame: blue (active) or faded blue (inactive), round top corners
		Color frame = active ? Xp.BLUE_DARK : new Color(0x7B9BE6);
		g2.setColor(frame);
		g2.fillRoundRect(0, 0, w - 4, h - 4, 16, 16);
		g2.fillRect(0, 12, w - 4, h - 16);

		// the title bar gradient (the XP "Luna" blue)
		float[] stops = { 0f, 0.08f, 0.4f, 0.88f, 1f };
		Color[] colors;
		if (active) {
			colors = new Color[] { new Color(0x0997FF), new Color(0x0053EE), new Color(0x0050EE), new Color(0x0066FF), new Color(0x003DD7) };
		} else {
			colors = new Color[] { new Color(0xA6BDF1), new Color(0x7B9BE6), new Color(0x7A98E2), new Color(0x7A98E2), new Color(0x6A88D6) };
		}
		LinearGradientPaint bar = new LinearGradientPaint(0, 0, 0, BAR, stops, colors);
		g2.setPaint(bar);
		g2.fillRoundRect(0, 0, w - 4, BAR + 8, 16, 16);
		g2.setColor(frame);
		g2.fillRect(0, BAR, w - 4, 1);

		// icon + title (on the left; on the right in Arabic)
		boolean rtl = Xp.rtl(this);
		g2.setFont(Xp.fontFor(title, Xp.TITLE, 17f));
		FontMetrics fm = g2.getFontMetrics();
		int iconW = icon == null ? 0 : icon.getIconWidth() + 8;
		int x = rtl ? w - 4 - 9 - iconW - fm.stringWidth(title) : 9;
		if (icon != null) {
			int ix = rtl ? w - 4 - 9 - icon.getIconWidth() : x;
			icon.paintIcon(this, g2, ix, (BAR - icon.getIconHeight()) / 2);
			if (!rtl) {
				x = x + iconW;
			}
		}
		int ty = (BAR - fm.getHeight()) / 2 + fm.getAscent();
		g2.setColor(new Color(0x0A1E8C));
		g2.drawString(title, x + 1, ty + 1);           // the small dark shadow
		g2.setColor(Color.WHITE);
		g2.drawString(title, x, ty);

		// the red X, only if this window can be closed
		if (closeListener != null) {
			Rectangle b = closeButton();
			drawTitleButton(g2, b.x, b.y, 2);
		}
		g2.dispose();
	}

	private void drawTitleButton(Graphics2D g2, int x, int y, int which) {
		int s = 22;
		Color a, b;
		if (which == 2) {
			a = new Color(0xF3A183); b = new Color(0xC63D1C);          // red close
		} else {
			a = new Color(0x5B9CFF); b = new Color(0x1048C8);          // blue
		}
		if (!active) {
			a = new Color(a.getRed(), a.getGreen(), a.getBlue(), 150);
			b = new Color(b.getRed(), b.getGreen(), b.getBlue(), 150);
		}
		g2.setPaint(new GradientPaint(x, y, a, x + s, y + s, b));
		g2.fillRoundRect(x, y, s, s, 5, 5);
		g2.setColor(Color.WHITE);
		g2.setStroke(new BasicStroke(1f));
		g2.drawRoundRect(x, y, s, s, 5, 5);
		g2.setStroke(new BasicStroke(2.2f));
		if (which == 0) {
			g2.fillRect(x + 6, y + s - 8, 8, 3);                       // _
		} else if (which == 1) {
			g2.drawRect(x + 6, y + 6, 10, 10);                         // []
			g2.fillRect(x + 6, y + 6, 10, 3);
		} else {
			g2.drawLine(x + 7, y + 7, x + s - 7, y + s - 7);           // X
			g2.drawLine(x + s - 7, y + 7, x + 7, y + s - 7);
		}
	}
}
