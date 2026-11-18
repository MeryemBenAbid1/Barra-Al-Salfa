/* =====================================================================
 *  HelpWindow.java  -  "How to play" (+ the small "Points" window)
 * ---------------------------------------------------------------------
 *  Opened from the desktop icon or the sign-in pop-up (bonus feature).
 *  LEFT : Contents (6 pages) and Players
 *  RIGHT: the page: numbered steps, each with a small pixel picture
 *  Back / Next go through the pages. The X button closes the help.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class HelpWindow extends XpWindow {

	// the 6 pages: title key, then { picture, line key, small line key } per step
	private static final String[] TITLES = { "the_goal", "h_topics", "h_asking", "h_voting", "h_last", "h_points" };
	private static final String[][] STEPS = {
		{ "step1", "step1b", "step2", "step2b", "step3", "step3b", "step4", "step4b" },
		{ "t1", "t1b", "t2", "t2b" },
		{ "a1", "a1b", "a2", "a2b" },
		{ "v1", "v1b", "v2", "v2b" },
		{ "l1", "l1b", "l2", "l2b" },
		{ "pp1", "pp1b", "pp2", "pp2b" } };
	private static final String[][][] PICTURES = {
		{ Xp.SMILE, Xp.FOOD, null, null },          // null = a character / the "?" face
		{ Xp.FOOD, Xp.RANDOM },
		{ Xp.CHAT, Xp.MIC },
		{ null, Xp.SMILE },
		{ null, Xp.PAPER },
		{ Xp.SMILE, Xp.EVENTS } };

	private int page = 0;
	private final ArrayList<XpParts.Row> rows = new ArrayList<XpParts.Row>();
	private final JPanel right = XpParts.whiteArea(18, 24);

	public HelpWindow(ActionListener close) {
		super(Lang.t("i_help"), Xp.pixelIcon(Xp.RANDOM, 2));
		helpMenu();
		body().setBorder(new EmptyBorder(0, 0, 10, 0));
		setCloseListener(close);

		// ---- LEFT: contents + players ----
		XpParts.TaskPane pane = new XpParts.TaskPane(220);
		XpParts.Section contents = new XpParts.Section(Lang.t("contents"));
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(Color.WHITE);
		for (int i = 0; i < TITLES.length; i++) {
			final int index = i;
			XpParts.Row row = new XpParts.Row(Lang.t(TITLES[i]), Xp.pixelIcon(Xp.PAPER, 2));
			row.onClick(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showPage(index);
				}
			});
			rows.add(row);
			list.add(row);
		}
		contents.content().add(list, BorderLayout.CENTER);
		pane.addSection(contents);

		XpParts.Section players = new XpParts.Section(Lang.t("m_players"));
		JLabel playersText = new JLabel(XpParts.wrap(Lang.t("h_players"), 160));
		playersText.setFont(Xp.fontFor(Lang.t("h_players"), Xp.MONO, 14f));
		playersText.setForeground(Xp.BLUE_TEXT);
		playersText.setBorder(new EmptyBorder(10, 10, 10, 10));
		players.content().add(playersText, BorderLayout.CENTER);
		pane.addSection(players);
		body().add(pane, BorderLayout.LINE_START);

		// ---- RIGHT: the page ----
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		body().add(right, BorderLayout.CENTER);
		showPage(0);
	}

	// builds the page again with its title and steps
	public void showPage(int index) {
		page = index;
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).setSelected(i == page);
		}
		right.removeAll();
		right.add(new XpParts.Heading(Lang.t(TITLES[page])));
		right.add(Box.createVerticalStrut(16));
		String[] steps = STEPS[page];
		for (int s = 0; s < steps.length / 2; s++) {
			right.add(step(s + 1, PICTURES[page][s], Lang.t(steps[s * 2]), Lang.t(steps[s * 2 + 1])));
			right.add(Box.createVerticalStrut(12));
		}

		// Back / Next
		XpButton back = new XpButton(Xp.backTriangle() + " " + Lang.t("back"), XpButton.NORMAL);
		XpButton next = new XpButton(Lang.t("next") + " " + Xp.nextTriangle(), XpButton.GO);
		back.setEnabled(page > 0);
		next.setEnabled(page < TITLES.length - 1);
		back.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPage(page - 1);
			}
		});
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPage(page + 1);
			}
		});
		JPanel buttons = Xp.clear(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		buttons.add(back);
		buttons.add(next);
		buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
		right.add(Box.createVerticalStrut(6));
		right.add(buttons);
		XpParts.leftAlign(right);
		right.revalidate();
		right.repaint();
	}

	// one step:  (1)  [picture]  Bold line
	//                            small grey line
	private JPanel step(final int number, String[] picture, String line, String small) {
		JPanel row = Xp.clear(new BorderLayout(14, 0));
		JComponent circle = new JComponent() {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Xp.smooth(g);
				g2.setColor(new Color(0x0A58E8));
				g2.fillOval(0, 14, 34, 34);
				g2.setColor(Color.WHITE);
				g2.setFont(Xp.PIXEL.deriveFont(17f));
				FontMetrics fm = g2.getFontMetrics();
				String n = "" + number;
				g2.drawString(n, 17 - fm.stringWidth(n) / 2, 31 + fm.getAscent() / 2 - 2);
				g2.dispose();
			}
		};
		circle.setPreferredSize(new Dimension(34, 62));
		Icon icon;
		if (picture != null) {
			icon = Xp.pixelIcon(picture, 4);
		} else if (page == 0) {
			icon = Avatar.preset(1).icon(4);           // a character
		} else {
			icon = new Icon() {                        // the "?" face
				public void paintIcon(Component c, Graphics g, int x, int y) { Avatar.drawMystery(g, x, y, 4); }
				public int getIconWidth()  { return 40; }
				public int getIconHeight() { return 40; }
			};
		}
		JLabel pic = new JLabel(icon, JLabel.CENTER);
		pic.setOpaque(true);
		pic.setBackground(Xp.FACE);
		pic.setBorder(BorderFactory.createLineBorder(new Color(0xA9BCE3), 2, true));
		pic.setPreferredSize(new Dimension(62, 62));
		JPanel left = Xp.clear(new FlowLayout(FlowLayout.LEADING, 14, 0));
		left.add(circle);
		left.add(pic);
		row.add(left, BorderLayout.LINE_START);

		JPanel text = Xp.clear(new BorderLayout(0, 2));
		JLabel big = new JLabel(XpParts.wrap(line, 310));
		big.setFont(Xp.fontFor(line, Xp.PIXEL, 17f));
		big.setForeground(Xp.INK);
		JLabel sm = new JLabel(XpParts.wrap(small, 310));
		sm.setFont(Xp.fontFor(small, Xp.MONO, 14f));
		sm.setForeground(new Color(0x555555));
		text.add(big, BorderLayout.NORTH);
		text.add(sm, BorderLayout.CENTER);
		row.add(text, BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
		return row;
	}

	// =================================================================
	// The small "Points" window next to the help
	// =================================================================
	public static XpWindow pointsWindow() {
		XpWindow w = new XpWindow(Lang.t("points"), Xp.pixelIcon(Xp.SMILE, 2));
		JPanel list = XpParts.listBox(new GridLayout(3, 1));
		String[][] lines = { { "p_catch", Lang.t("each") }, { "p_escape", "+2" }, { "p_guess", "+1" } };
		for (int i = 0; i < lines.length; i++) {
			JPanel row = new JPanel(new BorderLayout());
			row.setBackground(Color.WHITE);
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, i < 2 ? 1 : 0, 0, Xp.LINE), new EmptyBorder(8, 10, 8, 10)));
			row.add(Xp.label(Lang.t(lines[i][0]), Xp.MONO, 14f, Xp.INK), BorderLayout.LINE_START);
			row.add(Xp.label(lines[i][1], Xp.PIXEL, 14f, Xp.GREEN), BorderLayout.LINE_END);
			list.add(row);
		}
		w.body().add(list, BorderLayout.CENTER);
		return w;
	}
}
