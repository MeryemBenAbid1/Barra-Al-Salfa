/* =====================================================================
 *  SpyClient.java  -  "Brra Al-Salfa" (برا السالفة) Game Client
 *  CpE 458 - Network Programming - Dr. Mohamad Awad
 * ---------------------------------------------------------------------
 *  BIG PICTURE
 *   1. The game opens as a Windows XP desktop (Desktop.java).
 *   2. The sign-in pop-up asks for the nickname and the server address.
 *   3. The player chooses a character, then we connect with a Socket to
 *      port 1202. The FIRST line we send is the nickname.
 *   4. A thread (run) reads every line from the server. Each line starts
 *      with a prefix (CHAT:, ROLE:, PLAYERS: ...). handleMessage looks
 *      at the prefix and updates the windows on the Swing thread
 *      (SwingUtilities.invokeLater), because only that thread may
 *      change the windows.
 *   5. Each part of the game is a set of XP windows on the desktop:
 *        LOBBY  -> Game Lobby + Chat
 *        DISCUSS-> Your secret word + Suspects + Time left + Chat
 *        VOTE   -> Vote + Chat
 *        GUESS  -> Last chance + Chat
 *        RESULT -> Round result        (then Scores after the last round)
 *
 *  SECTION MAP (search for the tag, e.g. "[C6]")
 *   [C1] Variables                  [C7] Screens (which windows to show)
 *   [C2] main + the desktop         [C8] Sending messages
 *   [C3] Sign in + Who is online    [C9] Voice chat (push-to-talk)
 *   [C4] Character + connect        [C10] Help, TV screen, language
 *   [C5] Receiving thread (run)
 *   [C6] handleMessage (one "if" per message prefix)
 *
 *  MESSAGES (Client -> Server)
 *   <nickname>        first line          #WHO   ask who is online
 *   #HOST             first line of the TV screen
 *   AVATAR:<code>     my character        CHAT:<text>
 *   PICK:<topic>      host picks a topic  START_GAME
 *   VOTE:<name>       my vote             GUESS:<word>
 *  (Server -> Client: see handleMessage [C6] and the top of SpyServer)
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class SpyClient extends JFrame implements Runnable {

	// =================================================================
	// [C1] VARIABLES
	// =================================================================
	private static final int PORT = 1202;
	private static final int ROUNDS = 3;

	// network
	private Socket socket;
	private Scanner input;
	private PrintWriter output;
	private boolean connected = false;
	private boolean tvMode = false;              // this computer is the TV screen

	// me
	private String myName = "";
	private String serverAddress = "";
	private Avatar myAvatar = null;

	// the players and the game (everything the server told us)
	private Vector<String> players = new Vector<String>();
	private HashMap<String, Avatar> avatars = new HashMap<String, Avatar>();
	private String host = "";
	private ArrayList<String> topics = new ArrayList<String>();
	private String picked = "RANDOM";
	private String phase = "LOBBY";
	private int round = 0;
	private int timeLeft = 0, phaseTotal = -1;
	private int role = WordWindow.WATCHER;
	private String word = "", wordArabic = "", topic = "";
	// question turns (bonus): TURN:<q>/<total>|<asker>|<answerer>
	private String turnAsker = null, turnAnswerer = null;
	private int question = 0, questions = 0;
	private Vector<String> roundPlayers = new Vector<String>();
	private int votesDone = 0, votesTotal = 0;
	private String lastVoter = null;
	private String caught = "";
	private String[] guessOptions = new String[0];
	private String guessed = null;
	private HashMap<String, String> voteList = new HashMap<String, String>();
	private HashMap<String, Integer> roundPoints = new HashMap<String, Integer>();
	private ArrayList<ScoresWindow.Score> scores = new ArrayList<ScoresWindow.Score>();
	private boolean gameOver = false;
	private ArrayList<String[]> history = new ArrayList<String[]>();
	private ArrayList<String> playedTopics = new ArrayList<String>();
	private String resultText = "";

	// the screen we are on: SIGNIN, CHARACTER, LOBBY, ROUND, VOTE, GUESS, RESULT, SCORES, TV
	private String screen = "SIGNIN";

	// the windows
	private final Desktop desktop = new Desktop();
	private final ChatWindow chat = new ChatWindow();
	private SignInDialog signIn;
	private OnlineWindow online;
	private CharacterWindow character;
	private SuspectsWindow suspects;
	private TimeWindow timeWindow;
	private VoteWindow voteWindow;
	private GuessWindow guessWindow;
	private TvWindow tv;
	private LobbyWindow lobbyWindow;
	private Set<Integer> takenFaces = new HashSet<Integer>();
	private VoiceChat voice;

	// =================================================================
	// [C2] MAIN + THE DESKTOP
	// =================================================================
	public static void main(String[] args) {
		// draw at 100% so the 1280 x 800 desktop fits on scaled laptop screens
		System.setProperty("sun.java2d.uiScale", "1");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new SpyClient();
			}
		});
	}

	public SpyClient() {
		setTitle("Brra Al-Salfa");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		desktop.setPreferredSize(new Dimension(Desktop.WIDTH, Desktop.HEIGHT));
		setContentPane(desktop);

		// the 4 desktop icons (top left)
		desktop.addIcon(Lang.t("i_help"), Xp.RANDOM, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showHelp(0);
			}
		});
		desktop.addIcon(Lang.t("i_tv"), Xp.TV, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startTvMode();
			}
		});
		desktop.addIcon(Lang.t("i_rules"), Xp.PAPER, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showHelp(5);
			}
		});
		desktop.addIcon(Lang.t("i_scores"), Xp.EVENTS, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (connected && !tvMode) {
					showScoresOverlay();
				} else {
					desktop.balloon(Xp.pixelIcon(Xp.SMILE, 3), Lang.t("scores"), Lang.t("no_rounds"));
				}
			}
		});

		// "Help" in the menu bar of every window
		desktop.helpListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showHelp(0);
			}
		};

		// the language button in the tray (عربي / EN)
		Xp.onClick(desktop.taskbar().languageButton(), new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switchLanguage();
			}
		});

		// the chat window: what happens when we send / talk
		chat.setListener(new ChatWindow.Listener() {
			public void send(String text) {
				sendMessage("CHAT:" + text);
			}
			public boolean startTalking() {
				return voice != null && voice.startTalking();
			}
			public void stopTalking() {
				if (voice != null) {
					voice.stopTalking();
				}
			}
		});

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		showSignIn();
		startWhoIsOnline();
	}

	// =================================================================
	// [C3] SIGN IN + WHO IS ONLINE
	// =================================================================
	private void showSignIn() {
		screen = "SIGNIN";
		desktop.clearWindows();
		online = new OnlineWindow();
		online.setActive(false);
		XpWindow tip = OnlineWindow.tip();
		tip.setActive(false);
		desktop.place(online, 820, 90, 400, 352);
		desktop.place(tip, 820, 470, 400, 110);
		desktop.taskbar().setTabs(Lang.t("sign_in"), Lang.t("who_online"));

		// the sign-in pop-up (a JDialog, see SignInDialog.java)
		if (signIn != null) {
			signIn.dispose();
		}
		signIn = new SignInDialog(desktop, myName, serverAddress, new SignInDialog.Listener() {
			public void next(String name, String server) {
				myName = name;
				serverAddress = server;
				showCharacter();
			}
			public void help() {
				showHelp(0);
			}
		});
		signIn.show();
	}

	// "Who is online": every 3 seconds, a short connection asks the server
	// "#WHO" and it answers "ONLINE:Nouri=1-1-1,Sara=3-3-5" (bonus)
	private void startWhoIsOnline() {
		Thread who = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (!connected && (screen.equals("SIGNIN") || screen.equals("CHARACTER"))) {
						String host = signIn == null ? serverAddress : signIn.server();
						final String answer = askWhoIsOnline(host.length() == 0 ? "localhost" : host);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								showWhoIsOnline(answer);
							}
						});
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException ex) {
						return;
					}
				}
			}
		});
		who.setDaemon(true);          // stops by itself when the game closes
		who.start();
	}

	// one short TCP connection: send "#WHO", read one line, close
	private String askWhoIsOnline(String host) {
		try {
			Socket s = new Socket();
			s.connect(new InetSocketAddress(host, PORT), 1000);     // wait at most 1 second
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			Scanner in = new Scanner(s.getInputStream(), "UTF-8");
			out.println("#WHO");
			String line = in.nextLine();
			s.close();
			return line;
		} catch (Exception ex) {
			return null;                         // no server there
		}
	}

	// "ONLINE:Nouri=1-1-1,Sara=3-3-5" -> the list in the window
	private void showWhoIsOnline(String answer) {
		if (online == null) {
			return;
		}
		if (answer == null || !answer.startsWith("ONLINE:")) {
			online.setOffline();
			return;
		}
		String list = answer.substring(7);
		String[] parts = list.length() == 0 ? new String[0] : list.split(",");
		String[] names = new String[parts.length];
		Avatar[] faces = new Avatar[parts.length];
		takenFaces.clear();
		for (int i = 0; i < parts.length; i++) {
			String[] nameAndCode = parts[i].split("=");
			names[i] = nameAndCode[0];
			faces[i] = nameAndCode.length > 1 ? Avatar.parse(nameAndCode[1]) : Avatar.forName(names[i]);
			takenFaces.add(faces[i].face);
		}
		online.setPlayers(names, faces);
	}

	// =================================================================
	// [C4] CHARACTER + CONNECT
	// =================================================================
	private void showCharacter() {
		screen = "CHARACTER";
		desktop.clearWindows();
		// start with the first free character
		Avatar start = myAvatar;
		if (start == null) {
			int face = 0;
			while (takenFaces.contains(face) && face < Avatar.COUNT - 1) {
				face++;
			}
			start = Avatar.preset(face);
		}
		character = new CharacterWindow(myName, takenFaces.size() + 1, start, takenFaces, new CharacterWindow.Listener() {
			public void back() {
				showSignIn();
			}
			public void join(Avatar chosen) {
				myAvatar = chosen;
				connectToServer(false);
			}
		});
		placeAuto(character.preview(), 990, 24, 270);
		character.preview().setActive(false);
		desktop.place(character, 128, 24, 840, 680);
		desktop.taskbar().setTabs(Lang.t("choose_char"), Lang.t("preview"));
		desktop.paintImmediately(0, 0, Desktop.WIDTH, Desktop.HEIGHT);   // the new screen first,
		hideSignInLater();                                               // then the pop-up goes
	}

	// connects in a new thread, so the window does not freeze while waiting
	private void connectToServer(final boolean asTv) {
		final String host = serverAddress.length() == 0 ? "localhost" : serverAddress;
		Thread connector = new Thread(new Runnable() {
			public void run() {
				try {
					// 1. the socket (wait at most 3 seconds)
					Socket s = new Socket();
					s.connect(new InetSocketAddress(host, PORT), 3000);
					socket = s;
					input = new Scanner(socket.getInputStream(), "UTF-8");
					output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
					// 2. the FIRST line is the nickname (or #HOST for the TV)
					if (asTv) {
						output.println("#HOST");
					} else {
						output.println(myName);
						output.println("AVATAR:" + myAvatar.code());
					}
					connected = true;
					tvMode = asTv;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							joined();
						}
					});
					// 3. this thread now becomes the receiving thread
					SpyClient.this.run();
				} catch (final IOException ex) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							connectFailed(host);
						}
					});
				}
			}
		});
		connector.start();
	}

	// we are in: show the lobby (or the TV)
	private void joined() {
		if (signIn != null) {
			signIn.dispose();
			signIn = null;
		}
		chat.clear();
		chat.setMyName(myName);
		if (tvMode) {
			showTv();
			return;
		}
		chat.setNameColor(myName, Avatar.SHIRT[myAvatar.shirt]);
		avatars.put(myName, myAvatar);
		showLobby();
		// voice chat: connect to the server's UDP voice relay
		voice = new VoiceChat(new VoiceChat.Listener() {
			public void talking(String name, boolean on) {
				chat.setTalking(name, on);
			}
			public void problem(String langKey) {
				chat.addInfo(Lang.t(langKey));
			}
		});
		voice.start(serverAddress.length() == 0 ? "localhost" : serverAddress, myName);
	}

	// the server is not there: the XP error box, then sign in again
	private void connectFailed(String host) {
		showSignIn();
		signIn.setActive(false);
		ErrorDialog.show(desktop, Lang.t("err_title"), Lang.f("err_connect", "<b>" + host + "</b>"), Lang.t("err_connect2"));
		signIn.setActive(true);
	}

	// =================================================================
	// [C5] RECEIVING THREAD
	//  reads one line at a time; each line is handled on the Swing thread
	// =================================================================
	public void run() {
		try {
			while (true) {
				final String message = input.nextLine();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						handleMessage(message);
					}
				});
			}
		} catch (Exception ex) {
			// NoSuchElementException: the server closed the connection
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					disconnected("err_lost");
				}
			});
		}
	}

	// the connection ended (lost, kicked, or name taken)
	private void disconnected(String reasonKey) {
		if (!connected) {
			return;                        // already handled
		}
		connected = false;
		tvMode = false;
		if (voice != null) {
			voice.stop();
			voice = null;
		}
		try {
			socket.close();
		} catch (Exception ex) {
			// already closed
		}
		players.clear();
		avatars.clear();
		history.clear();
		playedTopics.clear();
		scores.clear();
		gameOver = false;
		showSignIn();
		signIn.setActive(false);
		if (reasonKey.equals("err_taken")) {
			ErrorDialog.show(desktop, Lang.t("err_title"), Lang.f("err_taken", "<b>" + myName + "</b>"), Lang.t("err_taken2"));
		} else {
			ErrorDialog.show(desktop, Lang.t("game"), Lang.t(reasonKey), "");
		}
		signIn.setActive(true);
	}

	// =================================================================
	// [C6] HANDLE ONE MESSAGE FROM THE SERVER  (look at the prefix)
	// =================================================================
	private void handleMessage(String message) {
		if (message.equals("NAME_TAKEN")) {
			disconnected("err_taken");

		} else if (message.equals("FULL")) {
			disconnected("err_full");

		} else if (message.equals("KICKED")) {
			disconnected("err_kicked");

		} else if (message.startsWith("CHAT:")) {
			chat.addChat(message.substring(5));                 // "Nouri: hi everyone!"

		} else if (message.startsWith("INFO:")) {
			chat.addInfo(Lang.info(message.substring(5)));   // Arabic: translated here

		} else if (message.equals("BLOCKED")) {
			chat.addAlert(Lang.t("blocked"));                   // the secret word was typed

		} else if (message.startsWith("PLAYERS:")) {
			// "PLAYERS:Meryem,Nouri,Alrayyan" -> the Vector of names
			players.clear();
			String list = message.substring(8);
			if (list.length() > 0) {
				players.addAll(Arrays.asList(list.split(",")));
			}
			playersChanged();

		} else if (message.startsWith("AVATARS:")) {
			// "AVATARS:Meryem=0-0-0,Nouri=1-1-1" -> the face of each player
			String[] parts = message.substring(8).split(",");
			for (int i = 0; i < parts.length; i++) {
				String[] nameAndCode = parts[i].split("=");
				if (nameAndCode.length == 2) {
					Avatar a = Avatar.parse(nameAndCode[1]);
					avatars.put(nameAndCode[0], a);
					chat.setNameColor(nameAndCode[0], Avatar.SHIRT[a.shirt]);
				}
			}
			playersChanged();

		} else if (message.startsWith("HOST:")) {
			host = message.substring(5);
			playersChanged();

		} else if (message.startsWith("CATEGORIES:")) {
			// "CATEGORIES:Food|أكل,Places|أماكن"
			topics.clear();
			String[] parts = message.substring(11).split(",");
			for (int i = 0; i < parts.length; i++) {
				String[] pair = parts[i].split("\\|");
				if (pair[0].length() > 0) {
					topics.add(pair[0]);
					Lang.remember(pair[0], pair.length > 1 ? pair[1] : "");
				}
			}
			if (screen.equals("LOBBY")) {
				refreshLobby();
			}

		} else if (message.startsWith("PICKED:")) {
			picked = message.substring(7).split("\\|")[0];
			if (screen.equals("LOBBY")) {
				refreshLobby();
			}

		} else if (message.startsWith("ROUND:")) {
			// "ROUND:1/3": a new round begins, forget the last one
			round = Integer.parseInt(message.substring(6).split("/")[0]);
			if (round == 1) {
				history.clear();
				playedTopics.clear();
				gameOver = false;
			}
			role = WordWindow.WATCHER;              // until ROLE says otherwise
			word = "";
			wordArabic = "";
			guessed = null;
			voteList.clear();
			roundPoints.clear();
			roundPlayers = new Vector<String>(players);
			turnAsker = null;                       // no question yet
			turnAnswerer = null;
			question = 0;

		} else if (message.startsWith("CATEGORY:")) {
			topic = message.substring(9);
			playedTopics.add(topic);

		} else if (message.startsWith("WORD_AR:")) {
			wordArabic = message.substring(8);

		} else if (message.startsWith("ROLE:")) {
			// THE ROLE: the secret word, or "You are Brra Al-Salfa!"
			String text = message.substring(5);
			if (text.startsWith("The secret word is: ")) {
				role = WordWindow.INSIDER;
				word = text.substring(20);
				Lang.remember(word, wordArabic);
			} else {
				role = WordWindow.OUTSIDER;
			}

		} else if (message.startsWith("TURN:")) {
			// "TURN:3/8|Meryem|Nouri" -> question 3 of 8: Meryem asks Nouri (bonus)
			String[] parts = message.substring(5).split("\\|");
			String[] qt = parts[0].split("/");
			question = Integer.parseInt(qt[0]);
			questions = Integer.parseInt(qt[1]);
			turnAsker = parts[1];
			turnAnswerer = parts[2];
			phaseTotal = -1;                        // a new countdown starts
			chat.addInfo(Lang.f("x_asks_y", turnAsker, turnAnswerer));
			showTurn();
			if (tvMode) {
				// the TV shows the turn itself
			} else if (turnAnswerer.equals(myName)) {
				desktop.balloon(Xp.pixelIcon(Xp.SMILE, 4), Lang.f("question_of", question, questions), Xp.rtlLine(Lang.f("turn_answer", turnAsker)));
			} else if (turnAsker.equals(myName)) {
				desktop.balloon(Xp.pixelIcon(Xp.SMILE, 4), Lang.f("question_of", question, questions), Xp.rtlLine(Lang.f("turn_ask", turnAnswerer)));
			} else {
				desktop.hideBalloon();
			}

		} else if (message.startsWith("PHASE:")) {
			phase = message.substring(6);
			phaseTotal = -1;                        // the next TIMER tells the length
			phaseChanged();

		} else if (message.startsWith("TIMER:")) {
			timeLeft = Integer.parseInt(message.substring(6));
			if (phaseTotal < 0) {
				phaseTotal = timeLeft;
			}
			timerTick();

		} else if (message.startsWith("VOTED:")) {
			lastVoter = message.substring(6);

		} else if (message.startsWith("VOTES:")) {
			// "VOTES:2/4" -> live vote tracking (bonus)
			String[] xy = message.substring(6).split("/");
			votesDone = Integer.parseInt(xy[0]);
			votesTotal = Integer.parseInt(xy[1]);
			if (voteWindow != null) {
				voteWindow.setVotes(votesDone, votesTotal);
			}
			if (tv != null && phase.equals("VOTE")) {
				tv.showVote(round, ROUNDS, timeLeft, Math.max(1, phaseTotal), votesDone, votesTotal);
			}
			if (lastVoter != null && votesDone > 0) {
				int left = votesTotal - votesDone;
				desktop.balloon(Xp.pixelIcon(Xp.SMILE, 4), Lang.f("x_voted", lastVoter),
						left == 0 ? Lang.t("all_voted") : left == 1 ? Lang.t("left_to_vote1") : Lang.f("left_to_vote", left));
				lastVoter = null;
			}

		} else if (message.startsWith("CAUGHT:")) {
			caught = message.substring(7);

		} else if (message.startsWith("GUESS_OPTIONS:")) {
			guessOptions = message.substring(14).split(",");

		} else if (message.equals("GUESS_TURN")) {
			// it is MY last chance (the window is already open, see phaseChanged)

		} else if (message.startsWith("GUESSED:")) {
			guessed = message.substring(8);

		} else if (message.startsWith("VOTE_LIST:")) {
			// "VOTE_LIST:Meryem>Alrayyan,Nouri>Alrayyan"
			voteList.clear();
			String[] parts = message.substring(10).split(",");
			for (int i = 0; i < parts.length; i++) {
				String[] pair = parts[i].split(">");
				if (pair.length == 2) {
					voteList.put(pair[0], pair[1]);
				}
			}

		} else if (message.startsWith("ROUND_POINTS:")) {
			// "ROUND_POINTS:Meryem=1,Alrayyan=0"
			roundPoints.clear();
			String[] parts = message.substring(13).split(",");
			for (int i = 0; i < parts.length; i++) {
				String[] pair = parts[i].split("=");
				if (pair.length == 2) {
					roundPoints.put(pair[0], Integer.parseInt(pair[1]));
				}
			}

		} else if (message.startsWith("RESULT:")) {
			resultText = message.substring(7);
			showResult();

		} else if (message.startsWith("SCORES:")) {
			// "SCORES:Meryem=5=3=1,Sara=4=2=1"  name=points=caught=wasOutsider
			scores.clear();
			String list = message.substring(7);
			String[] parts = list.length() == 0 ? new String[0] : list.split(",");
			for (int i = 0; i < parts.length; i++) {
				String[] v = parts[i].split("=");
				if (v.length == 4) {
					ScoresWindow.Score s = new ScoresWindow.Score();
					s.name = v[0];
					s.points = Integer.parseInt(v[1]);
					s.caught = Integer.parseInt(v[2]);
					s.outsider = Integer.parseInt(v[3]);
					scores.add(s);
				}
			}

		} else if (message.startsWith("GAME_OVER:")) {
			gameOver = true;
			if (tv != null) {
				tv.showWinner(message.substring(10));
			}
		}
	}

	// the list of players, their faces or the host changed
	private void playersChanged() {
		if (screen.equals("LOBBY")) {
			refreshLobby();
		} else if (tv != null) {
			tv.setPlayers(phase.equals("LOBBY") ? players : roundPlayers, avatars);
			if (phase.equals("LOBBY") && screen.equals("TV")) {
				tv.showLobby(serverAddress.length() == 0 ? "localhost" : serverAddress);
			}
		}
	}

	// the server moved the game to another phase
	private void phaseChanged() {
		if (tvMode) {
			tvPhase();
			return;
		}
		if (phase.equals("LOBBY")) {
			// after a result we stay on the result / scores until the next round
			if (!screen.equals("RESULT") && !screen.equals("SCORES")) {
				showLobby();
			}
		} else if (phase.equals("DISCUSS")) {
			showRound();
		} else if (phase.equals("VOTE")) {
			showVote();
		} else if (phase.equals("GUESS")) {
			showGuess();
		}
	}

	// every second: the time windows and "asking now"
	private void timerTick() {
		desktop.taskbar().setExtra(phase.equals("LOBBY") ? "" : XpParts.clock(timeLeft));
		if (tvMode) {
			tvPhase();
			return;
		}
		if (phase.equals("DISCUSS") && timeWindow != null) {
			timeWindow.setTime(timeLeft, phaseTotal);
		} else if (phase.equals("VOTE") && voteWindow != null) {
			voteWindow.setTime(timeLeft);
		} else if (phase.equals("GUESS") && guessWindow != null) {
			guessWindow.setTime(timeLeft);
		}
	}

	// the question turn changed: the Suspects window, the time window and the TV
	private void showTurn() {
		if (suspects != null) {
			suspects.setTurn(turnAsker, turnAnswerer, myName, question, questions);
		}
		if (timeWindow != null) {
			timeWindow.setQuestion(question, questions);
		}
		if (tvMode) {
			tvPhase();
		}
	}

	// =================================================================
	// [C7] SCREENS  (which XP windows are on the desktop)
	// =================================================================

	// puts a window that is as tall as it needs to be
	private void placeAuto(XpWindow w, int x, int y, int width) {
		w.setSize(width, 100);
		w.doLayout();
		int height = w.getPreferredSize().height;
		desktop.place(w, x, y, width, Math.min(height, Desktop.HEIGHT - Desktop.TASKBAR - y - 8));
	}

	private void showLobby() {
		if (screen.equals("LOBBY") && overlayOpen()) {
			return;          // help / scores is on top: the lobby is built again when it closes
		}
		// already in the lobby (someone joined, a topic was picked...):
		// only the lobby window is swapped; the chat stays, with what you typed
		boolean refresh = screen.equals("LOBBY") && lobbyWindow != null && lobbyWindow.getParent() == desktop;
		screen = "LOBBY";
		if (refresh) {
			desktop.remove(lobbyWindow);
		} else {
			desktop.clearWindows();
			desktop.hideBalloon();
		}
		desktop.taskbar().setExtra("");
		final LobbyWindow lobby = new LobbyWindow(topics, picked, myName, host, players, avatars,
				serverAddress.length() == 0 ? "localhost" : serverAddress, new LobbyWindow.Listener() {
			public void pickTopic(String t) {
				sendMessage("PICK:" + t);
			}
			public void startRound() {
				sendMessage("START_GAME");
			}
		});
		lobbyWindow = lobby;
		if (!refresh) {
			chat.setActive(false);
			chat.setInputEnabled(true);
			desktop.place(chat, 870, 24, 380, 680);
		}
		desktop.place(lobby, 128, 24, 730, 680);
		desktop.taskbar().setTabs(Lang.t("lobby"), Lang.t("chat"));
	}

	// many messages can change the lobby at once (PLAYERS, AVATARS, HOST when
	// someone joins): we build it ONCE, after all of them, instead of 3 times
	private boolean lobbyPending = false;

	private void refreshLobby() {
		if (lobbyPending) {
			return;
		}
		lobbyPending = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				lobbyPending = false;
				if (screen.equals("LOBBY")) {
					showLobby();
				}
			}
		});
	}

	private void showRound() {
		screen = "ROUND";
		desktop.clearWindows();
		desktop.hideBalloon();
		WordWindow wordWindow = new WordWindow(role, word, wordArabic, topic);
		suspects = new SuspectsWindow(roundPlayers, avatars, myName);
		timeWindow = new TimeWindow(round, ROUNDS);
		timeWindow.setTime(timeLeft, Math.max(1, phaseTotal));
		suspects.setTurn(turnAsker, turnAnswerer, myName, question, questions);
		timeWindow.setQuestion(question, questions);
		suspects.setActive(false);
		timeWindow.setActive(false);
		chat.setActive(false);
		desktop.place(chat, 870, 24, 380, 680);
		desktop.place(timeWindow, 128, 560, 730, 144);
		desktop.place(suspects, 128, 272, 730, 280);
		desktop.place(wordWindow, 128, 24, 730, 236);
		desktop.taskbar().setTabs(Lang.t("secret_word"), Lang.t("suspects"), Lang.t("chat"));
		chat.focusInput();
	}

	private void showVote() {
		screen = "VOTE";
		desktop.clearWindows();
		boolean playing = role != WordWindow.WATCHER;
		voteWindow = new VoteWindow(roundPlayers, avatars, myName, playing, new VoteWindow.Listener() {
			public void vote(String name) {
				sendMessage("VOTE:" + name);
			}
		});
		voteWindow.setVotes(votesDone = 0, votesTotal = roundPlayers.size());
		chat.setActive(false);
		desktop.place(chat, 870, 24, 380, 680);
		placeAuto(voteWindow, 128, 60, 730);       // next to the chat, not over it
		desktop.taskbar().setTabs(Lang.t("vote"), Lang.t("chat"));
	}

	private void showGuess() {
		screen = "GUESS";
		desktop.clearWindows();
		boolean myTurn = caught.equals(myName);
		Avatar face = avatars.get(caught);
		guessWindow = new GuessWindow(caught, face, guessOptions, myTurn, 20, new GuessWindow.Listener() {
			public void guess(String w) {
				sendMessage("GUESS:" + w);
			}
		});
		chat.setActive(false);
		desktop.place(chat, 870, 24, 380, 680);
		placeAuto(guessWindow, 128, 60, 730);
		desktop.taskbar().setTabs(Lang.t("last_chance"), Lang.t("chat"));
		if (!myTurn) {
			desktop.balloon(face == null ? Xp.pixelIcon(Xp.SMILE, 4) : face.icon(4),
					Lang.t("everyone_watching"), Lang.f("x_guessing", caught));
		}
	}

	// "Outsider: Alrayyan | Word: Shawarma | WordAr: شاورما | Winner: The players | reason"
	private String resultPart(String key) {
		String[] parts = resultText.split("\\|");
		for (int i = 0; i < parts.length; i++) {
			String p = parts[i].trim();
			if (p.startsWith(key + ": ")) {
				return p.substring(key.length() + 2).trim();
			}
		}
		return "";
	}

	// how the round ended, from the RESULT message
	private int ending() {
		boolean outsiderWon = resultPart("Winner").startsWith("Brra Al-Salfa");
		if (!outsiderWon) {
			return ResultWindow.CAUGHT;
		}
		return guessed != null ? ResultWindow.STOLEN : ResultWindow.ESCAPED;
	}

	private void showResult() {
		String outsider = resultPart("Outsider");
		String theWord = resultPart("Word");
		String arabic = resultPart("WordAr");
		Lang.remember(theWord, arabic);
		int end = ending();
		// remember this round for the "Round history" window
		boolean caughtOutsider = end != ResultWindow.ESCAPED;
		history.add(new String[] { "" + round, theWord, outsider, caughtOutsider ? "caught" : "escaped" });

		if (tvMode) {
			String verdict = Lang.t(end == ResultWindow.CAUGHT ? "players_win" : end == ResultWindow.STOLEN ? "outsider_steals" : "outsider_wins");
			tv.showResult(outsider, theWord, verdict);
			return;
		}
		screen = "RESULT";
		desktop.clearWindows();
		desktop.hideBalloon();
		desktop.taskbar().setExtra("");
		final ResultWindow result = new ResultWindow(round, ROUNDS, outsider, avatars.get(outsider), theWord, arabic,
				end, guessed, roundPlayers, avatars, voteList, roundPoints, myName.equals(host),
				new ResultWindow.Listener() {
			public void seeScores() {
				showScores();
			}
			public void nextRound() {
				showLobby();            // the host picks the next topic, then Start round
			}
		});
		placeAuto(result, 150, 40, 760);
		desktop.taskbar().setTabs(Lang.f("round_result", round));
		if (round < ROUNDS && !myName.equals(host)) {
			desktop.balloon(Xp.pixelIcon(Xp.SMILE, 4), Lang.f("round_of", round + 1, ROUNDS),
					Lang.f("wait_host_next", host, round + 1));
		} else if (round >= ROUNDS) {
			desktop.balloon(Xp.pixelIcon(Xp.SMILE, 4), Lang.t("game_over_b"), Lang.t("see_final"));
		}
	}

	private void showScores() {
		screen = "SCORES";
		desktop.clearWindows();
		desktop.hideBalloon();                  // the "last round" note is done
		ScoresWindow scoresWindow = new ScoresWindow(scores, avatars, history.size(), playedTopics, gameOver,
				myName.equals(host), new ScoresWindow.Listener() {
			public void exit() {
				System.exit(0);
			}
			public void playAgain() {
				showLobby();            // a new game starts with Start round
			}
			public void back() {
				if (history.size() > 0) {
					showResultAgain();         // back to the result we came from
				} else {
					showLobby();
				}
			}
		});
		scoresWindow.setCloseListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (history.size() > 0) {
					showResultAgain();
				} else {
					showLobby();
				}
			}
		});
		ScoresWindow.HistoryWindow historyWindow = new ScoresWindow.HistoryWindow(history, avatars);
		historyWindow.setActive(false);
		placeAuto(historyWindow, 910, 24, 350);
		desktop.place(scoresWindow, 128, 24, 760, 680);
		desktop.taskbar().setTabs(Lang.t("scores"), Lang.t("round_history"));
	}

	// the Scores icon during the game: the scores open ON TOP of the game,
	// and Back / X brings the game windows back (like Help)
	private void showScoresOverlay() {
		if (overlayOpen() || screen.equals("SCORES")) {
			return;
		}
		putAway();
		final ActionListener close = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bringBack();
			}
		};
		ScoresWindow scoresWindow = new ScoresWindow(scores, avatars, history.size(), playedTopics, gameOver,
				myName.equals(host), new ScoresWindow.Listener() {
			public void exit() {
				System.exit(0);
			}
			public void playAgain() {
				showLobby();
			}
			public void back() {
				bringBack();
			}
		});
		scoresWindow.setCloseListener(close);
		ScoresWindow.HistoryWindow historyWindow = new ScoresWindow.HistoryWindow(history, avatars);
		historyWindow.setCloseListener(close);
		historyWindow.setActive(false);
		overlay = scoresWindow;
		placeAuto(historyWindow, 910, 24, 350);
		desktop.place(scoresWindow, 128, 24, 760, 680);
		desktop.taskbar().setTabs(Lang.t("scores"), Lang.t("round_history"));
	}

	// "Back" from the scores goes back to the last result
	private void showResultAgain() {
		history.remove(history.size() - 1);       // showResult adds it again
		showResult();
	}

	// =================================================================
	// [C8] SENDING MESSAGES
	// =================================================================
	private void sendMessage(String message) {
		if (output != null && connected) {
			output.println(message);
		}
	}

	// =================================================================
	// [C9] VOICE CHAT: see joined() and the chat listener in [C2]
	//  VoiceChat.java sends the microphone over UDP while the
	//  "Hold to talk" button is pressed.
	// =================================================================

	// =================================================================
	// [C10] HELP, TV SCREEN, LANGUAGE
	// =================================================================
	// Help, Rules and the Scores icon open ON TOP of what is on the desktop:
	// the windows that are open now are put away, and come back on close.
	private ArrayList<XpWindow> before = new ArrayList<XpWindow>();
	private String[] beforeTabs = new String[0];
	private XpWindow overlay;                  // the help / scores window on top

	// is the help (or scores) window still on the desktop?
	private boolean overlayOpen() {
		return overlay != null && overlay.getParent() == desktop;
	}

	private void putAway() {
		before = new ArrayList<XpWindow>();
		Component[] all = desktop.getComponents();
		for (int i = all.length - 1; i >= 0; i--) {         // bottom window first
			if (all[i] instanceof XpWindow) {
				before.add((XpWindow) all[i]);
			}
		}
		beforeTabs = desktop.taskbar().tabs();
		desktop.clearWindows();
	}

	private void bringBack() {
		overlay = null;
		if (screen.equals("LOBBY")) {
			showLobby();                       // fresh: players may have joined meanwhile
			return;
		}
		desktop.clearWindows();
		for (int i = 0; i < before.size(); i++) {
			Rectangle r = before.get(i).getBounds();
			desktop.place(before.get(i), r.x, r.y, r.width, r.height);
		}
		desktop.taskbar().setTabs(beforeTabs);
		if (screen.equals("SIGNIN") && signIn != null) {
			signIn.show();
		}
	}

	// the sign-in pop-up would cover the help: hide it AFTER the help is
	// on the desktop, so there is never an empty desktop in between
	private void hideSignInLater() {
		if (signIn != null) {
			signIn.hide();
		}
	}

	private void showHelp(int page) {
		if (screen.equals("TV")) {
			return;
		}
		if (overlayOpen()) {
			if (overlay instanceof HelpWindow) {
				((HelpWindow) overlay).showPage(page);     // already open: just turn the page
				return;
			}
			bringBack();                                     // scores -> help
		}
		putAway();
		final XpWindow points = HelpWindow.pointsWindow();
		final HelpWindow help = new HelpWindow(null);
		ActionListener close = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bringBack();
			}
		};
		help.setCloseListener(close);
		points.setCloseListener(close);
		help.showPage(page);
		points.setActive(false);
		overlay = help;
		placeAuto(points, 930, 24, 330);
		desktop.place(help, 128, 24, 780, 680);
		desktop.taskbar().setTabs(Lang.t("i_help"), Lang.t("points"));
		desktop.paintImmediately(0, 0, Desktop.WIDTH, Desktop.HEIGHT);   // draw the help now
		hideSignInLater();
	}

	// the "TV screen" icon: this computer becomes the big shared screen
	// (a projector / TV in the room). First we explain it, then connect as #HOST.
	private void startTvMode() {
		if (tvMode) {
			return;                            // already the TV
		}
		if (connected) {
			// this window is a player: the TV is one more SpyClient window
			desktop.balloon(Xp.pixelIcon(Xp.TV, 3), Lang.t("i_tv"), Lang.t("tv_busy"));
			return;
		}
		if (signIn != null) {
			serverAddress = signIn.server();
			signIn.hide();                     // out of the way: the question is in front
		}
		boolean yes = ErrorDialog.ask(desktop, Lang.t("i_tv"), Lang.t("tv_q_title"),
				Lang.t("tv_q1") + "<br><br>" + Lang.t("tv_q2"), Lang.t("tv_go"), Lang.t("cancel"));
		if (!yes && signIn != null && screen.equals("SIGNIN")) {
			signIn.show();                     // Cancel: the sign in comes back
		}
		if (!yes) {
			return;
		}
		if (signIn != null) {
			signIn.hide();
		}
		connectToServer(true);
	}

	// the X on the TV window: stop being the TV and go back to sign in
	private void leaveTv() {
		connected = false;                     // so disconnected() shows no error
		tvMode = false;
		try {
			socket.close();                    // the server sees "bye"
		} catch (Exception ex) {
			// already closed
		}
		tv = null;
		desktop.showIcons(true);
		players.clear();
		avatars.clear();
		history.clear();
		playedTopics.clear();
		scores.clear();
		gameOver = false;
		showSignIn();
	}

	private void showTv() {
		screen = "TV";
		desktop.clearWindows();
		desktop.showIcons(false);
		tv = new TvWindow();
		tv.setPlayers(players, avatars);
		tv.showLobby(serverAddress.length() == 0 ? "localhost" : serverAddress);
		tv.setCloseListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				leaveTv();
			}
		});
		desktop.place(tv, 20, 14, 1240, 726);
		desktop.taskbar().setTabs(Lang.t("i_tv"));
	}

	// the TV screen follows the game
	private void tvPhase() {
		if (tv == null) {
			return;
		}
		int total = Math.max(1, phaseTotal);
		if (phase.equals("DISCUSS")) {
			tv.setPlayers(roundPlayers, avatars);
			tv.showRound(round, ROUNDS, topic, timeLeft, total, turnAsker, turnAnswerer);
		} else if (phase.equals("VOTE")) {
			tv.showVote(round, ROUNDS, timeLeft, total, votesDone, votesTotal);
		} else if (phase.equals("GUESS")) {
			tv.showGuess(caught, timeLeft, total);
		}
		// LOBBY: the TV keeps showing the result until the next round
	}

	// عربي <-> EN: every text comes from Lang, so we just build the screen again
	private void switchLanguage() {
		Lang.set(Lang.arabic() ? Lang.EN : Lang.AR);
		desktop.taskbar().setLanguageText(Lang.arabic() ? "EN" : "عربي");
		desktop.renameIcon(0, Lang.t("i_help"));
		desktop.renameIcon(1, Lang.t("i_tv"));
		desktop.renameIcon(2, Lang.t("i_rules"));
		desktop.renameIcon(3, Lang.t("i_scores"));
		chat.applyLanguage();
		overlay = null;
		desktop.clearWindows();                 // every window is built again, mirrored or not
		if (screen.equals("SIGNIN")) {
			if (signIn != null) {
				myName = signIn.name();
				serverAddress = signIn.server();
			}
			showSignIn();
		} else if (screen.equals("CHARACTER")) {
			showCharacter();
		} else if (screen.equals("LOBBY")) {
			showLobby();
		} else if (screen.equals("RESULT")) {
			showResultAgain();
		} else if (screen.equals("SCORES")) {
			showScores();
		} else if (screen.equals("TV")) {
			showTv();
			tvPhase();
		} else {
			phaseChanged();
		}
	}
}
