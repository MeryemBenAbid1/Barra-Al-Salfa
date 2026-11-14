/* =====================================================================
 *  HostScreen.java  -  the big shared screen for a gathering (TV / projector)
 * ---------------------------------------------------------------------
 *  The host screen connects with "#HOST" as its name. The server sends it
 *  every broadcast but never a ROLE, so the secret word is never shown
 *  on the TV. Players join from their own laptops.
 *
 *  When a host screen is connected it is THE HOST: the drawer of files
 *  pops up on it (FilePopup), whoever sits at this computer picks a
 *  tag and presses OPEN CASE.
 *
 *  One view per phase (CardLayout):
 *   LOBBY   how to join (server address), the files, the agents, MOST WANTED
 *   ROUND   a huge timer, the question order, the lineup
 *   VOTE    WHO IS AL-DAKHEEL?, a VOTED stamp lands on each player who votes
 *   GUESS   the outsider was caught: last chance + timer
 *   RESULT  the reveal: who it was, the word, who won
 *
 *  The window can be maximized on a TV: the timer grows with the window.
 * ===================================================================== */

import java.awt.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import javax.swing.*;

public class HostScreen extends JPanel {

	// what the host screen can ask the server (SpyClient sends it)
	public interface HostListener {
		void pick(String category);
		void openCase();
	}

	private final CardLayout views = new CardLayout();
	private final JPanel deck = new JPanel(views);
	private String current = "LOBBY";

	// header
	private final Stamp fileStamp = new Stamp("", Theme.RED, 22f, -3);
	private String fileName = "RANDOM";

	// lobby
	private final JLabel joinTitle = new JLabel();
	private final JLabel joinHow = new JLabel();
	private final JLabel address = new JLabel();
	private final JLabel pickTitle = new JLabel();
	private final ChosenFolder chosenFolder = new ChosenFolder();
	private final FilePopup popup = new FilePopup();
	private final DossierButton openButton = new DossierButton("", DossierButton.STAMP, 26f, -2);
	private final IdCard[] lobbyCards = new IdCard[12];
	private HostListener hostListener;
	private boolean screenIsHost = false;    // HOST:#SCREEN received
	private boolean lobbyOpen = true;        // false while a round is running
	private final Leaderboard lobbyBoard = new Leaderboard(8);

	// round
	private final BigTimer roundTimer = new BigTimer();
	private final JLabel roundTitle = new JLabel();
	private final JLabel orderTitle = new JLabel();
	private final JLabel orderList = new JLabel();
	private final JPanel lineup = new JPanel();
	private ArrayList<String[]> order = new ArrayList<String[]>();   // {asker, answerer}

	// vote
	private final BigTimer voteTimer = new BigTimer();
	private final JLabel voteTitle = new JLabel();
	private final JLabel voteCount = new JLabel();
	private final JPanel voteGrid = new JPanel();
	private final ArrayList<IdCard> voteCards = new ArrayList<IdCard>();
	private final HashSet<String> voted = new HashSet<String>();
	private int votesDone = 0, votesTotal = 0;

	// guess
	private final BigTimer guessTimer = new BigTimer();
	private final JLabel caughtTitle = new JLabel();
	private final JLabel lastChance = new JLabel();
	private String caughtName = "";

	// result
	private final Polaroid mugshot = new Polaroid("mugshot", "Al-Dakheel unmasked", "", -4, true);
	private final JLabel suspectLabel = new JLabel();
	private final JLabel wasLabel = new JLabel();
	private final JLabel wordTitle = new JLabel();
	private final JLabel wordLabel = new JLabel();
	private final JLabel wordArabic = new JLabel();
	private final JLabel reasonLabel = new JLabel();
	private final Stamp winnerStamp = new Stamp("", Theme.RED, 34f, -4);
	private final Leaderboard resultBoard = new Leaderboard(8);
	private String rOutsider = "", rWord = "", rReason = "";
	private boolean rOutsiderWon = false;

	private ArrayList<String> players = new ArrayList<String>();
	private String serverAddress = "";

