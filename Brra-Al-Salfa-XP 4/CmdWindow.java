/* =====================================================================
 *  CmdWindow.java  -  the server log, drawn like the Windows XP CMD
 *                     "C:\WINDOWS\system32\cmd.exe - java SpyServer"
 * ---------------------------------------------------------------------
 *  - black background, the VT323 terminal font
 *  - the big orange / yellow block letters "BRRA AL-SALFA" at the top
 *  - every log line starts with the time:  [12:50:02] + JOIN  Meryem ...
 *    the important word of a line has a color (JOIN green, LEAVE red...)
 *  - at the bottom, during a round: "Time left 1:04  ██████░░░░"
 *  Lines can be added from any thread: they are added on the Swing
 *  thread with SwingUtilities.invokeLater.
 * ===================================================================== */

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

public class CmdWindow extends XpWindow {

	// the CMD colors
	public static final Color TEXT = new Color(0xC8C8C8);
	public static final Color DIM = new Color(0x8A8A8A);
	public static final Color WHITE = new Color(0xF2F2F2);
	public static final Color GREEN = new Color(0x3BD13B);
	public static final Color RED = new Color(0xF0563A);
	public static final Color YELLOW = new Color(0xF6E05E);
	public static final Color ORANGE = new Color(0xF59A3C);
	public static final Color CYAN = new Color(0x4FD1C5);

	private final JTextPane screen = new JTextPane();
	private final TimerLine timerLine = new TimerLine();

	public CmdWindow() {
		super("C:\\WINDOWS\\system32\\cmd.exe - java SpyServer", cmdIcon());
		body().setBackground(Xp.CMD_BLACK);
		body().setBorder(new EmptyBorder(0, 0, 0, 0));
		screen.setEditable(false);
		screen.setBackground(Xp.CMD_BLACK);
		screen.setBorder(new EmptyBorder(10, 14, 10, 14));
		JScrollPane scroll = new JScrollPane(screen);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		body().add(scroll, BorderLayout.CENTER);
		body().add(timerLine, BorderLayout.SOUTH);

		// the start of the CMD, like on a real Windows XP
		line(DIM, "Microsoft Windows XP [Version 5.1.2600]");
		line(DIM, "(C) Copyright 1985-2001 Microsoft Corp.");
		line(TEXT, "");
		part(TEXT, "C:\\Brra-Al-Salfa> ");
		line(WHITE, "java SpyServer");
		line(TEXT, "");
		banner();
		line(TEXT, "");
		line(CYAN, "  CpE 458 \u00B7 Multiplayer Server \u00B7 port 1202");
		line(TEXT, "");
	}

