/* =====================================================================
 *  LobbyScreen.java  -  screen 1: the host picks a file, agents wait
 * ---------------------------------------------------------------------
 *   CENTER  the chosen file (ChosenFolder)
 *           the agents (one row of ID cards) + OPEN CASE
 *   END     chat
 *   ON TOP  the drawer pop-up (FilePopup): it opens by itself for the
 *           host when the room is created; the host can open it again
 *           by clicking the chosen folder.
 *
 *  WHO IS THE HOST?  The server decides and sends "HOST:<name>":
 *   - the TV host screen, if one is connected  -> "HOST:#SCREEN"
 *   - otherwise the first player who joined    -> "HOST:Nouri"
 *  Only the host gets the drawer and sees OPEN CASE. Everybody else
 *  sees "HOST: <name>" and the chosen file in the middle.
 * ===================================================================== */

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.*;

public class LobbyScreen extends JPanel {

	public interface PickListener {
		void pick(String category);      // a category name, or "RANDOM"
	}

	public static final int SLOTS = 8;
	public static final int MIN_PLAYERS = 2;   // same as MIN_PLAYERS in SpyServer
	public static final String SCREEN_HOST = "#SCREEN";   // the TV is the host

	private final IdCard[] cards = new IdCard[SLOTS];
	private final JLabel pickTitle = new JLabel();
	private final JLabel countLabel = new JLabel();
	private final JLabel footerText = new JLabel();
	private final ChosenFolder chosenFolder = new ChosenFolder();
	private final FilePopup popup = new FilePopup();
	private final JPanel content = new JPanel(new BorderLayout(26, 0));
	private final DossierButton startButton;
	private final JPanel chatSlot = new JPanel(new BorderLayout());

	private final HashSet<String> shown = new HashSet<String>();   // names already on a card
	private ArrayList<String> names = new ArrayList<String>();
	private String myName = "";
	private String hostName = "";
	private boolean lobbyOpen = true;       // false while a round is running
	private boolean wasHost = false;        // to open the drawer only when we BECOME host

	public LobbyScreen() {
		setOpaque(false);
		setLayout(new BorderLayout());
		content.setOpaque(false);
		add(FilePopup.over(content, popup), BorderLayout.CENTER);

		// ================= CENTER: files, agents, OPEN CASE =================
		JPanel center = new JPanel(new BorderLayout(0, 14));
		center.setOpaque(false);

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(new DashedBorder(DashedBorder.BOTTOM, false, Theme.INK, 6));
		pickTitle.setForeground(Theme.INK);
		countLabel.setForeground(Theme.INK);
		header.add(pickTitle, BorderLayout.LINE_START);
		header.add(countLabel, BorderLayout.LINE_END);
		center.add(header, BorderLayout.NORTH);

		center.add(chosenFolder, BorderLayout.CENTER);
		chosenFolder.setOnClick(new Runnable() {
			public void run() {
				popup.open();                      // the host wants another file
			}
		});

		JPanel bottom = new JPanel(new BorderLayout(0, 12));
		bottom.setOpaque(false);
		JPanel row = new JPanel(new GridLayout(1, SLOTS, 10, 0));
		row.setOpaque(false);
		row.setPreferredSize(new Dimension(600, 128));
		for (int i = 0; i < SLOTS; i++) {
			cards[i] = new IdCard();
			row.add(cards[i]);
		}
		bottom.add(row, BorderLayout.CENTER);

		JPanel footer = new JPanel(new BorderLayout(18, 0));
		footer.setOpaque(false);
		footer.setBorder(new DashedBorder(DashedBorder.TOP, true, Theme.FAINT, 10));
		footerText.setForeground(Theme.INK);
		startButton = new DossierButton("", DossierButton.STAMP, 24f, -2);
		startButton.setPreferredSize(new Dimension(250, 74));
		footer.add(footerText, BorderLayout.CENTER);
		footer.add(startButton, BorderLayout.LINE_END);
		bottom.add(footer, BorderLayout.SOUTH);
		center.add(bottom, BorderLayout.SOUTH);
		content.add(center, BorderLayout.CENTER);

		// ================= END: chat =================
		chatSlot.setOpaque(false);
		chatSlot.setPreferredSize(new Dimension(300, 100));
		content.add(chatSlot, BorderLayout.LINE_END);

		setPlayers(new ArrayList<String>());
		applyLanguage();
	}

	public DossierButton getStartButton() { return startButton; }

