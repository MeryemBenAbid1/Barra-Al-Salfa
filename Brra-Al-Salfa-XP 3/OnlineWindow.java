/* =====================================================================
 *  OnlineWindow.java  -  "Who is online" (+ the small "Tip" window)
 * ---------------------------------------------------------------------
 *  Shown next to the sign-in pop-up, BEFORE the player joins.
 *  SpyClient asks the server every 3 seconds with a short connection:
 *        client -> "#WHO"         server -> "ONLINE:Nouri=1-1-1,Sara=3-3-5"
 *  and calls setPlayers(...) with the answer (see SpyClient [C3]).
 * ===================================================================== */

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class OnlineWindow extends XpWindow {

	private final JPanel list = new JPanel();

	public OnlineWindow() {
		super(Lang.t("who_online"), Xp.pixelIcon(Xp.CHAT, 2));
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(Color.WHITE);
		JPanel box = XpParts.listBox(new BorderLayout());
		box.add(list, BorderLayout.NORTH);
		body().add(box, BorderLayout.CENTER);
		setOffline();
	}

	// the server answered: one row per player in the lobby
	public void setPlayers(String[] names, Avatar[] avatars) {
		list.removeAll();
		for (int i = 0; i < names.length; i++) {
			JPanel row = Xp.clear(new BorderLayout(12, 0));
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Xp.LINE), new EmptyBorder(8, 10, 8, 10)));
			row.add(new Avatar.Portrait(avatars[i], 44, false), BorderLayout.LINE_START);
			JPanel text = Xp.clear(new GridLayout(2, 1));
			text.add(Xp.label(names[i], Xp.PIXEL, 18f, Xp.INK));
			JLabel status = Xp.label(Lang.t("in_lobby"), Xp.MONO, 13f, Xp.GREEN);
			status.setIcon(XpParts.dot(Xp.GREEN));
			status.setIconTextGap(7);
			text.add(status);
			row.add(text, BorderLayout.CENTER);
			row.setAlignmentX(Xp.start());
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
			list.add(row);
		}
		String footer;
		if (names.length == 0) {
			footer = Lang.t("online_none");
		} else {
			footer = Lang.f("online_count", names.length, 8 - names.length);
		}
		addFooter(footer);
	}

	// the server could not be reached
	public void setOffline() {
		list.removeAll();
		addFooter(Lang.t("online_off"));
	}

	private void addFooter(String text) {
		JLabel footer = new JLabel(XpParts.wrap(text, 330));
		footer.setFont(Xp.fontFor(text, Xp.MONO, 13f));
		footer.setForeground(Xp.GREY);
		footer.setAlignmentX(Xp.start());
		footer.setBorder(new EmptyBorder(10, 12, 10, 12));
		list.add(footer);
		list.revalidate();
		list.repaint();
	}

	// the small "Tip" window under "Who is online"
	public static XpWindow tip() {
		XpWindow tip = new XpWindow(Lang.t("tip"), Xp.pixelIcon(Xp.SMILE, 2));
		JLabel text = new JLabel(XpParts.wrap(Lang.t("tip_text"), 300), Xp.pixelIcon(Xp.SMILE, 3), JLabel.LEADING);
		text.setIconTextGap(14);
		text.setFont(Xp.fontFor(Lang.t("tip_text"), Xp.MONO, 15f));
		text.setForeground(Xp.INK);
		tip.body().add(text, BorderLayout.CENTER);
		return tip;
	}
}
