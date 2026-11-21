/* =====================================================================
 *  XpParts.java  -  small pieces that many windows use
 * ---------------------------------------------------------------------
 *  [P1] TaskPane   the light blue column on the left of a window
 *  [P2] Section    a white box with a blue title  ("Pick a topic")
 *  [P3] Heading    a big blue title with a line under it
 *  [P4] Row        one line of a list (icon + text), can be selected
 *  [P5] PlayerCard a character + the name under it
 *  [P6] EmptySlot  a dashed square for a free place in the lobby
 *  [P7] Blocks     the green progress bar made of small blocks
 *  [P8] HintField  a text field that shows a grey hint when it is empty
 *  [P9] helpers    white area, small text, a list box
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class XpParts {

	public static final Color PANE_TOP = new Color(0xC4D3F6);      // task pane gradient
	public static final Color PANE_BOTTOM = new Color(0xA7BCEE);
	public static final Color SELECTED = new Color(0xFCDFC7);      // selected row (peach)

	// =================================================================
	// [P1] TASK PANE: a column with a light blue gradient
	// =================================================================
	public static class TaskPane extends JPanel {
		public TaskPane(int width) {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(new EmptyBorder(12, 12, 12, 12));
			setPreferredSize(new Dimension(width, 10));
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(new GradientPaint(0, 0, PANE_TOP, 0, getHeight(), PANE_BOTTOM));
			g2.fillRect(0, 0, getWidth(), getHeight());
		}

		// adds a section and a small gap under it
		public void addSection(Section s) {
			s.setAlignmentX(Xp.start());
			add(s);
			add(Box.createVerticalStrut(12));
		}
	}

	// =================================================================
	// [P2] SECTION: white box, title on a white -> blue gradient
	// =================================================================
	public static class Section extends JPanel {
		private final JLabel title = new JLabel();
		private final JPanel content = new JPanel(new BorderLayout());

		public Section(String text) {
			setLayout(new BorderLayout());
			setOpaque(false);
			JPanel header = new JPanel(new BorderLayout()) {
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = Xp.smooth(g);
					g2.setPaint(new GradientPaint(0, 0, Color.WHITE, getWidth(), 0, PANE_TOP));
					g2.fillRoundRect(0, 0, getWidth(), getHeight() + 8, 10, 10);
					g2.dispose();
				}
			};
			header.setOpaque(false);
			header.setBorder(new EmptyBorder(7, 10, 7, 10));
			setTitle(text);
			title.setForeground(Xp.BLUE_TEXT);
			header.add(title, BorderLayout.CENTER);
			content.setBackground(Color.WHITE);
			add(header, BorderLayout.NORTH);
			add(content, BorderLayout.CENTER);
		}

		public void setTitle(String text) {
			title.setText(text);
			title.setFont(Xp.fontFor(text, Xp.PIXEL, 16f));
		}

		public JPanel content() {
			return content;
		}

		// a section is never taller than it needs (inside a BoxLayout)
		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

	// =================================================================
	// [P3] HEADING: "Players (4 of 8)" in blue, with a line under it
	// =================================================================
	public static class Heading extends JLabel {
		public Heading(String text) {
			setForeground(Xp.BLUE_TEXT);
			setBorder(new EmptyBorder(0, 0, 8, 0));
			setText(text);
		}

		public void setText(String text) {
			super.setText(text);
			setFont(Xp.fontFor(text, Xp.PIXEL, 21f));
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(PANE_TOP);
			g.fillRect(0, getHeight() - 2, getWidth(), 2);
		}

		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

	// =================================================================
	// [P4] ROW: one clickable line (icon + text), e.g. a topic
	// =================================================================
	public static class Row extends JLabel {
		private boolean selected = false;

		public Row(String text, Icon icon) {
			super(text, icon, JLabel.LEADING);
			setIconTextGap(10);
			setBorder(new EmptyBorder(6, 10, 6, 10));
			setFont(Xp.fontFor(text, Xp.MONO, 15f));
			setForeground(Xp.BLUE_TEXT);
			setOpaque(true);
			setBackground(Color.WHITE);
		}

		public void setSelected(boolean on) {
			selected = on;
			setBackground(on ? SELECTED : Color.WHITE);
			setForeground(on ? Xp.INK : Xp.BLUE_TEXT);
			setFont(Xp.fontFor(getText(), on ? Xp.MONO_BOLD : Xp.MONO, 15f));
		}

		public boolean isSelected() {
			return selected;
		}

		public void setText(String text) {
			super.setText(text);
			setFont(Xp.fontFor(text, selected ? Xp.MONO_BOLD : Xp.MONO, 15f));
		}

		// when clicked, run this
		public void onClick(final ActionListener action) {
			Xp.onClick(this, action);
			// a light blue line under the mouse, so you see it can be clicked
			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					if (!selected) {
						setBackground(new Color(0xE8F0FC));
					}
				}
				public void mouseExited(MouseEvent e) {
					setBackground(selected ? SELECTED : Color.WHITE);
				}
			});
		}

		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

	// =================================================================
	// [P5] PLAYER CARD: character + name (+ a small line under it)
	// =================================================================
	public static class PlayerCard extends JPanel {
		private final Avatar.Portrait portrait;
		private final JLabel name = new JLabel("", JLabel.CENTER);
		private final JLabel sub = new JLabel(" ", JLabel.CENTER);

		public PlayerCard(Avatar avatar, String playerName, int size) {
			setOpaque(false);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			portrait = new Avatar.Portrait(avatar, size, false);
			portrait.setAlignmentX(Component.CENTER_ALIGNMENT);
			name.setAlignmentX(Component.CENTER_ALIGNMENT);
			sub.setAlignmentX(Component.CENTER_ALIGNMENT);
			name.setForeground(Xp.INK);
			name.setBorder(new EmptyBorder(1, 5, 1, 5));
			sub.setFont(Xp.MONO.deriveFont(13f));
			sub.setForeground(Xp.ORANGE_DARK);
			add(portrait);
			add(Box.createVerticalStrut(6));
			add(name);
			add(sub);
			setName(playerName);
		}

		public void setName(String text) {
			name.setText(text);
			name.setFont(Xp.fontFor(text, Xp.PIXEL, 17f));
		}

		public void setSub(String text) {
			sub.setText(text.length() == 0 ? " " : text);
			sub.setFont(Xp.fontFor(text, Xp.MONO, 13f));
		}

		public void setAvatar(Avatar a) {
			portrait.setAvatar(a);
		}

		// a small star after the name (the host)
		public void setStar(boolean on) {
			name.setHorizontalTextPosition(JLabel.LEADING);
			name.setIconTextGap(6);
			name.setIcon(on ? new Icon() {
				public void paintIcon(Component c, Graphics g, int x, int y) {
					g.setColor(Xp.INK);
					g.setFont(Xp.WORD.deriveFont(12f));
					g.drawString("\u2605", x, y + 12);       // the star from the pixel font
				}
				public int getIconWidth()  { return 13; }
				public int getIconHeight() { return 13; }
			} : null);
		}

		// orange frame + orange name badge (e.g. "asking now")
		public void setHighlight(boolean on) {
			portrait.setRing(on);
			name.setOpaque(on);
			name.setBackground(Xp.ORANGE);
			name.setForeground(on ? Color.WHITE : Xp.INK);
			repaint();
		}
	}

	// =================================================================
	// [P6] EMPTY SLOT: a dashed square with "empty" under it
	// =================================================================
	public static class EmptySlot extends JComponent {
		private String text;

		public EmptySlot(String text) {
			this.text = text;
			setPreferredSize(new Dimension(100, 105));
		}

		public void setText(String t) {
			text = t;
			repaint();
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Xp.smooth(g);
			int s = 74;
			int x = (getWidth() - s) / 2;
			g2.setColor(new Color(0xA9BCE3));
			// a dashed line: 6 pixels drawn, 4 pixels empty
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[] { 6f, 4f }, 0f));
			g2.drawRoundRect(x, 4, s, s, 8, 8);
			g2.setFont(Xp.fontFor(text, Xp.MONO, 13f));
			FontMetrics fm = g2.getFontMetrics();
			g2.setColor(new Color(0xA0A0A0));
			g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, s + 24);
			g2.dispose();
		}
	}

	// =================================================================
	// [P7] BLOCKS: the XP progress bar (small green blocks)
	// =================================================================
	public static class Blocks extends JComponent {
		private int value = 0, max = 1;

		public Blocks(int height) {
			setPreferredSize(new Dimension(200, height));
		}

		public void setValue(int v, int m) {
			value = Math.max(0, v);
			max = Math.max(1, m);
			repaint();
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			int w = getWidth(), h = getHeight();
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, w, h);
			g2.setColor(Xp.FIELD_BORDER);
			g2.drawRect(0, 0, w - 1, h - 1);
			g2.drawRect(1, 1, w - 3, h - 3);
			int block = h - 8;                         // each block is (almost) square
			int step = block - 2 + 3;                  // block width + gap
			int count = (w - 8) / step;                // how many fit in the bar
			int filled = (int) Math.round(count * (double) value / max);
			for (int i = 0; i < filled; i++) {
				int x = 4 + i * step;
				g2.setPaint(new GradientPaint(0, 4, new Color(0x8FE07A), 0, h - 4, new Color(0x2E9A2A)));
				g2.fillRect(x, 4, block - 2, h - 8);
			}
		}
	}

	// =================================================================
	// [P8] HINT FIELD: grey text inside the field while it is empty
	// =================================================================
	public static class HintField extends JTextField {
		private String hint;

		public HintField(String hint) {
			this.hint = hint;
			JTextField style = Xp.field("");          // copy the kit look
			setFont(style.getFont());
			setForeground(style.getForeground());
			setBackground(Color.WHITE);
			setCaretColor(Xp.INK);
			setBorder(style.getBorder());
		}

		public void setHint(String text) {
			hint = text;
			repaint();
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (getText().length() == 0) {
				Graphics2D g2 = Xp.smooth(g);
				g2.setFont(Xp.fontFor(hint, Xp.MONO, getFont().getSize2D() - 1));
				g2.setColor(new Color(0x9A9A9A));
				Insets in = getInsets();
				FontMetrics fm = g2.getFontMetrics();
				g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
				g2.dispose();
			}
		}
	}

	// =================================================================
	// [P9] HELPERS
	// =================================================================

	// a white panel (the right side of the lobby, the help pages...)
	public static JPanel whiteArea(int top, int side) {
		JPanel p = new JPanel();
		p.setBackground(Color.WHITE);
		p.setBorder(new EmptyBorder(top, side, top, side));
		return p;
	}

	// a white box with the blue XP field border (lists, tables)
	public static JPanel listBox(LayoutManager layout) {
		JPanel p = new JPanel(layout);
		p.setBackground(Color.WHITE);
		p.setBorder(Xp.whiteBox());
		return p;
	}

	// small grey text (hints under things)
	public static JLabel hint(String text) {
		JLabel l = new JLabel(Xp.hasArabic(text) ? Xp.rtlLine(text) : text);
		l.setFont(Xp.fontFor(text, Xp.MONO, 13f));
		l.setForeground(Xp.GREY);
		return l;
	}

	// text that wraps on many lines (HTML in a JLabel wraps by itself)
	// (Swing's HTML makes a "px" 4/3 bigger, so we ask for 3/4 of the width)
	public static String wrap(String text, int width) {
		String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		if (Lang.arabic()) {
			// \u200F (a right-to-left mark) makes the whole line read right to left,
			// so English names inside Arabic text land in the right place
			return "<html><div align='right' style='width:" + (width * 3 / 4) + "px'>\u200F" + safe + "</div></html>";
		}
		return "<html><div style='width:" + (width * 3 / 4) + "px'>" + safe + "</div></html>";
	}

	// in a BoxLayout column, every child starts at the left edge
	// (the right edge in Arabic)
	public static void leftAlign(JPanel column) {
		Component[] all = column.getComponents();
		for (int i = 0; i < all.length; i++) {
			if (all[i] instanceof JComponent) {
				((JComponent) all[i]).setAlignmentX(Xp.start());
			}
		}
	}

	// a small round dot (e.g. the green "online" dot)
	public static Icon dot(final Color color) {
		return new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2 = Xp.smooth(g);
				g2.setColor(color);
				g2.fillOval(x, y, 8, 8);
				g2.dispose();
			}
			public int getIconWidth()  { return 8; }
			public int getIconHeight() { return 8; }
		};
	}

	// formats seconds as m:ss  (74 -> "1:14")
	public static String clock(int seconds) {
		int s = Math.max(0, seconds);
		return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
	}
}
