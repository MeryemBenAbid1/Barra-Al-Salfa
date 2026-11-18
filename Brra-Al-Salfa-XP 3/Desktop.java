/* =====================================================================
 *  Desktop.java  -  the XP desktop behind every screen
 * ---------------------------------------------------------------------
 *  - the wallpaper (blue sky + green hills, images/wallpaper.png)
 *  - the desktop icons on the left (How to play, TV screen, Rules, Scores)
 *  - the taskbar at the bottom: "start", one tab per open window,
 *    the language button (عربي / EN) and the clock
 *  - the yellow balloon notice that pops up above the clock
 *
 *  The windows of a screen are placed on it with exact positions
 *  (null layout), exactly like in the design.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Desktop extends JPanel {

	public static final int WIDTH = 1280, HEIGHT = 800, TASKBAR = 40;

	private final Taskbar taskbar = new Taskbar();
	private final JPanel icons = Xp.clear(null);
	private Balloon balloon = null;
	private javax.swing.Timer balloonTimer = null;

	public Desktop() {
		setLayout(null);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		taskbar.setBounds(0, HEIGHT - TASKBAR, WIDTH, TASKBAR);
		add(taskbar);
		icons.setBounds(0, 0, 120, 420);
		add(icons);
	}

	// Windows on the desktop can overlap. Swing normally assumes children
	// never overlap and repaints one alone; then a window BEHIND (e.g. the
	// chat getting a new line) would paint over the window in front.
	// false = always paint the windows in their real order: no glitches.
	public boolean isOptimizedDrawingEnabled() {
		return false;
	}

	// what "Help" in the menu bar of every window does (set by SpyClient)
	public ActionListener helpListener = null;

	public Taskbar taskbar() {
		return taskbar;
	}

	// =================================================================
	// DESKTOP ICONS  (a cream tile with a pixel picture and a white name)
	// =================================================================
	public void addIcon(String name, String[] picture, final ActionListener onClick) {
		int index = icons.getComponentCount();
		DesktopIcon icon = new DesktopIcon(name, picture);
		icon.setBounds(14, 20 + index * 100, 96, 90);
		Xp.onClick(icon, onClick);          // ONE click opens it
		icons.add(icon);
	}

	public void renameIcon(int index, String name) {
		((DesktopIcon) icons.getComponent(index)).name = name;
		icons.repaint();
	}

	public void showIcons(boolean on) {
		icons.setVisible(on);
	}

	// =================================================================
	// WINDOWS
	// =================================================================

	// removes every window (the taskbar and the icons stay)
	public void clearWindows() {
		Component[] all = getComponents();
		for (int i = 0; i < all.length; i++) {
			if (all[i] != taskbar && all[i] != icons && all[i] != balloon) {
				remove(all[i]);
			}
		}
		repaint();
	}

	// puts a window on the desktop at x, y with this size
	public void place(XpWindow window, int x, int y, int w, int h) {
		window.setBounds(x, y, w, h);
		Xp.orient(window);                     // Arabic: everything inside reads right to left
		if (window.getParent() != this) {
			add(window);
		}
		setComponentZOrder(window, balloon != null && balloon.getParent() == this ? 1 : 0);
		window.validate();                     // lay it out NOW, so it is never drawn half-empty
		repaint();
	}

	// =================================================================
	// BALLOON  (yellow notice above the clock, closes by itself)
	// =================================================================
	public void balloon(Icon picture, String title, String text) {
		if (balloon != null) {
			remove(balloon);
		}
		balloon = new Balloon(picture, title, text);
		Dimension d = balloon.getPreferredSize();
		balloon.setBounds(WIDTH - d.width - 18, HEIGHT - TASKBAR - d.height - 6, d.width, d.height);
		add(balloon);
		setComponentZOrder(balloon, 0);        // on top of everything
		repaint();
		if (balloonTimer != null) {
			balloonTimer.stop();
		}
		balloonTimer = new javax.swing.Timer(4500, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hideBalloon();
			}
		});
		balloonTimer.setRepeats(false);
		balloonTimer.start();
	}

	public void hideBalloon() {
		if (balloon != null) {
			remove(balloon);
			balloon = null;
			repaint();
		}
	}

	// =================================================================
	// PAINTING THE WALLPAPER
	// =================================================================
	protected void paintComponent(Graphics g) {
		Image wall = Xp.wallpaper();
		if (wall != null) {
			g.drawImage(wall, 0, 0, getWidth(), getHeight(), null);
		} else {
			// no picture: a plain XP sky and a green hill
			Graphics2D g2 = Xp.smooth(g);
			g2.setPaint(new GradientPaint(0, 0, new Color(0x1957D6), 0, 520, new Color(0x9CC8F5)));
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setColor(new Color(0x5AA828));
			g2.fillOval(-300, 470, 1400, 700);
			g2.dispose();
		}
	}

	// =================================================================
	// ONE DESKTOP ICON
	// =================================================================
	static class DesktopIcon extends JComponent {
		String name;
		String[] picture;

		boolean hover = false, down = false;

		DesktopIcon(String name, String[] picture) {
			this.name = name;
			this.picture = picture;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			// XP look: a blue glow under the mouse, darker while pressed
			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
				public void mouseExited(MouseEvent e)  { hover = false; down = false; repaint(); }
				public void mousePressed(MouseEvent e) { down = true; repaint(); }
				public void mouseReleased(MouseEvent e) { down = false; repaint(); }
			});
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Xp.smooth(g);
			int tile = 52, tx = (getWidth() - tile) / 2;
			if (hover || down) {
				g2.setColor(new Color(49, 106, 197, down ? 170 : 110));     // XP selection blue
				g2.fillRoundRect(2, 0, getWidth() - 4, getHeight(), 8, 8);
			}
			g2.setColor(new Color(0, 0, 0, 70));
			g2.fillRoundRect(tx + 1, 3, tile, tile, 10, 10);          // shadow
			g2.setColor(new Color(255, 255, 255, 235));
			g2.fillRoundRect(tx, 0, tile, tile, 10, 10);
			Xp.drawPixels(g2, picture, tx + 6, 6, 5);
			// the name gets smaller until it fits under the tile
			float size = 14f;
			g2.setFont(Xp.fontFor(name, Xp.UI, size));
			while (g2.getFontMetrics().stringWidth(name) > getWidth() - 6 && size > 9f) {
				size = size - 0.5f;
				g2.setFont(Xp.fontFor(name, Xp.UI, size));
			}
			FontMetrics fm = g2.getFontMetrics();
			int x = (getWidth() - fm.stringWidth(name)) / 2;
			g2.setColor(new Color(0, 0, 0, 200));
			g2.drawString(name, x + 1, tile + 19);
			g2.setColor(Color.WHITE);
			g2.drawString(name, x, tile + 18);
			g2.dispose();
		}
	}

	// =================================================================
	// THE TASKBAR
	// =================================================================
	public static class Taskbar extends JPanel {
		private final JPanel tabs = Xp.clear(new FlowLayout(FlowLayout.LEFT, 6, 5));
		private final JLabel clock = new JLabel();
		private final JLabel language = new JLabel();
		private final JLabel extra = new JLabel();

		Taskbar() {
			setLayout(new BorderLayout());
			setOpaque(false);

			// "start" (green, round right side)
			JComponent start = new JComponent() {
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = Xp.smooth(g);
					int w = getWidth(), h = getHeight();
					g2.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[] { 0f, 0.1f, 0.45f, 1f },
							new Color[] { new Color(0x3C9E3C), new Color(0x62C05E), new Color(0x3C9E3C), new Color(0x2F8A2F) }));
					g2.fillRoundRect(-20, 0, w + 20, h, 26, 26);
					Xp.drawPixels(g2, Xp.SMILE, 12, (h - 27) / 2, 3);
					g2.setFont(Xp.TITLE_ITALIC.deriveFont(Font.ITALIC, 21f));
					g2.setColor(new Color(0x1B4D1B));
					g2.drawString("start", 49, 28);
					g2.setColor(Color.WHITE);
					g2.drawString("start", 48, 27);
					g2.dispose();
				}
			};
			start.setPreferredSize(new Dimension(118, TASKBAR));
			add(start, BorderLayout.WEST);
			add(tabs, BorderLayout.CENTER);

			// the tray: language button, smiley, clock
			JPanel tray = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 9)) {
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(), new float[] { 0f, 0.12f, 0.9f, 1f },
							new Color[] { new Color(0x0C59B9), new Color(0x18B9F0), new Color(0x139BEB), new Color(0x1290E8) }));
					g2.fillRect(0, 0, getWidth(), getHeight());
					g2.setColor(new Color(0x092E9E));
					g2.fillRect(0, 0, 1, getHeight());
				}
			};
			extra.setFont(Xp.UI.deriveFont(Font.BOLD, 15f));
			extra.setForeground(new Color(0xFFE08A));
			language.setForeground(Color.WHITE);
			language.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			clock.setFont(Xp.UI.deriveFont(15f));
			clock.setForeground(Color.WHITE);
			tray.add(extra);
			tray.add(language);
			tray.add(new JLabel(Xp.pixelIcon(Xp.SMILE, 2)));
			tray.add(clock);
			tray.setBorder(new EmptyBorder(0, 6, 0, 4));
			add(tray, BorderLayout.EAST);
			setLanguageText("عربي");

			// the clock changes every 10 seconds (javax.swing.Timer)
			tick();
			javax.swing.Timer timer = new javax.swing.Timer(10000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tick();
				}
			});
			timer.start();
		}

		private void tick() {
			clock.setText(new SimpleDateFormat("h:mm a").format(new Date()));
		}

		// the text of the language button: "عربي" or "EN"
		public void setLanguageText(String text) {
			language.setText(text);
			language.setFont(Xp.fontFor(text, Xp.UI, 15f));
		}

		public JLabel languageButton() {
			return language;
		}

		// a short text next to the clock (e.g. the timer "1:14")
		public void setExtra(String text) {
			extra.setText(text);
		}

		// one tab per open window; the first one is the active window
		private String[] tabNames = new String[0];

		public void setTabs(String... names) {
			tabNames = names;
			tabs.removeAll();
			for (int i = 0; i < names.length; i++) {
				tabs.add(new Tab(names[i], i == 0));
			}
			tabs.revalidate();
			tabs.repaint();
		}

		// the tabs now (so they can come back after Help)
		public String[] tabs() {
			return tabNames;
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(), new float[] { 0f, 0.04f, 0.1f, 0.88f, 1f },
					new Color[] { new Color(0x3168D5), new Color(0x4993E6), new Color(0x2157D7), new Color(0x2663E0), new Color(0x1941A5) }));
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	// one tab in the taskbar
	static class Tab extends JComponent {
		String name;
		boolean on;

		Tab(String name, boolean on) {
			this.name = name;
			this.on = on;
			setPreferredSize(new Dimension(170, 30));
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Xp.smooth(g);
			g2.setColor(on ? new Color(0x1E52B7) : new Color(0x3D7EE9));
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
			g2.setColor(on ? new Color(0x123C8F) : new Color(0x1A4FB8));
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
			g2.setFont(Xp.fontFor(name, Xp.UI, 14f));
			g2.setColor(Color.WHITE);
			String text = name;
			FontMetrics fm = g2.getFontMetrics();
			while (fm.stringWidth(text) > getWidth() - 20 && text.length() > 3) {
				text = text.substring(0, text.length() - 2);
			}
			g2.drawString(text, 10, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
			g2.dispose();
		}
	}

	// =================================================================
	// THE BALLOON  (yellow, with a little tail pointing at the tray)
	// =================================================================
	static class Balloon extends JComponent {
		Icon picture;
		String title, text;

		Balloon(Icon picture, String title, String text) {
			this.picture = picture;
			this.title = title;
			this.text = text;
			// as wide as the longest line (at least 300)
			int iconW = picture == null ? 0 : picture.getIconWidth() + 12;
			int textW = Math.max(getFontMetrics(Xp.fontFor(title, Xp.PIXEL, 17f)).stringWidth(title),
					getFontMetrics(Xp.fontFor(text, Xp.MONO, 14f)).stringWidth(text));
			setPreferredSize(new Dimension(Math.max(300, 14 + iconW + textW + 22), 82));
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Xp.smooth(g);
			int w = getWidth(), h = getHeight() - 12;
			g2.setColor(new Color(0, 0, 0, 50));
			g2.fillRoundRect(3, 3, w - 4, h - 2, 18, 18);
			g2.setColor(Xp.BALLOON);
			g2.fillRoundRect(0, 0, w - 4, h - 2, 18, 18);
			Polygon tail = new Polygon(new int[] { w - 80, w - 60, w - 58 }, new int[] { h - 3, h - 3, h + 10 }, 3);
			g2.fillPolygon(tail);
			g2.setColor(new Color(0x333333));
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(0, 0, w - 4, h - 2, 18, 18);
			g2.drawLine(w - 80, h - 2, w - 58, h + 10);
			g2.drawLine(w - 60, h - 2, w - 58, h + 10);
			g2.setColor(Xp.BALLOON);
			g2.fillRect(w - 79, h - 4, 19, 4);
			int x = 14;
			if (picture != null) {
				picture.paintIcon(this, g2, x, (h - picture.getIconHeight()) / 2);
				x = x + picture.getIconWidth() + 12;
			}
			g2.setColor(Xp.INK);
			g2.setFont(Xp.fontFor(title, Xp.PIXEL, 17f));
			g2.drawString(title, x, h / 2 - 4);
			g2.setFont(Xp.fontFor(text, Xp.MONO, 14f));
			g2.drawString(text, x, h / 2 + 16);
			g2.dispose();
		}
	}
}
