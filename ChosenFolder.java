/* =====================================================================
 *  ChosenFolder.java  -  the file the host chose, lying in the lobby
 * ---------------------------------------------------------------------
 *  One big manila folder. Its tag shows the chosen topic (or "?" for
 *  RANDOM). When the host changes the file, the folder bounces.
 *  The host can click it to open the drawer pop-up again.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.JComponent;

public class ChosenFolder extends JComponent {

	private String topic = "RANDOM";
	private boolean clickable = false;
	private boolean hover = false;
	private float bounce = 1f;           // little scale bump when it changes
	private Runnable onClick;

	public ChosenFolder() {
		setOpaque(false);
		setPreferredSize(new Dimension(340, 220));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
			public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
			public void mouseClicked(MouseEvent e) {
				if (clickable && onClick != null) {
					Theme.play("click");
					onClick.run();
				}
			}
		});
	}

	public void setOnClick(Runnable r) { onClick = r; }

	public void setClickable(boolean on) {
		clickable = on;
		setCursor(Cursor.getPredefinedCursor(on ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
		repaint();
	}

	public void setTopic(String name) {
		if (name.equals(topic)) {
			return;
		}
		topic = name;
		Anim.run(360, new Anim.Step() {
			public void update(float t) {
				bounce = 0.85f + 0.15f * Anim.easeBack(t);
				repaint();
			}
		}, null);
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Theme.smooth(g);
		int w = Math.min(getWidth() - 20, 340);
		int h = Math.min(getHeight() - 20, 210);
		int lift = clickable && hover ? 6 : 0;
		g2.translate(getWidth() / 2.0, getHeight() / 2.0 - lift);
		g2.scale(bounce, bounce);
		int x = -w / 2;
		int y = -h / 2;
		int tabH = 40;
		int tabW = (int) (w * 0.62);
		int tabX = Lang.rtl() ? x + w - tabW - 16 : x + 16;

		// shadow
		g2.setColor(new Color(0, 0, 0, 45 + lift * 4));
		g2.fillRoundRect(x + 5, y + tabH + 8 + lift, w, h - tabH, 12, 12);
		// back + tag
		g2.setColor(new Color(201, 178, 133));
		g2.fillRoundRect(x, y + tabH - 6, w, h - tabH + 6, 12, 12);
		g2.fillRoundRect(tabX, y, tabW, tabH + 8, 10, 10);
		// the topic on a red label on the tag
		g2.setColor(Theme.RED);
		g2.fillRect(tabX + 10, y + 7, tabW - 20, tabH - 12);
		String label = topic.equals("RANDOM") ? "?  " + Lang.category(topic) : Lang.category(topic).toUpperCase();
		g2.setColor(Theme.PAPER);
		g2.setFont(Theme.fit(g2, Theme.stencil(22f), label, tabW - 36, 11f));
		Theme.centerText(g2, label, tabX + 10, y + 7, tabW - 20, tabH - 12);
		// a sheet of paper sticking out + the front
		g2.setColor(Theme.PAPER);
		g2.fillRect(x + 18, y + tabH - 2, w - 36, 18);
		g2.setColor(Theme.FOLDER);
		g2.fillRoundRect(x, y + tabH + 8, w, h - tabH - 8, 12, 12);
		g2.setColor(new Color(176, 154, 110));
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawRoundRect(x, y + tabH + 8, w, h - tabH - 8, 12, 12);

		// the host sees a small hint that they can change it
		if (clickable) {
			String hint = Lang.t("change_file");
			g2.setColor(hover ? Theme.INK : Theme.MUTED);
			g2.setFont(Theme.safe(Theme.stencil(15f), hint));
			Theme.centerText(g2, hint, x, y + h - 44, w, 30);
		}
		g2.dispose();
	}
}
