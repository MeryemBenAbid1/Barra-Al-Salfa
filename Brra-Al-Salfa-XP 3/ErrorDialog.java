/* =====================================================================
 *  ErrorDialog.java  -  the XP error box ("Connection failed")
 * ---------------------------------------------------------------------
 *  Works like JOptionPane.showMessageDialog: show(...) waits until the
 *  player presses OK (the JDialog is modal).
 *     (X)  Could not connect to 192.168.1.99 on port 1202.
 *          Make sure the server is running ...
 *                        [ OK ]
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ErrorDialog {

	private static boolean answer;        // what ask(...) returns

	// line1 may have <b>bold</b> parts; line2 can be ""
	public static void show(Desktop desktop, String title, String line1, String line2) {
		build(desktop, title, line1, line2, null, null);
	}

	// the same box with a blue (i) and two buttons; true = the player pressed "yes"
	// (like JOptionPane.showConfirmDialog)
	public static boolean ask(Desktop desktop, String title, String line1, String line2, String yes, String no) {
		answer = false;
		build(desktop, title, line1, line2, yes, no);
		return answer;
	}

	private static void build(Desktop desktop, String title, String line1, String line2, String yes, String no) {
		final boolean question = yes != null;
		XpWindow window = new XpWindow(title, null);
		JPanel body = window.body();
		body.setBorder(new EmptyBorder(18, 20, 14, 20));

		// the red circle with a white X (painted)
		JComponent redX = new JComponent() {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Xp.smooth(g);
				g2.setColor(question ? Xp.BLUE : Xp.RED);
				g2.fillOval(1, 1, 36, 36);
				g2.setColor(question ? Xp.BLUE_TEXT : new Color(0xA52A12));
				g2.setStroke(new BasicStroke(2f));
				g2.drawOval(1, 1, 36, 36);
				g2.setColor(Color.WHITE);
				if (question) {                              // the (i)
					g2.fillOval(16, 8, 6, 6);
					g2.fillRoundRect(16, 16, 6, 14, 3, 3);
				} else {                                     // the (X)
					g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g2.drawLine(13, 13, 25, 25);
					g2.drawLine(25, 13, 13, 25);
				}
				g2.dispose();
			}
		};
		redX.setPreferredSize(new Dimension(40, 40));
		JPanel iconBox = Xp.clear(new BorderLayout());
		iconBox.add(redX, BorderLayout.NORTH);
		iconBox.setBorder(Lang.arabic() ? new EmptyBorder(4, 18, 0, 0) : new EmptyBorder(4, 0, 0, 18));

		// the text (HTML so that it wraps and can have bold parts)
		String html = "<html><div" + (Lang.arabic() ? " align='right'" : "") + " style='width:" + ((question ? 400 : 340) * 3 / 4) + "px'>" + (Lang.arabic() ? "\u200F" : "") + line1;    // 3/4: see XpParts.wrap
		if (line2.length() > 0) {
			html = html + "<br><br>" + line2;
		}
		html = html + "</div></html>";
		JLabel text = new JLabel(html);
		text.setFont(Xp.fontFor(line1 + line2, Xp.MONO, 16f));
		text.setForeground(Xp.INK);

		XpButton ok = new XpButton(question ? yes : Lang.t("ok"), question ? XpButton.GO : XpButton.NORMAL);
		ok.setPreferredSize(new Dimension(Math.max(110, ok.getPreferredSize().width), 40));
		JPanel okRow = Xp.clear(new FlowLayout(FlowLayout.CENTER, 12, 0));
		okRow.setBorder(new EmptyBorder(16, 0, 0, 0));
		XpButton cancel = null;
		if (question) {
			cancel = new XpButton(no, XpButton.NORMAL);
			cancel.setPreferredSize(new Dimension(Math.max(110, cancel.getPreferredSize().width), 40));
			okRow.add(cancel);
		}
		okRow.add(ok);

		body.add(iconBox, BorderLayout.LINE_START);
		body.add(text, BorderLayout.CENTER);
		body.add(okRow, BorderLayout.SOUTH);

		final XpDialog dialog = new XpDialog(desktop, window, true);
		ActionListener close = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		};
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				answer = true;
				dialog.dispose();
			}
		});
		if (cancel != null) {
			cancel.addActionListener(close);
		}
		window.setCloseListener(close);
		Xp.orient(window);
		dialog.getRootPane().setDefaultButton(ok);       // Enter = OK
		dialog.showAt(question ? 370 : 400, question ? 250 : 300, question ? 540 : 480, 0);   // waits here until OK
	}
}
