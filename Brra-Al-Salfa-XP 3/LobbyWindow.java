/* =====================================================================
 *  LobbyWindow.java  -  "Game Lobby"
 * ---------------------------------------------------------------------
 *  LEFT  (task pane):
 *     "Pick a topic"  Food, Places, Jobs ... Random   (bonus: host picks)
 *     "Host tasks"    [ Start round ]  = the START GAME button of the brief
 *  RIGHT: "Players (4 of 8)" -> 8 places: the players, then empty places
 *  Only the host (the first player who joined) can pick and start.
 *  Everybody else sees the same window, with the buttons turned off.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LobbyWindow extends XpWindow {

	public interface Listener {
		void pickTopic(String topic);      // sends "PICK:<topic>"
		void startRound();                 // sends "START_GAME"
	}

	public static final int MAX_PLAYERS = 8;

	public LobbyWindow(ArrayList<String> topics, String picked, String myName, String host,
			Vector<String> players, HashMap<String, Avatar> avatars, String serverText, final Listener listener) {
		super(Lang.t("lobby"), Xp.pixelIcon(Xp.SMILE, 2));
		helpMenu();
		body().setBorder(new EmptyBorder(0, 0, 10, 0));
		boolean iAmHost = myName.equals(host);

		// =============================================================
		// LEFT: topics + host tasks
		// =============================================================
		XpParts.TaskPane pane = new XpParts.TaskPane(230);
		XpParts.Section topicSection = new XpParts.Section(Lang.t("pick_topic"));
		JPanel topicList = new JPanel();
		topicList.setLayout(new BoxLayout(topicList, BoxLayout.Y_AXIS));
		topicList.setBackground(Color.WHITE);
		// the topics from the server, then "Random"
		ArrayList<String> all = new ArrayList<String>(topics);
		all.add("RANDOM");
		for (int i = 0; i < all.size(); i++) {
			final String topic = all.get(i);
			String shown = topic.equals("RANDOM") ? Lang.t("random") : Lang.name(topic);
			XpParts.Row row = new XpParts.Row(shown, Xp.pixelIcon(Xp.topicIcon(topic), 2));
			row.setSelected(topic.equals(picked));
			if (iAmHost) {
				row.onClick(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						listener.pickTopic(topic);
					}
				});
			}
			topicList.add(row);
		}
		topicSection.content().add(topicList, BorderLayout.CENTER);
		pane.addSection(topicSection);

		XpParts.Section hostSection = new XpParts.Section(Lang.t("host_tasks"));
		JPanel hostBox = Xp.clear(new BorderLayout(0, 8));
		hostBox.setBorder(new EmptyBorder(10, 10, 10, 10));
		XpButton start = new XpButton(Lang.t("start_round"), XpButton.GREEN);
		start.setEnabled(iAmHost);
		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listener.startRound();
			}
		});
		hostBox.add(start, BorderLayout.NORTH);
		JLabel server = new JLabel(XpParts.wrap(Lang.t("server") + " " + serverText + " : 1202", 170));
		server.setFont(Xp.MONO.deriveFont(13f));
		server.setForeground(Xp.GREY);
		hostBox.add(server, BorderLayout.CENTER);
		hostSection.content().add(hostBox, BorderLayout.CENTER);
		pane.addSection(hostSection);
		body().add(pane, BorderLayout.LINE_START);

		// =============================================================
		// RIGHT: the players and the empty places
		// =============================================================
		JPanel right = XpParts.whiteArea(18, 20);
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		XpParts.Heading heading = new XpParts.Heading(Lang.f("players_count", players.size()));
		heading.setAlignmentX(Xp.start());
		right.add(heading);
		right.add(Box.createVerticalStrut(14));

		JPanel grid = Xp.clear(new GridLayout(2, 4, 12, 10));
		for (int i = 0; i < MAX_PLAYERS; i++) {
			if (i < players.size()) {
				String name = players.elementAt(i);
				XpParts.PlayerCard card = new XpParts.PlayerCard(avatars.get(name), name, 80);
				card.setStar(name.equals(host));                    // the host gets a star
				grid.add(card);
			} else {
				grid.add(new XpParts.EmptySlot(Lang.t("empty")));
			}
		}
		grid.setAlignmentX(Xp.start());
		grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
		right.add(grid);
		right.add(Box.createVerticalStrut(22));

		String hint = iAmHost ? Lang.t("host_hint") : Lang.f("guest_hint", host);
		if (iAmHost && players.size() < 2) {
			hint = hint + " " + Lang.t("need_players");
		}
		JLabel hintLabel = new JLabel(XpParts.wrap(hint, 430));
		hintLabel.setFont(Xp.fontFor(hint, Xp.MONO, 15f));
		hintLabel.setForeground(new Color(0x4A4A4A));
		hintLabel.setAlignmentX(Xp.start());
		right.add(hintLabel);
		body().add(right, BorderLayout.CENTER);
	}
}
