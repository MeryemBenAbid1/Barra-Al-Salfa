/* =====================================================================
 *  SpyClient.java  -  "Al-Dakheel" (الدخيل) Game Client
 *  CpE 458 - Network Programming - Dr. Mohamad Awad
 * ---------------------------------------------------------------------
 *  BIG PICTURE
 *   1. The sign-in screen asks for the codename (nickname) and server IP.
 *   2. We connect a Socket to the server (port 1202) and send the
 *      nickname as the FIRST message.
 *   3. A separate thread (run method) receives messages from the server.
 *      Each message is handed to the Swing thread (invokeLater), which
 *      updates the screens.
 *   4. Buttons send messages to the server (CHAT:, GUESS:, START_GAME, VOTE:).
 *   5. The language button (top bar) switches English <-> Arabic and
 *      flips the whole window right-to-left. See Lang.java.
 *   6. If the server answers NAME_TAKEN, we go back to the sign-in screen.
 *   7. Voice: hold HOLD TO TALK to speak. Sound goes over UDP (VoiceChat.java).
 *   8. Host screen: "OPEN AS HOST SCREEN" connects as "#HOST" and shows the
 *      big TV view (HostScreen.java) instead of playing.
 *   9. Category files: the lobby shows the server's files; PICK:<name> chooses.
 *
 *  The protocol is exactly the same as before, so SpyServer.java does
 *  not need any change.
 *
 *  SCREENS (one CardLayout, switched by the PHASE messages)
 *   CONNECT  -> ConnectScreen   sign-in
 *   LOBBY    -> LobbyScreen     agents on file, OPEN CASE
 *   ROUND    -> RoundScreen     secret card, questions, last chance
 *   VOTE     -> VoteScreen      who is Al-Dakheel?
 *   RESULT   -> ResultScreen    the verdict
 *   HOST     -> HostScreen      the big screen for a gathering (TV)
 *
 *  SECTION MAP (search for the tag, e.g. "[C6]")
 *   [C1] Settings          [C5] Connecting           [C9]  Result, XP, profile
 *   [C2] Variables         [C6] Sending to server    [C10] Switching screens
 *   [C3] main              [C7] Receiving thread     [C11] Painted frame parts
 *   [C4] Building window   [C8] Handling messages
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.*;
import java.util.*;

public class SpyClient extends JFrame implements Runnable {

	// =================================================================
	// [C1] SETTINGS
	// =================================================================
	private static final int PORT = 1202;                               // same port as the server
	// the server's ROLE text for the outsider (both the old and the new name work)
	private static final String[] OUTSIDER_TEXTS = { "You are Brra Al-Salfa!", "You are Al-Dakheel!" };

	// server INFO lines we do NOT print in the chat, because the screen
	// already shows them (ID cards, vote counter, timer...). Less text!
	private static final String[] HIDDEN_INFO = {
		" joined the game!", " has left the game.", " has voted.",
		"Welcome to Al-Dakheel", "Welcome back", " started! Category:",
		"Time is up! Vote", "Voting time is over!" };

	// =================================================================
	// [C2] VARIABLES
	// =================================================================
	private static Socket socket = null;
	private Scanner input;
	private PrintWriter output;
	private String myName = "";

	// game state on this side
	private boolean inRound = false;      // true after we receive our ROLE
	private boolean guessMode = false;    // true when we must guess the word
	private boolean wasOutsider = false;  // were we Al-Dakheel this round?
	private int myLevel = 0;              // to detect "LEVEL UP"
	private String myRank = "";           // to detect a new rank title
	private ArrayList<String> players = new ArrayList<String>();

	// result of this round (XP can arrive before or after RESULT)
	private boolean nameRejected = false; // the server said NAME_TAKEN
	private boolean hostMode = false;     // this window is the TV host screen
	private String pendingWordAr = "";    // WORD_AR arrives just before ROLE
	private boolean resultShowing = false;
	private int roundXp = -1;
	private boolean roundWin = false;

	// window parts
	private final Hud hud = new Hud();
	private final FolderTab tab = new FolderTab();
	private final CardLayout cards = new CardLayout();
	private final JPanel deck = new JPanel(cards);
	private final JPanel paper = new PaperPanel();
	private final FolderCover cover = new FolderCover();
	private String current = "";
	private String tabKey = "tab_file";

	private final ConnectScreen connectScreen = new ConnectScreen();
	private final LobbyScreen lobby = new LobbyScreen();
	private final RoundScreen round = new RoundScreen();
	private final VoteScreen vote = new VoteScreen();
	private final ResultScreen result = new ResultScreen();
	private final ChatPanel chat = new ChatPanel();
	private final HostScreen hostScreen = new HostScreen();

	// push-to-talk voice (UDP)
	private final VoiceChat voice = new VoiceChat(new VoiceChat.Listener() {
		public void talking(String name, boolean on) {
			chat.setTalking(name, on);
		}
		public void problem(final String key) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					chat.addAlert(Lang.t(key));
				}
			});
		}
	});

	// =================================================================
	// [C3] MAIN
	// =================================================================
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SpyClient frame = new SpyClient();
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);

				// close the socket when the window is closed (like GetRemoteTime)
				frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						if (socket != null) {
							try {
								socket.close();
							} catch (IOException ioEx) {
								System.out.println("Unable to close link!");
							}
						}
						System.exit(0);
					}
				});
				frame.show("CONNECT");

				// optional quick start:  java SpyClient Nouri 192.168.1.5
				//                   or  java SpyClient --host-screen 192.168.1.5
				if (args.length >= 1 && args[0].equals("--host-screen")) {
					frame.hostMode = true;
					frame.connectScreen.setBusy(true);
					frame.connectToServer("#HOST", args.length > 1 ? args[1] : "localhost");
				} else if (args.length >= 1) {
					frame.connectScreen.setBusy(true);
					frame.connectToServer(args[0], args.length > 1 ? args[1] : "localhost");
				}
			}
		});
	}

	// =================================================================
	// [C4] BUILDING THE WINDOW
	//   desk background
	//     NORTH  = folder tab (left) + HUD (right)
	//     CENTER = manila folder > paper > the screens (CardLayout)
	// =================================================================
	public SpyClient() {
		setTitle("Al-Dakheel");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel desk = new DeskPanel();
		desk.setLayout(new BorderLayout());
		desk.setBorder(new EmptyBorder(16, 28, 28, 28));
		desk.setPreferredSize(new Dimension(1280, 800));
		setContentPane(desk);

		// --- top row: tab + HUD ---
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.add(tab, BorderLayout.SOUTH);
		top.add(tabHolder, BorderLayout.WEST);
		JPanel hudHolder = new JPanel(new BorderLayout());
		hudHolder.setOpaque(false);
		hudHolder.setBorder(new EmptyBorder(0, 0, 10, 0));
		hudHolder.add(hud, BorderLayout.SOUTH);
		top.add(hudHolder, BorderLayout.CENTER);
		desk.add(top, BorderLayout.NORTH);

		// --- folder + paper + screens ---
		JPanel folder = new FolderPanel();
		folder.setLayout(new BorderLayout());
		folder.setBorder(new EmptyBorder(14, 14, 24, 14));
		paper.setLayout(new BorderLayout());
		paper.setBorder(new EmptyBorder(22, 26, 22, 26));
		deck.setOpaque(false);
		deck.add(connectScreen, "CONNECT");
		deck.add(lobby, "LOBBY");
		deck.add(round, "ROUND");
		deck.add(vote, "VOTE");
		deck.add(result, "RESULT");
		deck.add(hostScreen, "HOST");
		paper.add(deck, BorderLayout.CENTER);
		folder.add(paper, BorderLayout.CENTER);
		desk.add(folder, BorderLayout.CENTER);

		setGlassPane(cover);

		// --- what the buttons do ---
		connectScreen.setConnectListener(new ConnectScreen.ConnectListener() {
			public void connect(String nickname, String host) {
				hostMode = false;
				connectToServer(nickname, host);
			}
			public void openHostScreen(String host) {
				hostMode = true;
				connectToServer("#HOST", host);     // the server knows #HOST is a display
			}
		});
		chat.setTalkListener(new ChatPanel.TalkListener() {
			public boolean start() {
				return voice.startTalking();
			}
			public void stop() {
				voice.stopTalking();
			}
		});
		lobby.setPickListener(new LobbyScreen.PickListener() {
			public void pick(String category) {
				send("PICK:" + category);
			}
		});
		// the TV host screen can also pick the file and open the case
		hostScreen.setHostListener(new HostScreen.HostListener() {
			public void pick(String category) {
				send("PICK:" + category);
			}
			public void openCase() {
				send("START_GAME");
			}
		});
		chat.setSendListener(new ChatPanel.SendListener() {
			public void send(String text) {
				sendChatOrGuess(text);
			}
		});
		lobby.getStartButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send("START_GAME");
			}
		});
		result.getNewCaseButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send("START_GAME");
			}
		});
		result.getLobbyButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resultShowing = false;
				hud.clearRound();
				show("LOBBY");
			}
		});
		vote.setVoteListener(new VoteScreen.VoteListener() {
			public void vote(String name) {
				send("VOTE:" + name);            // only ONE vote per round
				chat.addInfo(Lang.f("you_accused", name));
			}
		});
		hud.getLanguageButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Lang.set(Lang.rtl() ? Lang.EN : Lang.AR);
				applyLanguage();
			}
		});
		applyLanguage();
	}

	// re-writes every text in the new language and flips the layout
	private void applyLanguage() {
		getContentPane().applyComponentOrientation(Lang.orientation());
		hud.applyLanguage();
		chat.applyLanguage();
		connectScreen.applyLanguage();
		lobby.applyLanguage();
		round.applyLanguage();
		vote.applyLanguage();
		result.applyLanguage();
		hostScreen.applyLanguage();
		tab.setText(Lang.t(tabKey));
		getContentPane().revalidate();
		getContentPane().repaint();
	}

	// =================================================================
	// [C5] CONNECTING TO THE SERVER
	//   runs in a background thread so the window doesn't freeze
	// =================================================================
	private void connectToServer(final String nickname, final String host) {
		new Thread(new Runnable() {
			public void run() {
				try {
					// create the socket and the streams
					socket = new Socket(host, PORT);
					input = new Scanner(socket.getInputStream());
					output = new PrintWriter(socket.getOutputStream(), true);

					// the FIRST message is the nickname
					output.println(nickname);

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							connected(nickname, host);
						}
					});
				} catch (UnknownHostException uhEx) {
					connectFailed("err_host", host);
				} catch (IOException ioEx) {
					connectFailed("err_connect", "");
				}
			}
		}).start();
	}

	private void connectFailed(final String key, final String value) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				connectScreen.setBusy(false);
				connectScreen.showError(key, value);
			}
		});
	}

	private void connected(String nickname, String host) {
		nameRejected = false;
		if (hostMode) {
			// the TV screen: no player, no chat, no voice
			setTitle("Al-Dakheel  -  Host Screen");
			hostScreen.setServerAddress(host);
			show("HOST");
			setExtendedState(JFrame.MAXIMIZED_BOTH);   // fill the TV
			Thread receiveThread = new Thread(this);
			receiveThread.start();
			return;
		}
		myName = nickname;
		setTitle("Al-Dakheel  -  Agent " + myName);
		hud.setName(myName);
		chat.setMyName(myName);
		lobby.setMyName(myName);
		round.setMyName(myName);
		vote.setMyName(myName);
		result.getBoard().setMyName(myName);
		show("LOBBY");
		voice.start(host, myName);              // push-to-talk over UDP

		// start the thread that receives messages from the server
		Thread receiveThread = new Thread(this);
		receiveThread.start();
	}

	// =================================================================
	// [C6] SENDING TO THE SERVER
	// =================================================================
	private void send(String message) {
		if (output != null) {
			output.println(message);
		}
	}

	private void sendChatOrGuess(String text) {
		if (guessMode) {
			// last chance: this text is our guess, not a chat message
			send("GUESS:" + text);
			guessMode = false;
			chat.setGuessMode(false);
			round.setStatus("st_guess_sent", true);
		} else {
			send("CHAT:" + text);
		}
	}

	// =================================================================
	// [C7] RECEIVING THREAD
	//   waits for messages and passes each one to the Swing thread
	// =================================================================
	public void run() {
		try {
			do {
				final String message = input.nextLine();     // wait for the next line
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						handleMessage(message);
					}
				});
			} while (true);
		} catch (Exception ex) {
			// NoSuchElementException when the socket is closed (empty catch, as the brief says)
		}
		// the server is gone
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (nameRejected) {
					return;                      // we closed it ourselves (NAME_TAKEN)
				}
				voice.stop();
				chat.addAlert(Lang.t("conn_lost"));
				hud.setPhase("ph_disconnected", Theme.RED);
				lobby.setStartEnabled(false);
				chat.setInputEnabled(false);
				vote.lock();
				result.getNewCaseButton().setEnabled(false);
			}
		});
	}

	// =================================================================
	// [C8] HANDLING EACH MESSAGE TYPE (look at the prefix)
	// =================================================================
	private void handleMessage(String message) {
		// category names in Arabic (used by every screen)
		if (message.startsWith("CATEGORIES:")) {
			ArrayList<String> names = new ArrayList<String>();
			for (String item : message.substring(11).split(",")) {
				String[] parts = item.split("\\|", -1);     // "Food|أكل"
				if (parts[0].trim().length() > 0) {
					names.add(parts[0].trim());
					Lang.rememberCategory(parts[0].trim(), parts.length > 1 ? parts[1] : "");
				}
			}
			lobby.setCategories(names);
			hostScreen.setCategories(names);
		}
		// the host screen draws everything itself 
		if (hostMode) {
			hostScreen.handle(message);
			return;
		}

		if (message.equals("NAME_TAKEN")) {
			nameTaken();
		}
		else if (message.startsWith("CHAT:")) {
			chat.addChat(message.substring(5));
		}
		else if (message.startsWith("INFO:")) {
			if (!isHiddenInfo(message.substring(5))) {
				chat.addInfo(message.substring(5));
			}
		}
		else if (message.startsWith("PLAYERS:")) {
			updatePlayers(message.substring(8));
		}
		else if (message.startsWith("PICKED:")) {
			// "Food|Meryem" = Meryem picked the Food file
			String[] parts = message.substring(7).split("\\|", -1);
			lobby.setPicked(parts[0]);          // the file cabinet animates the choice
		}
		else if (message.startsWith("HOST:")) {
			// who may pick the file and open the case
			lobby.setHost(message.substring(5));
			result.getNewCaseButton().setVisible(lobby.iAmHost());
		}
		else if (message.startsWith("WORD_AR:")) {
			pendingWordAr = message.substring(8);
		}
		else if (message.startsWith("ROLE:")) {
			receiveRole(message.substring(5));
		}
		else if (message.startsWith("CATEGORY:")) {
			hud.setCategory(message.substring(9));
		}
		else if (message.startsWith("PHASE:")) {
			setPhase(message.substring(6));
		}
		else if (message.startsWith("TIMER:")) {
			try {
				hud.setTime(Integer.parseInt(message.substring(6).trim()));
			} catch (NumberFormatException ex) {
				// ignore a bad number
			}
		}
		else if (message.startsWith("VOTES:")) {
			vote.setVotes(message.substring(6));
		}
		else if (message.equals("GUESS_TURN")) {
			guessMode = true;
			chat.setGuessMode(true);
			show("ROUND");
			round.getCard().reveal(SecretCard.GUESS, "");
			round.setStatus("st_caught_you", true);
			chat.focusInput();
		}
		else if (message.startsWith("RESULT:")) {
			showResult(message.substring(7));
		}
		else if (message.startsWith("XP:")) {
			receiveXp(message.substring(3));
		}
		else if (message.startsWith("PROFILE:")) {
			updateProfile(message.substring(8));
		}
		else if (message.startsWith("SCORES:")) {
			result.getBoard().setScores(message.substring(7));
		}
	}

	// true if this INFO line is one of the HIDDEN_INFO ones
	private boolean isHiddenInfo(String text) {
		for (int i = 0; i < HIDDEN_INFO.length; i++) {
			if (text.contains(HIDDEN_INFO[i])) {
				return true;
			}
		}
		return false;
	}

	// the server refused our codename: close and go back to sign-in
	private void nameTaken() {
		nameRejected = true;
		voice.stop();
		try {
			socket.close();
		} catch (IOException ioEx) {
			// already closed
		}
		output = null;
		chat.clear();
		hud.clearRound();
		hud.hideBadge();
		show("CONNECT");
		connectScreen.setBusy(false);
		connectScreen.setCodename(myName);
		connectScreen.showError("err_taken", "");
	}

	// "a,b,c" -> list of names
	private void updatePlayers(String list) {
		players = new ArrayList<String>();
		String[] names = list.split(",");
		for (int i = 0; i < names.length; i++) {
			if (!names[i].trim().equals("")) {
				players.add(names[i].trim());
			}
		}
		lobby.setPlayers(players);
		round.setPlayers(players);
	}

	private void receiveRole(String role) {
		inRound = true;
		roundXp = -1;
		if (isOutsiderText(role)) {
			wasOutsider = true;
			round.getCard().reveal(SecretCard.OUTSIDER, "");
		} else {
			wasOutsider = false;
			// if the server sends "Secret word: X", keep only X
			String word = role;
			if (word.contains(":")) {
				word = word.substring(word.lastIndexOf(':') + 1).trim();
			}
			Lang.rememberWord(word, pendingWordAr);    // Arabic from the category file
			pendingWordAr = "";
			round.getCard().reveal(SecretCard.WORD, word);
		}
	}

	private boolean isOutsiderText(String role) {
		for (int i = 0; i < OUTSIDER_TEXTS.length; i++) {
			if (role.equalsIgnoreCase(OUTSIDER_TEXTS[i])) {
				return true;
			}
		}
		return false;
	}

	// PHASE messages switch the screen
	private void setPhase(String phase) {
		if (phase.equals("LOBBY")) {
			inRound = false;
			guessMode = false;
			chat.setGuessMode(false);
			lobby.setStartEnabled(true);
			round.getCard().set(SecretCard.SEALED, "");
			if (!resultShowing && !current.equals("CONNECT")) {
				hud.clearRound();
				show("LOBBY");
			}
		}
		else if (phase.equals("DISCUSS")) {
			resultShowing = false;
			lobby.setStartEnabled(false);
			hud.setPhase("ph_interrogation", Theme.INK);
			round.setStatus(inRound ? "st_interrogation" : "st_progress", false);
			show("ROUND");
			// if no ROLE arrives, we joined in the middle: we are only watching
			Anim.later(900, new Runnable() {
				public void run() {
					if (!inRound) {
						round.getCard().reveal(SecretCard.OBSERVER, "");
						round.setStatus("st_observer", false);
					}
				}
			});
		}
		else if (phase.equals("VOTE")) {
			lobby.setStartEnabled(false);
			hud.setPhase("ph_accusation", Theme.RED);
			vote.open(players, inRound);         // only players of this round can vote
			show("VOTE");
		}
		else if (phase.equals("GUESS")) {
			lobby.setStartEnabled(false);
			hud.setPhase("ph_last_chance", Theme.RED);
			if (!guessMode) {
				round.setStatus("st_guessing", true);
			}
			show("ROUND");
		}
	}

	// =================================================================
	// [C9] RESULT, XP, PROFILE
	// =================================================================
	private void showResult(String text) {
		resultShowing = true;
		guessMode = false;
		chat.setGuessMode(false);
		chat.addAlert(Lang.t("case_closed_alert"));
		hud.setPhase("ph_verdict", Theme.INK);
		hud.hideTimer();
		result.showResult(text, myName);
		show("RESULT");
		if (roundXp >= 0) {
			applyVerdict();                      // XP arrived before RESULT
		} else {
			// spectators get no XP: give them a neutral stamp
			Anim.later(1500, new Runnable() {
				public void run() {
					if (roundXp < 0 && resultShowing && !result.hasVerdict()) {
						result.slamVerdict(inRound ? "v_case_filed" : "v_case_report", Theme.INK);
					}
				}
			});
		}
	}

	// "160,WIN"
	private void receiveXp(String data) {
		String[] parts = data.split(",");
		try {
			roundXp = Integer.parseInt(parts[0].trim());
		} catch (NumberFormatException ex) {
			roundXp = 0;
		}
		roundWin = parts.length > 1 && parts[1].trim().equals("WIN");
		if (resultShowing) {
			applyVerdict();
		}
	}

	private void applyVerdict() {
		final String stamp;
		if (wasOutsider) {
			stamp = roundWin ? "v_escaped" : "v_caught";
		} else {
			stamp = roundWin ? "v_case_closed" : "v_case_failed";
		}
		result.showXp(roundXp, roundWin);
		Anim.later(700, new Runnable() {
			public void run() {
				result.slamVerdict(stamp, (wasOutsider || roundWin) ? Theme.RED : Theme.INK);
			}
		});
		Theme.play(roundWin ? "win" : "lose");
		chat.addAlert(Lang.t(stamp) + "  +" + roundXp + " XP");
	}

	// data looks like "Detective,4,12,7,340"  (rank, level, games, wins, xp)
	private void updateProfile(String data) {
		String[] parts = data.split(",");
		if (parts.length < 5) {
			return;
		}
		try {
			String rank = parts[0];
			int level = Integer.parseInt(parts[1].trim());
			int games = Integer.parseInt(parts[2].trim());
			int wins = Integer.parseInt(parts[3].trim());
			int xp = Integer.parseInt(parts[4].trim());

			boolean levelUp = myLevel != 0 && level > myLevel;
			boolean newRank = myRank.length() > 0 && !rank.equals(myRank);
			myRank = rank;
			if (newRank) {
				chat.addAlert(Lang.f("promoted_alert", Lang.t(rank)));
			} else if (levelUp) {
				chat.addAlert(Lang.f("levelup_alert", level));
			}
			myLevel = level;

			hud.setProfile(myName, rank, level, xp);
			if (resultShowing) {
				result.showProfile(level, rank, xp, levelUp, newRank);
			}
			int winRate = games > 0 ? wins * 100 / games : 0;
			hud.setToolTipText("Games " + games + "   Wins " + wins + "   Win rate " + winRate + "%");
		} catch (NumberFormatException ex) {
			// ignore a broken profile line
		}
	}

	// =================================================================
	// [C10] SWITCHING SCREENS
	//   the folder cover slides away to reveal the next screen
	// =================================================================
	private void show(String name) {
		if (name.equals(current)) {
			return;
		}
		boolean first = current.length() == 0;
		current = name;
		if (name.equals("LOBBY")) {
			lobby.attachChat(chat);
			tabKey = "tab_file";
		} else if (name.equals("ROUND")) {
			round.attachChat(chat);
			tabKey = "tab_active";
		} else if (name.equals("VOTE")) {
			tabKey = "tab_accusation";
		} else if (name.equals("RESULT")) {
			tabKey = "tab_verdict";
		} else if (name.equals("HOST")) {
			tabKey = "tab_host";
		} else {
			tabKey = "tab_file";
		}
		tab.setText(Lang.t(tabKey));
		chat.applyComponentOrientation(Lang.orientation());
		cards.show(deck, name);
		if (!first) {
			Theme.play("swoosh");
		}
		cover.slideAway(SwingUtilities.convertRectangle(paper.getParent(), paper.getBounds(), getRootPane()));

		if (name.equals("CONNECT")) {
			connectScreen.playIntro();
		} else if (name.equals("LOBBY")) {
			lobby.playIntro();
		} else if (name.equals("ROUND")) {
			round.playIntro();
		}
	}

	// =================================================================
	// [C11] PAINTED PARTS OF THE FRAME
	// =================================================================
	// dark wooden desk
	private static class DeskPanel extends JPanel {
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			g2.setColor(Theme.DESK);
			g2.fillRect(0, 0, getWidth(), getHeight());
			// a few soft wood lines
			Random random = new Random(458);
			for (int i = 0; i < 26; i++) {
				int y = random.nextInt(Math.max(1, getHeight()));
				g2.setColor(new Color(0, 0, 0, 18 + random.nextInt(18)));
				g2.fillRect(0, y, getWidth(), 1 + random.nextInt(3));
			}
			g2.dispose();
		}
	}

	// manila folder (square top-left corner where the tab joins)
	private static class FolderPanel extends JPanel {
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			int w = getWidth();
			int h = getHeight() - 10;
			g2.setColor(Theme.SHADOW);
			g2.fillRoundRect(0, 10, w, h, 28, 28);
			g2.setColor(Theme.FOLDER);
			g2.fillRoundRect(0, 0, w, h, 28, 28);
			// square corner where the tab joins (top-right in Arabic)
			g2.fillRect(Lang.rtl() ? w - 30 : 0, 0, 30, 30);
			g2.dispose();
		}
	}

	// the paper sheet inside the folder
	private static class PaperPanel extends JPanel {
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			g2.setColor(Theme.PAPER);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
			g2.dispose();
		}
	}

	// the folder tab on top ("CASE FILE Nº 458")
	private static class FolderTab extends JComponent {
		private String text = "";

		void setText(String t) {
			text = t;
			revalidate();
			repaint();
		}

		public Dimension getPreferredSize() {
			FontMetrics fm = getFontMetrics(Theme.safe(Theme.stencil(18f), text));
			return new Dimension(fm.stringWidth(text) + 56, 42);
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Theme.smooth(g);
			g2.setColor(Theme.FOLDER);
			g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 24, 24);
			g2.setColor(new Color(59, 48, 36));
			g2.setFont(Theme.safe(Theme.stencil(18f), text));
			Theme.centerText(g2, text, 0, 2, getWidth(), getHeight());
			g2.dispose();
		}
	}

	// glass pane: a manila cover that slides up when the screen changes
	private static class FolderCover extends JComponent {
		private Rectangle area = new Rectangle();
		private float offset = 1f;    // 0 = fully covering, 1 = gone

		void slideAway(Rectangle paperArea) {
			area = paperArea;
			setVisible(true);
			Anim.run(420, new Anim.Step() {
				public void update(float t) {
					offset = Anim.easeOut(t);
					repaint();
				}
			}, new Runnable() {
				public void run() {
					setVisible(false);
				}
			});
		}

		protected void paintComponent(Graphics g) {
			if (offset >= 1f) {
				return;
			}
			Graphics2D g2 = Theme.smooth(g);
			g2.clipRect(area.x, area.y, area.width, area.height);
			int y = area.y - (int) (area.height * offset);
			g2.setColor(new Color(201, 178, 132));
			g2.fillRoundRect(area.x, y, area.width, area.height, 12, 12);
			g2.setColor(new Color(0, 0, 0, 60));
			g2.fillRect(area.x, y + area.height - 6, area.width, 6);
			String title = Lang.t("cover_title");
			String classified = Lang.t("classified");
			g2.setColor(new Color(59, 48, 36));
			g2.setFont(Theme.safe(Theme.stencil(54f), title));
			Theme.centerText(g2, title, area.x, y, area.width, area.height - 60);
			g2.setColor(Theme.RED);
			g2.setFont(Theme.safe(Theme.stencil(26f), classified));
			Theme.centerText(g2, classified, area.x, y + 60, area.width, area.height - 60);
			g2.dispose();
		}
	}
}