	// the small black "C:\" picture in the title bar
	private static Icon cmdIcon() {
		return new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.setColor(Color.BLACK);
				g.fillRect(x, y, 26, 18);
				g.setColor(new Color(0xC0C0C0));
				g.drawRect(x, y, 25, 17);
				g.setFont(Xp.CMD.deriveFont(16f));
				g.drawString("C:\\", x + 3, y + 14);
			}
			public int getIconWidth()  { return 26; }
			public int getIconHeight() { return 18; }
		};
	}

	// =================================================================
	// ADDING TEXT
	// =================================================================

	// a log line:  [12:50:02] <tag in color> <rest>
	public void log(final Color tagColor, final String tag, final String rest) {
		final String time = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				part(DIM, time);
				if (tag.length() > 0) {
					part(tagColor, tag + "  ");
				}
				line(TEXT, rest);
			}
		});
	}

	// a line without the time (tables, empty lines)
	public void plain(final Color color, final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				line(color, text);
			}
		});
	}

	private void line(Color color, String text) {
		part(color, text + "\n");
	}

	private void part(Color color, String text) {
		StyledDocument doc = screen.getStyledDocument();
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, color);
		StyleConstants.setFontFamily(style, Xp.hasArabic(text) ? Xp.AR_TEXT.getFamily() : Xp.CMD.getFamily());
		StyleConstants.setFontSize(style, Xp.hasArabic(text) ? 17 : 22);
		try {
			doc.insertString(doc.getLength(), text, style);
		} catch (BadLocationException ex) {
			// cannot happen: we always add at the end
		}
		screen.setCaretPosition(doc.getLength());            // scroll down
	}

	// all the text (for "Save log")
	public String text() {
		return screen.getText();
	}

	// =================================================================
	// THE BANNER  "BRRA AL-SALFA" in 5 x 5 block letters
	//  each letter is 5 rows of 0 and 1: 1 = a colored square
	// =================================================================
	private void banner() {
		final String[][] letters = {
			{ "B", "1110", "1001", "1110", "1001", "1110" },
			{ "R", "1110", "1001", "1110", "1010", "1001" },
			{ "A", "0110", "1001", "1111", "1001", "1001" },
			{ "L", "1000", "1000", "1000", "1000", "1111" },
			{ "S", "0111", "1000", "0110", "0001", "1110" },
			{ "F", "1111", "1000", "1110", "1000", "1000" },
			{ "-", "000", "000", "111", "000", "000" },
			{ " ", "00", "00", "00", "00", "00" } };
		// build the 5 rows of the whole text
		final String[] rows = { "", "", "", "", "" };
		String text = "BRRA AL-SALFA";
		for (int i = 0; i < text.length(); i++) {
			for (int l = 0; l < letters.length; l++) {
				if (letters[l][0].charAt(0) == text.charAt(i)) {
					for (int r = 0; r < 5; r++) {
						rows[r] = rows[r] + letters[l][r + 1] + "0";   // a gap after each letter
					}
				}
			}
		}
		final int px = 9;
		final Color[] rowColors = { Xp.ORANGE, Xp.ORANGE, ORANGE, YELLOW, YELLOW };
		JComponent picture = new JComponent() {
			protected void paintComponent(Graphics g) {
				for (int r = 0; r < 5; r++) {
					g.setColor(rowColors[r]);
					for (int c = 0; c < rows[r].length(); c++) {
						if (rows[r].charAt(c) == '1') {
							g.fillRect(20 + c * px, 4 + r * px, px - 1, px - 1);
						}
					}
				}
			}
		};
		picture.setPreferredSize(new Dimension(20 + rows[0].length() * px, 5 * px + 8));
		picture.setOpaque(false);
		// a component can be put inside the text, like a picture in a document
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setComponent(style, picture);
		StyledDocument doc = screen.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), " ", style);
			doc.insertString(doc.getLength(), "\n", new SimpleAttributeSet());
		} catch (BadLocationException ex) {
			// cannot happen
		}
	}

	// =================================================================
	// THE TIMER LINE at the bottom:  Time left 1:04  ██████░░░░
	// =================================================================
	public void setTimer(final String label, final int left, final int total) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				timerLine.set(label, left, total);
			}
		});
	}

	static class TimerLine extends JComponent {
		private String label = "";
		private int left = 0, total = 0;

		TimerLine() {
			setPreferredSize(new Dimension(10, 44));
		}

		void set(String l, int lft, int tot) {
			label = l;
			left = lft;
			total = tot;
			repaint();
		}

		protected void paintComponent(Graphics g) {
			g.setColor(Xp.CMD_BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (total <= 0) {
				return;                          // no round: an empty line
			}
			g.setFont(Xp.CMD.deriveFont(22f));
			g.setColor(YELLOW);
			String text = label + " " + XpParts.clock(left);
			g.drawString(text, 16, 28);
			int x = 30 + g.getFontMetrics().stringWidth(text);
			int blocks = 24;
			int filled = (int) Math.round(blocks * (double) left / total);
			for (int i = 0; i < blocks; i++) {
				g.setColor(i < filled ? GREEN : new Color(0x2E2E2E));
				g.fillRect(x + i * 15, 12, 14, 22);
			}
		}
	}
}