	public void attachChat(ChatPanel chat) {
		chatSlot.add(chat, BorderLayout.CENTER);
		chatSlot.revalidate();
		chatSlot.repaint();
	}

	public void setMyName(String name) {
		myName = name;
	}

	public void setPickListener(final PickListener l) {
		popup.getCabinet().setPickListener(new FileCabinet.PickListener() {
			public void pick(String category) {
				l.pick(category);
			}
		});
	}

	// CATEGORIES message: the category files on the server
	public void setCategories(ArrayList<String> categoryNames) {
		popup.getCabinet().setCategories(categoryNames);
	}

	// PICKED message: the chosen file
	public void setPicked(String name) {
		popup.setPicked(name);             // stamp + close (if it is open)
		chosenFolder.setTopic(name);
	}

	// HOST message: who may pick the file and open the case
	public void setHost(String name) {
		hostName = name;
		refreshCards();
		updateHostParts();
		// the room was just created / we just became host: show the drawer
		if (iAmHost() && !wasHost && lobbyOpen) {
			Anim.later(500, new Runnable() {
				public void run() {
					if (iAmHost() && lobbyOpen) {
						popup.open();
					}
				}
			});
		}
		wasHost = iAmHost();
	}

	public boolean iAmHost() {
		return hostName.equals(myName) && myName.length() > 0;
	}

	// the host's name as people should read it ("#SCREEN" = the TV)
	private String hostLabel() {
		if (hostName.equals(SCREEN_HOST)) {
			return Lang.t("the_host");
		}
		return hostName;
	}

	public void applyLanguage() {
		startButton.setText(Lang.t("open_case"));
		updateHostParts();
		refreshCards();
		popup.applyLanguage();
		chosenFolder.repaint();
		revalidate();
		repaint();
	}

	public void playIntro() {
		chosenFolder.repaint();
	}

	// updates the subtitles (e.g. after a language change or a new host)
	private void refreshCards() {
		for (IdCard card : cards) {
			String name = card.getPlayerName();
			if (name != null) {
				card.setPlayer(name, subtitleFor(name));
			}
		}
	}

	// only the host's card gets a subtitle: "HOST"
	private String subtitleFor(String name) {
		return name.equals(hostName) ? Lang.t("host_tag") : "";
	}

	// called for every PLAYERS message
	public void setPlayers(ArrayList<String> newNames) {
		names = newNames;
		int delay = 0;
		for (int i = 0; i < SLOTS; i++) {
			IdCard card = cards[i];
			if (i < names.size()) {
				String name = names.get(i);
				boolean me = name.equals(myName);
				boolean changed = !name.equals(card.getPlayerName());
				card.setPlayer(name, subtitleFor(name));
				if (changed) {
					card.setStamp(me ? "you" : "", false);
					card.setStatus("", Theme.MUTED, me ? Theme.INK : Theme.CARD_EDGE);
					if (!shown.contains(name)) {
						card.revealIn(150 + delay);      // only brand-new agents get the reveal
						delay += 120;
						if (me) {
							stampLater(card);
						}
					}
				}
			} else {
				card.setEmpty();
			}
		}
		shown.clear();
		shown.addAll(names);
		updateHostParts();
	}

	private void stampLater(final IdCard card) {
		Anim.later(700, new Runnable() {
			public void run() {
				card.setStamp("you", true);
			}
		});
	}

	// title, count, footer text, OPEN CASE and the cabinet depend on who is host
	private void updateHostParts() {
		int players = names.size();
		boolean host = iAmHost();

		String title = host ? Lang.t("you_are_host") : Lang.f("host_is", hostLabel());
		Theme.text(pickTitle, Theme.stencil(22f), Lang.rtl() ? title : title.toUpperCase());
		Theme.text(countLabel, Theme.monoBold(16f), Lang.f("present", Math.min(players, SLOTS), SLOTS));

		String footer;
		if (players < MIN_PLAYERS) {
			footer = Lang.f("need_more", MIN_PLAYERS - players);
		} else if (host) {
			footer = "";
		} else {
			footer = Lang.f("waiting_for", hostLabel());
		}
		Theme.text(footerText, Theme.type(17f), footer);

		chosenFolder.setClickable(host && lobbyOpen);
		if (!(host && lobbyOpen)) {
			popup.close();                     // a round started, or we are not host any more
		}
		startButton.setVisible(host);
		startButton.setEnabled(host && lobbyOpen && players >= MIN_PLAYERS);
	}

	public void setStartEnabled(boolean on) {
		lobbyOpen = on;
		updateHostParts();
	}
}