	public HostScreen() {
		setOpaque(false);
		setLayout(new BorderLayout(0, 16));

		// ---------- header ----------
		JPanel header = new JPanel(new BorderLayout(20, 0));
		header.setOpaque(false);
		header.setBorder(new DashedBorder(DashedBorder.BOTTOM, false, Theme.INK, 8));
		JPanel titles = new JPanel(new FlowLayout(FlowLayout.LEADING, 18, 0));
		titles.setOpaque(false);
		JLabel title = new JLabel("AL-DAKHEEL");
		title.setFont(Theme.stencil(44f));
		title.setForeground(Theme.INK);
		JLabel arabic = new JLabel("الدخيل");
		arabic.setFont(Theme.arabic(42f));
		arabic.setForeground(Theme.RED);
		titles.add(title);
		titles.add(arabic);
		header.add(titles, BorderLayout.LINE_START);
		fileStamp.setFill(Theme.PAPER);
		header.add(fileStamp, BorderLayout.LINE_END);
		add(header, BorderLayout.NORTH);

		deck.setOpaque(false);
		deck.add(buildLobby(), "LOBBY");
		deck.add(buildRound(), "ROUND");
		deck.add(buildVote(), "VOTE");
		deck.add(buildGuess(), "GUESS");
		deck.add(buildResult(), "RESULT");
		add(deck, BorderLayout.CENTER);
		applyLanguage();
	}

