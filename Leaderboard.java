/* =====================================================================
 *  Leaderboard.java  -  "MOST WANTED" list
 * ---------------------------------------------------------------------
 *  setScores() takes the SCORES message from the server, for example:
 *      "meryem   260 pts,nouri   210 pts"
 * ===================================================================== */

import java.awt.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;

public class Leaderboard extends JComponent {

	private static final Pattern ROW = Pattern.compile("^(.*?)\\s+(-?\\d+)\\s*pts?(.*)$");
	private static final Pattern STREAK = Pattern.compile("(\\d+)\\s*win streak");

	private final ArrayList<String[]> rows = new ArrayList<String[]>();   // {name, points, streak}
	private final int maxRows;
	private String myName = "";

	public Leaderboard(int maxRows) {
		this.maxRows = maxRows;
		setOpaque(false);
	}

	public void setMyName(String name) {
		myName = name;
	}

	public void setScores(String data) {
		rows.clear();
		String[] parts = data.split(",");
		for (int i = 0; i < parts.length && rows.size() < maxRows; i++) {
			String part = parts[i].trim();
			if (part.length() == 0) {
				continue;
			}
			Matcher m = ROW.matcher(part);
			if (m.matches()) {
				// "Meryem   260 pts   (2 win streak!)" -> streak "x2"
				String streak = "";
				Matcher s = STREAK.matcher(m.group(3));
				if (s.find()) {
					streak = "x" + s.group(1);
				}
				rows.add(new String[] { m.group(1).trim(), m.group(2), streak });
			} else {
				rows.add(new String[] { part, "", "" });
			}
		}
		repaint();
	}

	public Dimension getPreferredSize() {
		return new Dimension(260, 40 + Math.max(3, Math.min(rows.size(), maxRows)) * 28);
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Theme.smooth(g);
		int w = getWidth();
		boolean rtl = Lang.rtl();
		String title = Lang.t("most_wanted");
		g2.setColor(Theme.INK);
		g2.setFont(Theme.safe(Theme.stencil(18f), title));
		g2.drawString(title, Theme.lineX(g2, title, 0, w), 20);
		g2.fillRect(0, 28, w, 2);

		int y = 38;
		if (rows.isEmpty()) {
			String none = Lang.t("no_records");
			g2.setColor(Theme.MUTED);
			g2.setFont(Theme.safe(Theme.type(15f), none));
			g2.drawString(none, Theme.lineX(g2, none, 0, w), y + 18);
		}
		for (int i = 0; i < rows.size(); i++) {
			String name = rows.get(i)[0];
			boolean me = name.equalsIgnoreCase(myName);
			if (me) {
				g2.setColor(Theme.PAPER_DARK);
				g2.fillRect(0, y, w, 26);
			}
			g2.setFont(Theme.monoBold(16f));
			FontMetrics fm = g2.getFontMetrics();
			int base = y + (26 - fm.getHeight()) / 2 + fm.getAscent();

			// position number: left in English, right in Arabic
			String pos = (i + 1) + ".";
			g2.setColor(i == 0 ? Theme.RED : Theme.INK);
			g2.drawString(pos, rtl ? w - 6 - fm.stringWidth(pos) : 6, base);

			// points: right in English, left in Arabic
			String pts = rows.get(i)[1];
			g2.setColor(Theme.INK);
			int ptsW = fm.stringWidth(pts);
			g2.drawString(pts, rtl ? 6 : w - 6 - ptsW, base);

			// win streak tag next to the points
			String streak = rows.get(i)[2];
			if (streak.length() > 0) {
				g2.setFont(Theme.stencil(12f));
				FontMetrics sm = g2.getFontMetrics();
				int sw = sm.stringWidth(streak) + 10;
				int sx = rtl ? 14 + ptsW : w - 14 - ptsW - sw;
				g2.setColor(Theme.RED);
				g2.setStroke(new BasicStroke(2f));
				g2.drawRect(sx, y + 5, sw, 16);
				g2.drawString(streak, sx + 5, y + 18);
			}

			// name
			g2.setColor(Theme.INK);
			g2.setFont(Theme.fit(g2, Theme.type(16f), name, w - 140, 11f));
			int nameW = g2.getFontMetrics().stringWidth(name);
			g2.drawString(name, rtl ? w - 38 - nameW : 38, base);
			y += 28;
		}
		g2.dispose();
	}
}