	// ================= building the views =================
	private JPanel buildLobby() {
		JPanel view = new JPanel(new BorderLayout(30, 0));
		view.setOpaque(false);

		JPanel join = new JPanel();
		join.setOpaque(false);
		join.setLayout(new BoxLayout(join, BoxLayout.Y_AXIS));
		join.setPreferredSize(new Dimension(380, 100));
		joinTitle.setForeground(Theme.INK);
		joinHow.setForeground(Theme.INK_SOFT);
		address.setFont(Theme.stencil(54f));
		address.setForeground(Theme.RED);
		join.add(Box.createVerticalStrut(10));
		join.add(joinTitle);
		join.add(Box.createVerticalStrut(14));
		join.add(joinHow);
		join.add(Box.createVerticalStrut(10));
		join.add(address);
		join.add(Box.createVerticalGlue());
		view.add(join, BorderLayout.LINE_START);

		// center: the files on top, the agents and OPEN CASE below
		JPanel center = new JPanel(new BorderLayout(0, 14));
		center.setOpaque(false);
		pickTitle.setForeground(Theme.INK);
		pickTitle.setBorder(new DashedBorder(DashedBorder.BOTTOM, false, Theme.INK, 6));
		center.add(pickTitle, BorderLayout.NORTH);
		center.add(chosenFolder, BorderLayout.CENTER);
		chosenFolder.setOnClick(new Runnable() {
			public void run() {
				popup.open();
			}
		});
		popup.getCabinet().setPickListener(new FileCabinet.PickListener() {
			public void pick(String category) {
				if (hostListener != null) {
					hostListener.pick(category);
				}
			}
		});

		JPanel bottom = new JPanel(new BorderLayout(18, 0));
		bottom.setOpaque(false);
		JPanel grid = new JPanel(new GridLayout(2, 6, 10, 10));
		grid.setOpaque(false);
		grid.setPreferredSize(new Dimension(600, 250));
		for (int i = 0; i < lobbyCards.length; i++) {
			lobbyCards[i] = new IdCard();
			grid.add(lobbyCards[i]);
		}
		bottom.add(grid, BorderLayout.CENTER);
		openButton.setPreferredSize(new Dimension(260, 84));
		openButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (hostListener != null) {
					hostListener.openCase();
				}
			}
		});
		JPanel buttonHolder = new JPanel(new GridBagLayout());     // keeps the button its size
		buttonHolder.setOpaque(false);
		buttonHolder.add(openButton);
		bottom.add(buttonHolder, BorderLayout.LINE_END);
		center.add(bottom, BorderLayout.SOUTH);
		view.add(center, BorderLayout.CENTER);
		lobbyBoard.setPreferredSize(new Dimension(260, 100));
		view.add(lobbyBoard, BorderLayout.LINE_END);
		return FilePopup.over(view, popup);      // the drawer pops up on top
	}

	private JPanel buildRound() {
		JPanel view = new JPanel(new BorderLayout(30, 18));
		view.setOpaque(false);

		JPanel center = new JPanel(new BorderLayout(0, 6));
		center.setOpaque(false);
		roundTitle.setForeground(Theme.INK);
		roundTitle.setHorizontalAlignment(JLabel.CENTER);
		center.add(roundTitle, BorderLayout.NORTH);
		center.add(roundTimer, BorderLayout.CENTER);
		view.add(center, BorderLayout.CENTER);

		JPanel orderPanel = new JPanel(new BorderLayout(0, 10));
		orderPanel.setOpaque(false);
		orderPanel.setPreferredSize(new Dimension(380, 100));
		orderTitle.setForeground(Theme.INK);
		orderTitle.setBorder(new DashedBorder(DashedBorder.BOTTOM, false, Theme.INK, 4));
		orderList.setForeground(Theme.INK);
		orderList.setVerticalAlignment(JLabel.TOP);
		orderPanel.add(orderTitle, BorderLayout.NORTH);
		orderPanel.add(orderList, BorderLayout.CENTER);
		view.add(orderPanel, BorderLayout.LINE_END);

		lineup.setOpaque(false);
		lineup.setPreferredSize(new Dimension(600, 180));
		view.add(lineup, BorderLayout.SOUTH);
		return view;
	}

	private JPanel buildVote() {
		JPanel view = new JPanel(new BorderLayout(30, 16));
		view.setOpaque(false);
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		voteTitle.setForeground(Theme.INK);
		voteCount.setForeground(Theme.RED);
		top.add(voteTitle, BorderLayout.LINE_START);
		top.add(voteCount, BorderLayout.LINE_END);
		view.add(top, BorderLayout.NORTH);
		voteGrid.setOpaque(false);
		JPanel gridHolder = new JPanel(new BorderLayout());     // cards keep their height
		gridHolder.setOpaque(false);
		gridHolder.add(voteGrid, BorderLayout.NORTH);
		view.add(gridHolder, BorderLayout.CENTER);
		voteTimer.setPreferredSize(new Dimension(330, 100));
		view.add(voteTimer, BorderLayout.LINE_END);
		return view;
	}

	private JPanel buildGuess() {
		JPanel view = new JPanel(new BorderLayout(0, 10));
		view.setOpaque(false);
		JPanel text = new JPanel(new GridLayout(2, 1, 0, 6));
		text.setOpaque(false);
		caughtTitle.setForeground(Theme.RED);
		caughtTitle.setHorizontalAlignment(JLabel.CENTER);
		lastChance.setForeground(Theme.INK);
		lastChance.setHorizontalAlignment(JLabel.CENTER);
		text.add(caughtTitle);
		text.add(lastChance);
		view.add(text, BorderLayout.NORTH);
		view.add(guessTimer, BorderLayout.CENTER);
		return view;
	}

	private JPanel buildResult() {
		JPanel view = new JPanel(new BorderLayout(36, 0));
		view.setOpaque(false);
		mugshot.setPreferredSize(new Dimension(320, 420));
		JPanel photoHolder = new JPanel(new GridBagLayout());
		photoHolder.setOpaque(false);
		photoHolder.add(mugshot);
		view.add(photoHolder, BorderLayout.LINE_START);

		JPanel info = new JPanel();
		info.setOpaque(false);
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		suspectLabel.setForeground(Theme.INK);
		wasLabel.setForeground(Theme.INK_SOFT);
		wordTitle.setForeground(Theme.MUTED);
		wordLabel.setForeground(Theme.RED);
		wordArabic.setForeground(Theme.INK_SOFT);
		reasonLabel.setForeground(Theme.INK);
		JPanel stampHolder = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		stampHolder.setOpaque(false);
		stampHolder.add(winnerStamp);
		stampHolder.setPreferredSize(new Dimension(500, 110));
		stampHolder.setMaximumSize(new Dimension(3000, 110));
		info.add(Box.createVerticalStrut(20));
		info.add(suspectLabel);
		info.add(wasLabel);
		info.add(Box.createVerticalStrut(18));
		info.add(wordTitle);
		info.add(wordLabel);
		info.add(wordArabic);
		info.add(Box.createVerticalStrut(18));
		info.add(stampHolder);
		info.add(Box.createVerticalStrut(10));
		info.add(reasonLabel);
		info.add(Box.createVerticalGlue());
		view.add(info, BorderLayout.CENTER);
		resultBoard.setPreferredSize(new Dimension(260, 100));
		view.add(resultBoard, BorderLayout.LINE_END);
		return view;
	}

	// ================= language =================
	public void applyLanguage() {
		Theme.text(joinTitle, Theme.stencil(34f), Lang.t("join_case"));
		joinHow.setFont(Theme.safe(Theme.type(18f), Lang.t("join_how")));
		joinHow.setText(Lang.html("join_how"));
		openButton.setText(Lang.t("open_case"));
		updateHostParts();
		popup.applyLanguage();
		chosenFolder.repaint();
		Theme.text(roundTitle, Theme.stencil(34f), Lang.t("host_round"));
		Theme.text(orderTitle, Theme.stencil(22f), Lang.t("host_order"));
		Theme.text(voteTitle, Theme.stencil(48f), Lang.t("vote_title"));
		Theme.text(lastChance, Theme.type(26f), Lang.t("host_last"));
		updateFileStamp();
		updateOrder();
		updateVoteCount();
		updateCaught();
		updateResultText();
		updateLineups();
		alignBoxes(this);
		revalidate();
		repaint();
	}

	// BoxLayout columns line up on the right in Arabic
	private void alignBoxes(Container c) {
		for (Component child : c.getComponents()) {
			if (child instanceof JPanel) {
				JPanel p = (JPanel) child;
				if (p.getLayout() instanceof BoxLayout) {
					Lang.alignColumn(p);
				}
				alignBoxes(p);
			}
		}
	}

	// ================= messages from the server =================
	public void setServerAddress(String host) {
		serverAddress = host;
		if (host.equals("localhost") || host.startsWith("127.")) {
			serverAddress = lanAddress();          // the TV must show the real address
		}
		address.setText(serverAddress);
		// make the font smaller until the whole address fits (no "192.168.6...")
		float size = 54f;
		address.setFont(Theme.stencil(size));
		while (size > 20f && address.getFontMetrics(address.getFont()).stringWidth(serverAddress) > 370) {
			size -= 2f;
			address.setFont(Theme.stencil(size));
		}
	}

	public void setHostListener(HostListener l) { hostListener = l; }

	// CATEGORIES message (read by SpyClient): the files to choose from
	public void setCategories(ArrayList<String> names) {
		popup.getCabinet().setCategories(names);
	}

	// only when this screen is the host: files clickable, OPEN CASE visible
	private void updateHostParts() {
		boolean canStart = screenIsHost && lobbyOpen;
		chosenFolder.setClickable(canStart);
		if (!canStart) {
			popup.close();
		}
		openButton.setVisible(screenIsHost);
		openButton.setEnabled(canStart && players.size() >= LobbyScreen.MIN_PLAYERS);
		String title = screenIsHost ? Lang.t("you_are_host") : Lang.t("host_waiting");
		Theme.text(pickTitle, Theme.stencil(26f), Lang.rtl() ? title : title.toUpperCase());
	}

	public void handle(String message) {
		if (message.startsWith("HOST:")) {
			boolean wasHost = screenIsHost;
			screenIsHost = message.substring(5).equals(LobbyScreen.SCREEN_HOST);
			updateHostParts();
			if (screenIsHost && !wasHost && lobbyOpen) {
				popup.open();                    // the room is ready: choose a file
			}
		} else if (message.startsWith("PLAYERS:")) {
			players = new ArrayList<String>();
			for (String n : message.substring(8).split(",")) {
				if (n.trim().length() > 0) {
					players.add(n.trim());
				}
			}
			updateLineups();
			updateHostParts();                   // OPEN CASE needs 2 players
		} else if (message.startsWith("PICKED:")) {
			String[] parts = message.substring(7).split("\\|", -1);
			fileName = parts[0];
			updateFileStamp();
			popup.setPicked(parts[0]);           // stamp, then the pop-up closes
			chosenFolder.setTopic(parts[0]);
		} else if (message.startsWith("CATEGORY:")) {
			fileName = message.substring(9).trim();
			updateFileStamp();
		} else if (message.startsWith("PHASE:")) {
			phase(message.substring(6));
		} else if (message.startsWith("TIMER:")) {
			try {
				int s = Integer.parseInt(message.substring(6).trim());
				roundTimer.setSeconds(s);
				voteTimer.setSeconds(s);
				guessTimer.setSeconds(s);
			} catch (NumberFormatException ex) {
				// ignore
			}
		} else if (message.startsWith("INFO:Question order:")) {
			order.clear();
			for (String pair : message.substring(20).split(",")) {
				String[] who = pair.trim().split(" asks ");
				if (who.length == 2) {
					order.add(new String[] { who[0].trim(), who[1].trim() });
				}
			}
			updateOrder();
		} else if (message.startsWith("INFO:") && message.contains(" has been caught!")) {
			caughtName = message.substring(5, message.indexOf(" has been caught!"));
			updateCaught();
		} else if (message.startsWith("VOTED:")) {
			String name = message.substring(6);
			voted.add(name);
			for (IdCard c : voteCards) {
				if (name.equals(c.getPlayerName())) {
					c.setStamp("voted", true);
				}
			}
		} else if (message.startsWith("VOTES:")) {
			try {
				String[] parts = message.substring(6).split("/");
				votesDone = Integer.parseInt(parts[0].trim());
				votesTotal = Integer.parseInt(parts[1].trim());
			} catch (Exception ex) {
				// ignore
			}
			updateVoteCount();
		} else if (message.startsWith("RESULT:")) {
			showResult(message.substring(7));
		} else if (message.startsWith("SCORES:")) {
			lobbyBoard.setScores(message.substring(7));
			resultBoard.setScores(message.substring(7));
		}
	}

	private void phase(String phase) {
		lobbyOpen = phase.equals("LOBBY");
		updateHostParts();
		if (phase.equals("LOBBY")) {
			if (!current.equals("RESULT")) {
				show("LOBBY");
			} else {
				// keep the reveal on the TV for a while, then back to the lobby
				Anim.later(20000, new Runnable() {
					public void run() {
						if (current.equals("RESULT")) {
							show("LOBBY");
						}
					}
				});
			}
		} else if (phase.equals("DISCUSS")) {
			voted.clear();
			caughtName = "";
			order.clear();
			updateOrder();
			show("ROUND");
		} else if (phase.equals("VOTE")) {
			voted.clear();
			updateLineups();
			show("VOTE");
		} else if (phase.equals("GUESS")) {
			show("GUESS");
		}
	}

	private void show(String name) {
		if (name.equals(current)) {
			return;
		}
		current = name;
		views.show(deck, name);
		Theme.play("swoosh");
		if (name.equals("RESULT")) {
			mugshot.dropIn();
		}
	}

	// ================= updating parts =================
	private void updateFileStamp() {
		fileStamp.setStamp(Lang.f("host_file", Lang.category(fileName).toUpperCase()), Theme.RED);
		fileStamp.revalidate();
	}

	private void updateOrder() {
		if (order.isEmpty()) {
			orderList.setText("");
			return;
		}
		String body = "";
		for (int i = 0; i < order.size(); i++) {
			body = body + (i + 1) + ".  " + Lang.f("host_asks", order.get(i)[0], order.get(i)[1]) + "<br><br>";
		}
		orderList.setFont(Theme.safe(Theme.type(22f), body));
		orderList.setText(Lang.rtl() ? "<html><div align='right'>" + body + "</div></html>" : "<html>" + body + "</html>");
	}

	private void updateVoteCount() {
		Theme.text(voteCount, Theme.stencil(30f), Lang.f("host_vote_count", votesDone, votesTotal));
	}

	private void updateCaught() {
		Theme.text(caughtTitle, Theme.stencil(54f), Lang.f("host_caught", caughtName.toUpperCase()));
	}

	// the three rows of ID cards (lobby, round lineup, vote)
	private void updateLineups() {
		for (int i = 0; i < lobbyCards.length; i++) {
			if (i < players.size()) {
				lobbyCards[i].setPlayer(players.get(i), "");
			} else {
				lobbyCards[i].setEmpty();
			}
		}
		lineup.removeAll();
		int count = Math.min(players.size(), 10);
		lineup.setLayout(new GridLayout(1, Math.max(6, count), 12, 0));
		for (int i = 0; i < count; i++) {
			IdCard c = new IdCard();
			c.setPlayer(players.get(i), "");
			lineup.add(c);
		}
		for (int i = count; i < 6; i++) {
			JPanel space = new JPanel();
			space.setOpaque(false);
			lineup.add(space);
		}
		lineup.applyComponentOrientation(Lang.orientation());
		lineup.revalidate();

		voteGrid.removeAll();
		voteCards.clear();
		voteGrid.setLayout(new GridLayout(players.size() > 4 ? 2 : 1, 4, 14, 14));
		for (String name : players) {
			IdCard c = new IdCard();
			c.setPreferredSize(new Dimension(150, players.size() > 4 ? 200 : 250));
			c.setPlayer(name, "");
			if (voted.contains(name)) {
				c.setStamp("voted", false);
			}
			voteCards.add(c);
			voteGrid.add(c);
		}
		voteGrid.applyComponentOrientation(Lang.orientation());
		voteGrid.revalidate();
		repaint();
	}

	// "Outsider: X | Word: Y | WordAr: Z | Winner: W | reason"
	private void showResult(String line) {
		rOutsider = "";
		rWord = "";
		rReason = "";
		rOutsiderWon = false;
		for (String part : line.split("\\|")) {
			part = part.trim();
			if (part.startsWith("Outsider:")) {
				rOutsider = part.substring(9).trim();
			} else if (part.startsWith("WordAr:")) {
				Lang.rememberWord(rWord, part.substring(7).trim());
			} else if (part.startsWith("Word:")) {
				rWord = part.substring(5).trim();
			} else if (part.startsWith("Winner:")) {
				rOutsiderWon = part.contains("(");
			} else if (part.length() > 0) {
				rReason = part.replace("Brra Al-Salfa", "Al-Dakheel");
			}
		}
		mugshot.setCaption(rOutsider.toUpperCase());
		updateResultText();
		show("RESULT");
		winnerStamp.setVisible(false);
		Anim.later(900, new Runnable() {
			public void run() {
				winnerStamp.slam();
				Theme.play("win");
			}
		});
	}

	private void updateResultText() {
		Theme.text(suspectLabel, Theme.stencil(54f), rOutsider.toUpperCase());
		Theme.text(wasLabel, Theme.stencil(28f), Lang.t("host_was"));
		Theme.text(wordTitle, Theme.stencil(20f), Lang.t("host_word"));
		Theme.text(wordLabel, Theme.stencil(72f), rWord.toUpperCase());
		String ar = Lang.arabicWord(rWord);
		wordArabic.setFont(Theme.arabic(50f));
		wordArabic.setText(ar.length() > 0 ? ar : " ");
		winnerStamp.setStamp(Lang.t(rOutsiderWon ? "host_outsider_wins" : "host_agents_win"), Theme.RED);
		winnerStamp.revalidate();
		// wrap long sentences instead of cutting them with "..."
		reasonLabel.setFont(Theme.safe(Theme.type(22f), rReason));
		reasonLabel.setText("<html><div style='width:430px'>" + rReason + "</div></html>");
	}

	// finds this computer's address on the local network (e.g. 192.168.1.5)
	private static String lanAddress() {
		// 1. ask the system which address it uses to go out to the internet.
		//    connect() on a UDP socket sends NOTHING, it only chooses the route,
		//    so this gives the Wi-Fi address and not a virtual one.
		try {
			DatagramSocket probe = new DatagramSocket();
			probe.connect(InetAddress.getByName("8.8.8.8"), 53);
			InetAddress a = probe.getLocalAddress();
			probe.close();
			if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
				return a.getHostAddress();
			}
		} catch (Exception ex) {
			// no internet: try the network cards one by one (below)
		}
		// 2. look at the network cards, but skip the virtual ones that macOS
		//    creates (bridge100 for sharing/VMs, utun for VPNs, ...)
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			while (nets.hasMoreElements()) {
				NetworkInterface net = nets.nextElement();
				String n = net.getName();
				if (!net.isUp() || net.isLoopback() || net.isVirtual()
						|| n.startsWith("bridge") || n.startsWith("utun") || n.startsWith("vmnet")
						|| n.startsWith("vboxnet") || n.startsWith("docker")) {
					continue;
				}
				Enumeration<InetAddress> addresses = net.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress a = addresses.nextElement();
					if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
						return a.getHostAddress();
					}
				}
			}
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception ex) {
			return "localhost";
		}
	}

	// ================= a big timer that grows with the screen =================
	private static class BigTimer extends JComponent {
		private int seconds = 0;
		private float pulse = 1f;

		BigTimer() {
			setOpaque(false);
		}

		void setSeconds(int s) {
			seconds = s;
			if (s <= 10 && s > 0 && isShowing()) {
				Anim.run(300, new Anim.Step() {
					public void update(float t) {
						pulse = 1.08f - 0.08f * Anim.easeOut(t);
						repaint();
					}
				}, null);
			}
			repaint();
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int w = getWidth();
			int h = getHeight();
			boolean urgent = seconds <= 10;
			String text = (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
			// the biggest size that fits the box
			float size = Math.min(h * 0.75f, w * 0.38f);
			g2.setFont(Theme.stencil(size));
			FontMetrics fm = g2.getFontMetrics();
			g2.translate(w / 2.0, h / 2.0);
			g2.scale(pulse, pulse);
			g2.setColor(urgent ? Theme.RED : Theme.INK);
			g2.drawString(text, -fm.stringWidth(text) / 2, fm.getAscent() / 2 - (int) (size * 0.05f));
			g2.dispose();
		}
	}
}
