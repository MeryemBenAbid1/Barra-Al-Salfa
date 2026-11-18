/* =====================================================================
 *  SpyServer.java  -  "Brra Al-Salfa" (برا السالفة) Game Server
 *  CpE 458 - Network Programming - Dr. Mohamad Awad
 * ---------------------------------------------------------------------
 *  BIG PICTURE
 *   1. The server window opens (an XP desktop with a CMD window) and
 *      the server starts listening on port 1202 right away.
 *   2. A server thread waits for players with serverSocket.accept().
 *   3. Every player that connects gets its OWN ClientHandler thread.
 *   4. All the handlers are kept in a Vector called "clients".
 *   5. Every line starts with a prefix (CHAT:, VOTE:, ROLE: ...)
 *      so the server and the client know what kind of message it is.
 *
 *  THE CLASSES IN THIS FILE
 *   SpyServer     -> the window + the game rules (one object)
 *   ClientHandler -> one thread per connected player (or TV screen)
 *   GameTimer     -> a thread that counts down the seconds
 *   VoiceRelay    -> a thread that relays push-to-talk voice (UDP)
 *  The look (XP windows, the CMD log) comes from the shared kit files:
 *   Xp, XpWindow, XpButton, Desktop, Avatar, XpParts, CmdWindow
 *
 *  THE GAME
 *   - 3 rounds. The host (the first player in the Vector) picks a topic
 *     and presses Start round. If the host leaves, the next player is host.
 *   - One random player is the outsider (Brra Al-Salfa): no secret word.
 *   - Questions (120 s) -> vote (30 s) -> if caught: last guess (20 s).
 *   - Points: +1 to each player who voted for the outsider,
 *             +2 to the outsider if not caught,
 *             +1 to the outsider if caught but guessed the word.
 *
 *  SECTION MAP (search for the tag, e.g. "[S7]")
 *   [S1] Settings and word bank            [S8]  Voting
 *   [S2] Variables                         [S9]  Counting the votes
 *   [S3] main + server window              [S10] Last guess (8 choices)
 *   [S4] Controls (buttons)                [S11] End of round + points
 *   [S5] Server thread (accept loop)       [S12] Scores
 *   [S6] Players join / leave / chat       [S13] Player profiles (RandomAccessFile)
 *   [S7] Start a round                     [S14] Round history + server log (text files)
 *
 *  MESSAGES  (Client -> Server)
 *   <nickname>       first line = the player's name
 *   #WHO             first line: "who is online?" (answer, then close)
 *   #HOST            first line: a TV screen (watches, never gets a ROLE)
 *   AVATAR:<code>    the player's character, e.g. 4-2-0
 *   CHAT:<text>      a chat message
 *   PICK:<topic>     the host picks the topic (or RANDOM)
 *   START_GAME       the host pressed Start round
 *   VOTE:<name>      vote for <name>
 *   GUESS:<word>     the caught outsider's last guess
 *
 *  MESSAGES  (Server -> Client)
 *   NAME_TAKEN / FULL / KICKED      then the connection is closed
 *   ONLINE:<name=code>,...          answer to #WHO
 *   PLAYERS:<n1>,<n2>,...           the players (Vector order)
 *   AVATARS:<name=code>,...         the characters
 *   HOST:<name>                     who the host is
 *   CATEGORIES:<topic|arabic>,...   the topics      PICKED:<topic>|<who>
 *   CHAT:<name>: <text>             INFO:<text>     BLOCKED
 *   ROUND:<n>/<3>                   a new round
 *   CATEGORY:<topic>                WORD_AR:<arabic word>
 *   ROLE:<text>                     "The secret word is: X" or "You are Brra Al-Salfa!"
 *   ORDER:<n1>,<n2>,...             the turn order for questions
 *   PHASE:<LOBBY|DISCUSS|VOTE|GUESS>          TIMER:<seconds left>
 *   VOTED:<name>     VOTES:<x>/<y>  (live vote tracking)
 *   CAUGHT:<name>    GUESS_OPTIONS:<w1>,...,<w8>    GUESS_TURN    GUESSED:<word>
 *   VOTE_LIST:<voter>target>,...    ROUND_POINTS:<name=points>,...
 *   RESULT:Outsider: X | Word: Y | WordAr: Z | Winner: W | reason
 *   SCORES:<name=points=caught=wasOutsider>,...      GAME_OVER:<winner>
 *
 *  VOICE  (UDP port 1203, see VoiceRelay)
 *   every packet: [type byte][name (writeUTF)][audio bytes]
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.*;
import java.util.*;

public class SpyServer extends JFrame implements ActionListener, Runnable {

	// =================================================================
	// [S1] SETTINGS AND WORD BANK
	// =================================================================
	private static final int PORT = 1202;            // port the server listens on
	static final int VOICE_PORT = 1203;              // UDP port for push-to-talk voice
	private static final int MIN_PLAYERS = 3;        // need at least 3 players to start
	private static final int MAX_PLAYERS = 8;
	private static final int ROUNDS = 3;             // a game has 3 rounds

	// how long each part of a round lasts (seconds)
	private static int DISCUSS_TIME = 120;           // questions & answers  (java SpyServer 60 = 60 s)
	private static final int VOTE_TIME = 30;         // voting
	private static final int GUESS_TIME = 20;        // outsider's last guess

	// points
	private static final int CATCH_POINTS = 1;       // each player who voted for the outsider
	private static final int ESCAPE_POINTS = 2;      // the outsider was not caught
	private static final int GUESS_POINTS = 1;       // the outsider guessed the word

	// the words: one .txt file per topic in this folder (see loadCategories)
	private static final String CATEGORY_FOLDER = "categories";

	// built-in word bank, used only if the categories folder is missing
	// WORDS[i] belongs to the topic CATEGORIES[i]
	private static final String[] WORDS = {
		"Shawarma", "Machboos", "Falafel", "Kunafa", "Pizza", "Karak Tea", "Burger", "Sushi",
		"Kuwait Towers", "Airport", "Hospital", "University", "Cinema", "Beach", "Mall", "Mosque",
		"Doctor", "Pilot", "Teacher", "Chef", "Firefighter", "Engineer", "Police", "Barber" };
	private static final String[] CATEGORIES = {
		"Food", "Food", "Food", "Food", "Food", "Food", "Food", "Food",
		"Places", "Places", "Places", "Places", "Places", "Places", "Places", "Places",
		"Jobs", "Jobs", "Jobs", "Jobs", "Jobs", "Jobs", "Jobs", "Jobs" };

	// files
	private static final String PROFILE_FILE = "players.dat";   // RandomAccessFile
	private static final String HISTORY_FILE = "history.txt";   // normal text file
	private static final String LOG_FILE = "server_log.txt";    // every log line
	private static final int NAME_SIZE = 15;                    // 15 chars per name
	private static final int REC_SIZE = 42;                     // 30 (name) + 4 + 4 + 4 bytes

	// =================================================================
	// [S2] VARIABLES
	// =================================================================
	private ServerSocket serverSocket;

	// the list of connected players (the research part: Vector)
	private Vector<ClientHandler> clients = new Vector<ClientHandler>();
	// TV screens: they see the game but don't play
	private Vector<ClientHandler> displays = new Vector<ClientHandler>();

	// the state of the game
	private boolean gameRunning = false;
	private String phase = "LOBBY";            // LOBBY, DISCUSS, VOTE or GUESS
	private int roundNumber = 0;               // 1, 2, 3 (0 = no round yet)
	private boolean gameOver = false;          // true after round 3
	private String secretWord = "";
	private String secretWordArabic = "";      // "" if the file has no Arabic for it
	private String category = "";
	private ClientHandler outsider = null;     // the player who is Brra Al-Salfa
	private String lastGuess = null;           // the outsider's last guess (or null)
	private int secondsLeft = 0;

	// the topic files: topic name -> list of {English, Arabic} words
	private LinkedHashMap<String, ArrayList<String[]>> bank = new LinkedHashMap<String, ArrayList<String[]>>();
	private HashMap<String, String> categoryArabic = new HashMap<String, String>();
	private String pickedCategory = "RANDOM";  // chosen in the lobby

	// relays push-to-talk voice between the players
	private VoiceRelay voice = new VoiceRelay(this);

	// the window
	private CmdWindow cmd;
	private Desktop desktop = new Desktop();
	private JLabel stateLabel, topicLabel, countLabel;
	private JPanel playerList;
	private XpButton roundButton, kickButton, saveButton, stopButton;
	private String selectedPlayer = null;      // clicked in the status window (for Kick)
	private PrintWriter logFile;

	// =================================================================
	// [S3] MAIN + SERVER WINDOW
	// =================================================================
	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale", "1");     // the 1280 x 800 desktop fits
		// "java SpyServer 60" gives 60 seconds of questions (shorter demo)
		if (args.length > 0) {
			DISCUSS_TIME = Integer.parseInt(args[0]);
		}
		SpyServer serverGUI = new SpyServer();
		// the server thread starts at once: no need to press a button
		Thread serverThread = new Thread(serverGUI);
		serverThread.start();
	}

	public SpyServer() {
		setTitle("Brra Al-Salfa - Server");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		desktop.setPreferredSize(new Dimension(Desktop.WIDTH, Desktop.HEIGHT));
		setContentPane(desktop);
		desktop.taskbar().setLanguageText("");

		// every log line is also written to server_log.txt (append)
		try {
			logFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(LOG_FILE, true), "UTF-8"), true);
		} catch (IOException ioEx) {
			logFile = null;
		}

		// ---- 1. the CMD window (the log) ----
		cmd = new CmdWindow();

		// ---- 2. "Server status": port, state, topic, players ----
		XpWindow status = new XpWindow("Server status", Xp.pixelIcon(Xp.TV, 2));
		JPanel info = Xp.clear(new GridLayout(4, 2, 0, 4));
		stateLabel = statusValue("Waiting");
		topicLabel = statusValue("—");
		countLabel = statusValue("0 / " + MAX_PLAYERS);
		info.add(statusName("Port"));
		info.add(statusValue("" + PORT));
		info.add(statusName("State"));
		info.add(stateLabel);
		info.add(statusName("Topic"));
		info.add(topicLabel);
		info.add(statusName("Players"));
		info.add(countLabel);
		status.body().add(info, BorderLayout.NORTH);
		playerList = new JPanel();
		playerList.setLayout(new BoxLayout(playerList, BoxLayout.Y_AXIS));
		playerList.setBackground(Color.WHITE);
		JPanel listTop = new JPanel(new BorderLayout());
		listTop.setBackground(Color.WHITE);
		listTop.add(playerList, BorderLayout.NORTH);
		JPanel listWrap = Xp.clear(new BorderLayout());
		listWrap.setBorder(new EmptyBorder(10, 0, 0, 0));
		listWrap.add(Xp.scroll(listTop), BorderLayout.CENTER);      // scrolls when there are 8 players
		status.body().add(listWrap, BorderLayout.CENTER);
		status.setActive(false);

		// ---- 3. "Controls": 4 buttons ----
		XpWindow controls = new XpWindow("Controls", Xp.pixelIcon(Xp.SMILE, 2));
		JPanel grid = Xp.clear(new GridLayout(2, 2, 8, 8));
		roundButton = smallButton("Start round", XpButton.GREEN);
		kickButton = smallButton("Kick player", XpButton.NORMAL);
		saveButton = smallButton("Save log", XpButton.NORMAL);
		stopButton = smallButton("Stop server", XpButton.GO);
		grid.add(roundButton);
		grid.add(kickButton);
		grid.add(saveButton);
		grid.add(stopButton);
		controls.body().add(grid, BorderLayout.CENTER);
		JLabel note = Xp.label("Log is also written to " + LOG_FILE, Xp.MONO, 12f, Xp.GREY);
		note.setBorder(new EmptyBorder(10, 0, 0, 0));
		controls.body().add(note, BorderLayout.SOUTH);
		controls.setActive(false);

		desktop.place(controls, 940, 470, 320, 172);
		desktop.place(status, 940, 24, 320, 434);
		desktop.place(cmd, 128, 24, 790, 690);
		desktop.taskbar().setTabs("cmd.exe - SpyServer", "Server status", "Controls");

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private JLabel statusName(String text) {
		return Xp.label(text, Xp.MONO, 14f, new Color(0x555555));
	}

	private JLabel statusValue(String text) {
		JLabel l = Xp.label(text, Xp.PIXEL, 16f, Xp.INK);
		l.setHorizontalAlignment(JLabel.RIGHT);
		return l;
	}

	private XpButton smallButton(String text, int kind) {
		XpButton b = new XpButton(text, kind);
		b.setTextSize(16f);
		b.setMargin(new Insets(7, 6, 7, 6));
		b.addActionListener(this);
		return b;
	}

	// a line in the CMD (and in server_log.txt)
	public void log(Color tagColor, String tag, String rest) {
		cmd.log(tagColor, tag, rest);
		if (logFile != null) {
			logFile.println(new Date() + "  " + tag + "  " + rest);
		}
	}

	public void log(String text) {
		log(CmdWindow.TEXT, "", text);
	}

	// the status window: state, topic, count and the player list
	private void updateStatus() {
		// copy the values now (we are inside a synchronized method)
		final String state;
		if (phase.equals("DISCUSS")) {
			state = "Round " + roundNumber + " · " + XpParts.clock(secondsLeft);
		} else if (phase.equals("VOTE")) {
			state = "Voting · " + XpParts.clock(secondsLeft);
		} else if (phase.equals("GUESS")) {
			state = "Last guess · " + XpParts.clock(secondsLeft);
		} else if (roundNumber > 0) {
			state = gameOver ? "Game over" : "Results";
		} else {
			state = "Waiting";
		}
		final String topic = gameRunning ? category : "—";
		final String count = clients.size() + " / " + MAX_PLAYERS;
		final String[] names = new String[clients.size()];
		final Avatar[] faces = new Avatar[clients.size()];
		for (int i = 0; i < clients.size(); i++) {
			names[i] = clients.elementAt(i).name;
			faces[i] = clients.elementAt(i).avatar;
		}
		final String roundText = gameRunning ? "End round" : (roundNumber > 0 && !gameOver ? "Next round" : "Start round");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				stateLabel.setText(state);
				topicLabel.setText(topic);
				countLabel.setText(count);
				roundButton.setText(roundText);
				showPlayers(names, faces);
			}
		});
	}

	// one row per player: face, name, "host" or "online"; click = select
	private void showPlayers(String[] names, Avatar[] faces) {
		playerList.removeAll();
		for (int i = 0; i < names.length; i++) {
			final String name = names[i];
			boolean selected = name.equals(selectedPlayer);
			JPanel row = new JPanel(new BorderLayout(10, 0));
			row.setBackground(selected ? Xp.ORANGE_SOFT : Color.WHITE);
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Xp.LINE), new EmptyBorder(5, 8, 5, 8)));
			row.add(new Avatar.Portrait(faces[i], 30, selected), BorderLayout.WEST);
			row.add(Xp.label(name, Xp.PIXEL, 17f, Xp.INK), BorderLayout.CENTER);
			JLabel state = Xp.label(i == 0 ? "host" : "online", Xp.MONO, 12f, Xp.GREEN);
			if (i > 0) {
				state.setIcon(XpParts.dot(Xp.GREEN));
				state.setIconTextGap(6);
			}
			row.add(state, BorderLayout.EAST);
			Xp.onClick(row, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectedPlayer = name;
					updateStatusLater();
				}
			});
			playerList.add(row);
		}
		if (names.length == 0) {
			JLabel none = XpParts.hint("No players yet.");
			none.setBorder(new EmptyBorder(10, 10, 10, 10));
			playerList.add(none);
		}
		playerList.revalidate();
		playerList.repaint();
	}

	private synchronized void updateStatusLater() {
		updateStatus();
	}

	// =================================================================
	// [S4] CONTROLS (the 4 buttons of the "Controls" window)
	// =================================================================
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == roundButton) {
			roundButtonPressed();
		} else if (event.getSource() == kickButton) {
			kick(selectedPlayer);
		} else if (event.getSource() == saveButton) {
			saveLog();
		} else if (event.getSource() == stopButton) {
			stopServer();
		}
	}

	// Start round / End round / Next round (the server can always do it)
	private synchronized void roundButtonPressed() {
		if (gameRunning) {
			cancelRound("The server ended the round.");
		} else {
			startGame(null);                 // null = the server itself
		}
	}

	// removes the selected player from the game
	private synchronized void kick(String name) {
		if (name == null) {
			log(CmdWindow.YELLOW, "KICK", "Click a player in Server status first.");
			return;
		}
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			if (p.name.equals(name)) {
				log(CmdWindow.RED, "KICK", name + " was removed by the server.");
				p.send("KICKED");
				p.close();                    // its thread ends and calls removePlayer
				selectedPlayer = null;
				return;
			}
		}
	}

	// writes everything in the CMD to a new file
	private void saveLog() {
		String fileName = "server_log_saved.txt";
		try {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
			out.print(cmd.text());
			out.close();
			log(CmdWindow.GREEN, "SAVED", "The log was saved to " + fileName);
		} catch (IOException ioEx) {
			log(CmdWindow.RED, "ERROR", "Could not write " + fileName);
		}
	}

	private void stopServer() {
		broadcast("INFO:The server was stopped.");
		log(CmdWindow.RED, "STOP", "Server stopped.");
		try {
			serverSocket.close();
		} catch (Exception ex) {
			// already closed
		}
		// wait half a second so the last lines are written, then quit
		javax.swing.Timer quit = new javax.swing.Timer(500, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		quit.setRepeats(false);
		quit.start();
	}

	// =================================================================
	// [S5] SERVER THREAD: WAIT FOR PLAYERS
	// =================================================================
	public void run() {
		try {
			// create server socket
			serverSocket = new ServerSocket(PORT);
			log("Server started. Waiting for players...");
			log(CmdWindow.TEXT, "", "Clients stored in a Vector<ClientHandler>");
			synchronized (this) {
				loadCategories();
				updateStatus();
			}
			// start the voice relay in its own thread
			Thread voiceThread = new Thread(voice);
			voiceThread.start();

			do {
				// wait for a player
				Socket client = serverSocket.accept();

				// handler for this player
				ClientHandler handler = new ClientHandler(client, this);
				// start a thread for this player
				Thread clientThread = new Thread(handler);
				clientThread.start();

			} while (true);
		} catch (IOException ioEx) {
			log(CmdWindow.RED, "STOP", "Server stopped: " + ioEx.getMessage());
		}
	}

	// =================================================================
	// [S6] PLAYERS JOIN / LEAVE / CHAT
	//  "synchronized" = only ONE thread at a time can run these methods,
	//  so two players can never change the Vector at the same moment.
	// =================================================================

	// sends one message to EVERY connected player (and to the TV screens)
	public synchronized void broadcast(String message) {
		for (int i = 0; i < clients.size(); i++) {
			clients.elementAt(i).send(message);
		}
		for (int i = 0; i < displays.size(); i++) {
			displays.elementAt(i).send(message);
		}
	}

	// sends "PLAYERS:n1,n2,n3" to everyone (the brief's player list)
	public synchronized void sendPlayerList() {
		String list = "PLAYERS:";
		for (int i = 0; i < clients.size(); i++) {
			list = list + clients.elementAt(i).name;
			if (i < clients.size() - 1) {
				list = list + ",";
			}
		}
		broadcast(list);
	}

	// "Meryem=0-0-0,Nouri=1-1-1"  (used by AVATARS: and ONLINE:)
	private String avatarList() {
		String list = "";
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			list = list + p.name + "=" + p.avatar.code();
			if (i < clients.size() - 1) {
				list = list + ",";
			}
		}
		return list;
	}

	// the host is the FIRST player in the Vector
	private String hostName() {
		return clients.size() > 0 ? clients.firstElement().name : "";
	}

	private boolean isHost(ClientHandler c) {
		return clients.size() > 0 && clients.firstElement() == c;
	}

	public synchronized void sendHost() {
		broadcast("HOST:" + hostName());
	}

	// is this nickname already used? (not case sensitive: "Nouri" = "nouri")
	private boolean nameInUse(String name) {
		for (int i = 0; i < clients.size(); i++) {
			if (clients.elementAt(i).name.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	// "#WHO": the sign-in screen asks who is in the lobby (bonus)
	public synchronized void whoIsOnline(ClientHandler asker) {
		asker.send("ONLINE:" + avatarList());
	}

	// called when a player sent their nickname.
	// The check and the add are in the SAME synchronized method, so two
	// players with the same name can't both get in at the same moment.
	public synchronized boolean addPlayer(ClientHandler player) {
		if (nameInUse(player.name) || player.name.length() == 0) {
			log(CmdWindow.RED, "REFUSED", "\"" + player.name + "\" (name taken)");
			player.send("NAME_TAKEN");
			return false;
		}
		if (clients.size() >= MAX_PLAYERS) {
			log(CmdWindow.RED, "REFUSED", player.name + " (the game is full)");
			player.send("FULL");
			return false;
		}
		clients.add(player);                                   // add to the Vector
		log(CmdWindow.GREEN, "+ JOIN", pad(player.name, 9) + "from " + player.address
				+ "   (" + clients.size() + "/" + MAX_PLAYERS + ")");
		broadcast("INFO:" + player.name + " joined the game!");
		sendPlayerList();
		log(CmdWindow.TEXT, "", "PLAYERS:" + hostAndOthers() + "  -> broadcast");
		broadcast("AVATARS:" + avatarList());
		sendHost();
		loadProfile(player);
		player.send(categoriesMessage());
		player.send("PICKED:" + pickedCategory + "|");
		player.send("PHASE:LOBBY");
		sendScores();
		if (gameRunning) {
			player.send("INFO:A round is in progress. You will play in the next round.");
		}
		updateStatus();
		return true;
	}

	private String hostAndOthers() {
		String list = "";
		for (int i = 0; i < clients.size(); i++) {
			list = list + (i > 0 ? "," : "") + clients.elementAt(i).name;
		}
		return list;
	}

	// a name followed by spaces, so the log columns line up
	private String pad(String text, int size) {
		while (text.length() < size) {
			text = text + " ";
		}
		return text;
	}

	// AVATAR:<code> -> remember the character and tell everyone
	public synchronized void setAvatar(ClientHandler player, String code) {
		player.avatar = Avatar.parse(code);
		broadcast("AVATARS:" + avatarList());
		updateStatus();
	}

	// a TV screen joined: send it everything it needs to draw the game
	public synchronized void addDisplay(ClientHandler display) {
		displays.add(display);
		log(CmdWindow.CYAN, "+ TV", "A TV screen connected from " + display.address);
		display.send("PLAYERS:" + hostAndOthers());
		display.send("AVATARS:" + avatarList());
		display.send("HOST:" + hostName());
		display.send(categoriesMessage());
		display.send("PHASE:" + phase);
		if (gameRunning) {
			display.send("ROUND:" + roundNumber + "/" + ROUNDS);
			display.send("CATEGORY:" + category);
		}
	}

	// is this the exact name of a connected player? (used by the voice relay)
	public synchronized boolean isPlayerName(String name) {
		for (int i = 0; i < clients.size(); i++) {
			if (clients.elementAt(i).name.equals(name)) {
				return true;
			}
		}
		return false;
	}

	// called when a player closes the window or loses connection
	public synchronized void removePlayer(ClientHandler player) {
		if (displays.contains(player)) {
			displays.remove(player);
			log(CmdWindow.CYAN, "- TV", "A TV screen disconnected.");
			return;
		}
		if (!clients.contains(player)) {
			return;                         // never finished joining (or #WHO)
		}
		clients.remove(player);             // remove from the Vector
		voice.forget(player.name);          // stop relaying their voice
		log(CmdWindow.RED, "- LEAVE", player.name + "   (" + clients.size() + "/" + MAX_PLAYERS + ")");
		broadcast("INFO:" + player.name + " has left the game.");
		sendPlayerList();
		sendHost();                         // if the host left, the next player is host

		// what happens to the round when someone leaves?
		if (gameRunning && player.inRound) {
			if (player == outsider) {
				cancelRound("The outsider (" + player.name + ") left, so the round was cancelled. The word was: " + secretWord);
			} else if (countInRound() < MIN_PLAYERS) {
				cancelRound("Not enough players left, so the round was cancelled.");
			} else if (phase.equals("VOTE")) {
				broadcast("VOTES:" + countVotesCast() + "/" + countInRound());
				if (countVotesCast() == countInRound()) {
					countVotes();           // everyone left has already voted
				}
			}
		}
		updateStatus();
	}

	// a chat message: add the sender's name and send it to everyone
	public synchronized void chat(ClientHandler sender, String text) {
		// WORD FILTER: a player who knows the word may not type it
		if (gameRunning && sender.inRound && sender != outsider
				&& (text.toLowerCase().contains(secretWord.toLowerCase())
					|| (secretWordArabic.length() > 0 && text.contains(secretWordArabic)))) {
			sender.send("BLOCKED");
			log(CmdWindow.RED, "BLOCKED", sender.name + " typed the secret word");
			return;
		}
		log(CmdWindow.TEXT, "", "CHAT:" + sender.name + ": " + text);
		broadcast("CHAT:" + sender.name + ": " + text);
	}

	// counts the players who are playing in the current round
	private int countInRound() {
		int count = 0;
		for (int i = 0; i < clients.size(); i++) {
			if (clients.elementAt(i).inRound) {
				count++;
			}
		}
		return count;
	}

	// counts how many of those players have already voted
	private int countVotesCast() {
		int count = 0;
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			if (p.inRound && p.votedFor != null) {
				count++;
			}
		}
		return count;
	}

	// =================================================================
	// [S7] START A ROUND: PICK THE WORD AND THE OUTSIDER
	//  starter = the player who pressed Start round (null = the server)
	// =================================================================
	public synchronized void startGame(ClientHandler starter) {
		if (starter != null && !isHost(starter)) {
			starter.send("INFO:Only the host can start the round.");
			return;
		}
		if (gameRunning) {
			return;
		}
		if (clients.size() < MIN_PLAYERS) {
			String text = "You need at least " + MIN_PLAYERS + " players to start.";
			if (starter != null) {
				starter.send("INFO:" + text);
			}
			log(CmdWindow.YELLOW, "WAIT", text);
			return;
		}

		// after the last round, a new game starts: points back to 0
		if (gameOver || roundNumber >= ROUNDS) {
			roundNumber = 0;
			gameOver = false;
			for (int i = 0; i < clients.size(); i++) {
				ClientHandler p = clients.elementAt(i);
				p.points = 0;
				p.caught = 0;
				p.outsiderCount = 0;
			}
			log(CmdWindow.YELLOW, "NEW GAME", "points are back to 0");
		}

		// a. read the topic files again (the host may have added a file)
		loadCategories();

		// b. the chosen topic, or a random one
		if (pickedCategory.equals("RANDOM") || !bank.containsKey(pickedCategory)) {
			ArrayList<String> names = new ArrayList<String>(bank.keySet());
			category = names.get((int) (Math.random() * names.size()));
		} else {
			category = pickedCategory;
		}
		// a random word from that topic's file
		ArrayList<String[]> words = bank.get(category);
		String[] chosen = words.get((int) (Math.random() * words.size()));
		secretWord = chosen[0];
		secretWordArabic = chosen[1];

		// c. randomly choose ONE player to be the outsider
		int outsiderIndex = (int) (Math.random() * clients.size());
		outsider = clients.elementAt(outsiderIndex);

		roundNumber++;
		gameRunning = true;
		lastGuess = null;
		phase = "DISCUSS";

		log(CmdWindow.YELLOW, "START_GAME", "topic=" + category + "  round " + roundNumber + "/" + ROUNDS);
		broadcast("ROUND:" + roundNumber + "/" + ROUNDS);
		broadcast("CATEGORY:" + category);            // everyone (even the outsider) knows the topic

		// d. send the roles: the word to everyone except the outsider
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			p.inRound = true;              // this player is playing this round
			p.votedFor = null;             // nobody has voted yet
			if (p == outsider) {
				p.send("ROLE:You are Brra Al-Salfa!");
			} else {
				if (secretWordArabic.length() > 0) {
					p.send("WORD_AR:" + secretWordArabic);   // before ROLE, so the window has it
				}
				p.send("ROLE:The secret word is: " + secretWord);
			}
		}
		log(CmdWindow.TEXT, "", "ROLE sent to " + clients.size() + " players  (secret word hidden)");

		// e. the turn order (bonus): start from a random player
		broadcast("ORDER:" + makeQuestionOrder());
		broadcast("PHASE:DISCUSS");
		broadcast("INFO:Round " + roundNumber + " started! Topic: " + category + ".");

		// f. start the countdown thread for the questions
		log(CmdWindow.TEXT, "", "Timer started: " + XpParts.clock(DISCUSS_TIME));
		GameTimer timer = new GameTimer(this, DISCUSS_TIME, "DISCUSS", roundNumber);
		timer.start();
		secondsLeft = DISCUSS_TIME;
		updateStatus();
	}

	// the host picked the topic for the next round (or RANDOM)
	public synchronized void pickCategory(ClientHandler player, String name) {
		if (!isHost(player) || gameRunning) {
			return;
		}
		if (!name.equals("RANDOM") && !bank.containsKey(name)) {
			return;
		}
		pickedCategory = name;
		log(CmdWindow.TEXT, "", "PICK:" + name + "  (" + player.name + ")");
		broadcast("PICKED:" + name + "|" + player.name);
	}

	// "CATEGORIES:Food|أكل,Places|أماكن,..."
	private String categoriesMessage() {
		String message = "CATEGORIES:";
		int i = 0;
		for (String name : bank.keySet()) {
			String arabic = categoryArabic.get(name);
			message = message + name + "|" + (arabic == null ? "" : arabic);
			if (i < bank.size() - 1) {
				message = message + ",";
			}
			i++;
		}
		return message;
	}

	// reads every .txt file in the categories folder:
	//   the file name is the topic,  "#ar: أكل" is its Arabic name,
	//   then one word per line:  Machboos | مجبوس
	private void loadCategories() {
		LinkedHashMap<String, ArrayList<String[]>> newBank = new LinkedHashMap<String, ArrayList<String[]>>();
		HashMap<String, String> newArabic = new HashMap<String, String>();
		File[] files = new File(CATEGORY_FOLDER).listFiles();
		if (files != null) {
			Arrays.sort(files);                       // alphabetical order
			for (int f = 0; f < files.length; f++) {
				String fileName = files[f].getName();
				if (!fileName.toLowerCase().endsWith(".txt")) {
					continue;
				}
				// no , | = > in names: they would break the messages
				String name = fileName.substring(0, fileName.length() - 4).replaceAll("[,|=>]", " ").trim();
				ArrayList<String[]> words = new ArrayList<String[]>();
				try {
					Scanner in = new Scanner(files[f], "UTF-8");
					while (in.hasNextLine()) {
						String line = in.nextLine().trim();
						if (line.length() == 0) {
							continue;
						}
						if (line.startsWith("#")) {
							if (line.toLowerCase().startsWith("#ar:")) {
								newArabic.put(name, line.substring(4).trim());   // Arabic name of the topic
							}
							continue;                         // a comment
						}
						String[] parts = line.split("\\|");
						String english = parts[0].trim().replaceAll("[,=>]", " ");
						String arabic = parts.length > 1 ? parts[1].trim() : "";
						if (english.length() > 0) {
							words.add(new String[] { english, arabic });
						}
					}
					in.close();
				} catch (IOException ioEx) {
					log(CmdWindow.RED, "ERROR", "Could not read " + CATEGORY_FOLDER + "/" + fileName);
				}
				if (words.size() > 0) {
					newBank.put(name, words);
				}
			}
		}

		if (newBank.isEmpty()) {
			// no files: use the built-in word bank
			for (int i = 0; i < WORDS.length; i++) {
				if (!newBank.containsKey(CATEGORIES[i])) {
					newBank.put(CATEGORIES[i], new ArrayList<String[]>());
				}
				newBank.get(CATEGORIES[i]).add(new String[] { WORDS[i], "" });
			}
		}
		if (!newBank.keySet().equals(bank.keySet())) {
			log(CmdWindow.TEXT, "", "Topics loaded: " + newBank.keySet());
		}
		bank = newBank;
		categoryArabic = newArabic;
		if (!pickedCategory.equals("RANDOM") && !bank.containsKey(pickedCategory)) {
			pickedCategory = "RANDOM";             // that file was removed
		}
		broadcast(categoriesMessage());
	}

	// TURN ORDER (bonus): every player once, starting from a random player
	// (% makes the index go back to 0 after the last player)
	private String makeQuestionOrder() {
		int start = (int) (Math.random() * clients.size());
		String order = "";
		for (int i = 0; i < clients.size(); i++) {
			order = order + clients.elementAt((start + i) % clients.size()).name;
			if (i < clients.size() - 1) {
				order = order + ",";
			}
		}
		return order;
	}

	// the timer thread asks this: "is my phase still the current one?"
	public synchronized boolean isPhaseActive(String timerPhase, int timerRound) {
		return gameRunning && timerRound == roundNumber && phase.equals(timerPhase);
	}

	// the timer thread calls this every second
	public synchronized void tick(int seconds) {
		secondsLeft = seconds;
		String label = phase.equals("VOTE") ? "Voting" : phase.equals("GUESS") ? "Last guess" : "Time left";
		int total = phase.equals("VOTE") ? VOTE_TIME : phase.equals("GUESS") ? GUESS_TIME : DISCUSS_TIME;
		cmd.setTimer(label, seconds, total);
		updateStatus();
		final String clock = XpParts.clock(seconds);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				desktop.taskbar().setExtra(clock);
			}
		});
	}

	// the timer thread calls this when its countdown reaches 0
	public synchronized void timeUp(String timerPhase, int timerRound) {
		if (!isPhaseActive(timerPhase, timerRound)) {
			return;                                 // that phase already ended
		}
		if (timerPhase.equals("DISCUSS")) {
			openVoting();
		} else if (timerPhase.equals("VOTE")) {
			countVotes();
		} else if (timerPhase.equals("GUESS")) {
			finishRound(false, outsider.name + " ran out of time and could not guess the word.");
		}
	}

	// =================================================================
	// [S8] VOTING
	// =================================================================
	private void openVoting() {
		phase = "VOTE";
		log(CmdWindow.YELLOW, "TIME UP", "-> voting opened (" + XpParts.clock(VOTE_TIME) + ")");
		broadcast("PHASE:VOTE");
		broadcast("INFO:Time is up! Vote for the player you think is Brra Al-Salfa.");
		broadcast("VOTES:0/" + countInRound());
		GameTimer timer = new GameTimer(this, VOTE_TIME, "VOTE", roundNumber);
		timer.start();
		updateStatus();
	}

	// the server records each player's vote
	public synchronized void vote(ClientHandler voter, String name) {
		if (!phase.equals("VOTE") || !voter.inRound || voter.votedFor != null || name.equals(voter.name)) {
			return;                                  // not now, already voted, or for yourself
		}
		voter.votedFor = name;
		log(CmdWindow.TEXT, "", "VOTE:" + voter.name + "->" + name);
		broadcast("VOTED:" + voter.name);           // we do not say for who!
		broadcast("VOTES:" + countVotesCast() + "/" + countInRound());

		// once all players have voted, count the votes
		if (countVotesCast() == countInRound()) {
			countVotes();
		}
	}

	// =================================================================
	// [S9] COUNTING THE VOTES
	// =================================================================
	private void countVotes() {
		ClientHandler mostVoted = null;
		int maxVotes = 0;
		boolean tie = false;

		// the vote table in the CMD
		cmd.plain(CmdWindow.TEXT, "");
		cmd.plain(CmdWindow.WHITE, "  +----------+-------+");
		cmd.plain(CmdWindow.WHITE, "  | PLAYER   | VOTES |");
		cmd.plain(CmdWindow.WHITE, "  +----------+-------+");

		// for every player, count how many votes they received
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler candidate = clients.elementAt(i);
			if (!candidate.inRound) {
				continue;
			}
			int votes = 0;
			for (int j = 0; j < clients.size(); j++) {
				ClientHandler voter = clients.elementAt(j);
				if (voter.inRound && candidate.name.equals(voter.votedFor)) {
					votes++;
				}
			}
			cmd.plain(CmdWindow.TEXT, "  | " + pad(candidate.name, 8).substring(0, 8) + " |   " + votes + "   |");

			// remember the player with the most votes
			if (votes > maxVotes) {
				maxVotes = votes;
				mostVoted = candidate;
				tie = false;
			} else if (votes == maxVotes && votes > 0) {
				tie = true;                // two players have the same top number
			}
		}
		cmd.plain(CmdWindow.WHITE, "  +----------+-------+");
		cmd.plain(CmdWindow.TEXT, "");

		if (mostVoted == null) {
			finishRound(true, "Nobody voted, so the outsider escaped!");
		} else if (tie) {
			finishRound(true, "The vote was a tie, so the outsider escaped!");
		} else if (mostVoted == outsider) {
			openLastGuess();
		} else {
			finishRound(true, mostVoted.name + " was innocent! The outsider fooled everyone.");
		}
	}

	// =================================================================
	// [S10] LAST GUESS (bonus): the caught outsider picks 1 of 8 words
	// =================================================================
	private void openLastGuess() {
		phase = "GUESS";
		log(CmdWindow.ORANGE, "CAUGHT", outsider.name + " -> last guess (" + XpParts.clock(GUESS_TIME) + ")");
		broadcast("CAUGHT:" + outsider.name);
		broadcast("GUESS_OPTIONS:" + makeGuessOptions());
		broadcast("PHASE:GUESS");
		outsider.send("GUESS_TURN");
		GameTimer timer = new GameTimer(this, GUESS_TIME, "GUESS", roundNumber);
		timer.start();
		updateStatus();
	}

	// the secret word + 7 other words (same topic first), mixed
	private String makeGuessOptions() {
		ArrayList<String> options = new ArrayList<String>();
		options.add(secretWord);
		ArrayList<String[]> same = new ArrayList<String[]>(bank.get(category));
		Collections.shuffle(same);
		for (int i = 0; i < same.size() && options.size() < 8; i++) {
			if (!options.contains(same.get(i)[0])) {
				options.add(same.get(i)[0]);
			}
		}
		// a small topic: take words from the other topics too
		for (String other : bank.keySet()) {
			ArrayList<String[]> words = bank.get(other);
			for (int i = 0; i < words.size() && options.size() < 8; i++) {
				if (!options.contains(words.get(i)[0])) {
					options.add(words.get(i)[0]);
				}
			}
		}
		Collections.shuffle(options);
		String list = "";
		for (int i = 0; i < options.size(); i++) {
			list = list + (i > 0 ? "," : "") + options.get(i);
		}
		return list;
	}

	public synchronized void guess(ClientHandler player, String word) {
		if (!phase.equals("GUESS") || player != outsider) {
			return;                               // only the caught outsider can guess
		}
		lastGuess = word.trim();
		log(CmdWindow.TEXT, "", "GUESS:" + player.name + " -> " + lastGuess);
		broadcast("GUESSED:" + lastGuess);
		if (lastGuess.equalsIgnoreCase(secretWord)) {
			finishRound(true, player.name + " guessed the word!");
		} else {
			finishRound(false, "Wrong guess! " + player.name + " could not escape.");
		}
	}

	// =================================================================
	// [S11] END OF ROUND + POINTS
	//  outsiderWins = true if the outsider escaped or guessed the word
	// =================================================================
	private void finishRound(boolean outsiderWins, String reason) {
		boolean caught = phase.equals("GUESS");          // the vote found the outsider
		String votes = "";
		String points = "";
		String pointsLog = "";

		// give the points of this round
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			if (!p.inRound) {
				continue;
			}
			int earned = 0;
			if (p == outsider) {
				p.outsiderCount++;
				if (!caught) {
					earned = ESCAPE_POINTS;                // not caught: +2
				} else if (outsiderWins) {
					earned = GUESS_POINTS;                 // caught, but guessed the word: +1
				}
			} else if (outsider.name.equals(p.votedFor)) {
				earned = CATCH_POINTS;                     // voted for the outsider: +1
				p.caught++;
			}
			p.points = p.points + earned;
			votes = votes + (votes.length() > 0 ? "," : "") + p.name + ">" + (p.votedFor == null ? "" : p.votedFor);
			points = points + (points.length() > 0 ? "," : "") + p.name + "=" + earned;
			if (earned > 0) {
				pointsLog = pointsLog + p.name + " +" + earned + "  ";
			}

			// the saved profile (players.dat)
			boolean won = (p == outsider) ? outsiderWins : !outsiderWins;
			p.games++;
			if (won) {
				p.wins++;
			}
			p.totalPoints = p.totalPoints + earned;
			saveProfile(p);
		}

		// the result: who the outsider was, the word, and who won
		String winner = outsiderWins ? "Brra Al-Salfa (" + outsider.name + ")" : "The players";
		String result = "Outsider: " + outsider.name + " | Word: " + secretWord;
		if (secretWordArabic.length() > 0) {
			result = result + " | WordAr: " + secretWordArabic;
		}
		result = result + " | Winner: " + winner + " | " + reason;

		broadcast("VOTE_LIST:" + votes);
		broadcast("ROUND_POINTS:" + points);
		broadcast("RESULT:" + result);
		log(CmdWindow.ORANGE, "RESULT", outsider.name + " was Brra Al-Salfa!  " + (outsiderWins ? "Outsider wins." : "Players win."));
		log(CmdWindow.TEXT, "", "Score: " + (pointsLog.length() > 0 ? pointsLog : "nobody scored"));
		logHistory(result);                               // write to history.txt

		if (roundNumber >= ROUNDS) {
			gameOver = true;
		}
		sendScores();
		if (gameOver) {
			String best = clients.size() > 0 ? sortedPlayers()[0].name : "";
			broadcast("GAME_OVER:" + best);
			log(CmdWindow.YELLOW, "GAME_OVER", best + " wins the game!");
		}
		endRound();
	}

	// stops a round without points (someone left, or the server ended it)
	private void cancelRound(String reason) {
		broadcast("INFO:" + reason);
		log(CmdWindow.RED, "CANCEL", reason);
		roundNumber--;                                    // this round does not count
		endRound();
	}

	// puts the game back in the lobby
	private void endRound() {
		gameRunning = false;
		phase = "LOBBY";
		for (int i = 0; i < clients.size(); i++) {
			clients.elementAt(i).inRound = false;
			clients.elementAt(i).votedFor = null;
		}
		broadcast("PHASE:LOBBY");
		cmd.setTimer("", 0, 0);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				desktop.taskbar().setExtra("");
			}
		});
		updateStatus();
	}

	// =================================================================
	// [S12] SCORES  (sorted from the most points down: bubble sort)
	// =================================================================
	private ClientHandler[] sortedPlayers() {
		ClientHandler[] sorted = new ClientHandler[clients.size()];
		for (int i = 0; i < sorted.length; i++) {
			sorted[i] = clients.elementAt(i);
		}
		for (int i = 0; i < sorted.length - 1; i++) {
			for (int j = 0; j < sorted.length - 1 - i; j++) {
				if (sorted[j].points < sorted[j + 1].points) {
					ClientHandler swap = sorted[j];
					sorted[j] = sorted[j + 1];
					sorted[j + 1] = swap;
				}
			}
		}
		return sorted;
	}

	// "SCORES:Meryem=5=3=1,Sara=4=2=1"   name=points=caught=wasOutsider
	private void sendScores() {
		ClientHandler[] sorted = sortedPlayers();
		String message = "SCORES:";
		for (int i = 0; i < sorted.length; i++) {
			ClientHandler p = sorted[i];
			message = message + (i > 0 ? "," : "") + p.name + "=" + p.points + "=" + p.caught + "=" + p.outsiderCount;
		}
		broadcast(message);
	}

	// =================================================================
	// [S13] PLAYER PROFILES  (RandomAccessFile, like RanFile1)
	//  Every record is 42 bytes:
	//    name   -> 15 chars x 2 bytes = 30 bytes
	//    games  -> int = 4 bytes
	//    wins   -> int = 4 bytes
	//    points -> int = 4 bytes   (all the points ever earned)
	// =================================================================

	// finds the player's record, or creates a new one. returns true if found.
	private boolean loadProfile(ClientHandler player) {
		boolean found = false;
		try {
			RandomAccessFile file = new RandomAccessFile(PROFILE_FILE, "rw");
			long numRecords = file.length() / REC_SIZE;
			String shortName = player.name;
			if (shortName.length() > NAME_SIZE) {
				shortName = shortName.substring(0, NAME_SIZE);   // only 15 chars are saved
			}

			// look at every record until we find this name
			for (int i = 0; i < numRecords; i++) {
				file.seek(i * REC_SIZE);
				String savedName = readString(file, NAME_SIZE).trim();
				if (savedName.equals(shortName)) {
					player.recordNum = i;
					player.games = file.readInt();
					player.wins = file.readInt();
					player.totalPoints = file.readInt();
					found = true;
					break;
				}
			}
			file.close();

			// a new player: their record goes at the end of the file
			if (!found) {
				player.recordNum = numRecords;
				saveProfile(player);
			} else {
				player.send("INFO:Welcome back, " + player.name + "! " + player.games + " games, "
						+ player.wins + " wins so far.");
			}
		} catch (IOException ioEx) {
			log(CmdWindow.RED, "ERROR", "Could not read " + PROFILE_FILE);
		}
		return found;
	}

	// writes the player's record at its position in the file
	private void saveProfile(ClientHandler player) {
		try {
			RandomAccessFile file = new RandomAccessFile(PROFILE_FILE, "rw");
			// find starting byte for this record, then position the file pointer
			file.seek(player.recordNum * REC_SIZE);
			writeString(file, player.name, NAME_SIZE);
			file.writeInt(player.games);
			file.writeInt(player.wins);
			file.writeInt(player.totalPoints);
			file.close();
		} catch (IOException ioEx) {
			log(CmdWindow.RED, "ERROR", "Could not save " + PROFILE_FILE);
		}
	}

	// same as RanFile1: write a fixed size string (pad with spaces)
	private void writeString(RandomAccessFile file, String text, int fixedSize) throws IOException {
		int size = text.length();
		if (size <= fixedSize) {
			file.writeChars(text);
			// now 'pad out' the field with spaces...
			for (int i = size; i < fixedSize; i++) {
				file.writeChar(' ');
			}
		} else {
			file.writeChars(text.substring(0, fixedSize));   // string is too long!
		}
	}

	// same as RanFile1: read a fixed size string
	private String readString(RandomAccessFile file, int fixedSize) throws IOException {
		String value = "";
		for (int i = 0; i < fixedSize; i++) {
			value += file.readChar();
		}
		return value;
	}

	// =================================================================
	// [S14] ROUND HISTORY  (text file, like FileTest_Append_Awad)
	// =================================================================
	private void logHistory(String result) {
		try {
			// true = APPEND to the file instead of overwriting it
			FileWriter myFile = new FileWriter(HISTORY_FILE, true);
			PrintWriter output = new PrintWriter(myFile);
			output.println(new Date() + "  |  Round " + roundNumber + "  |  " + result);
			output.close();
		} catch (IOException ioEx) {
			log(CmdWindow.RED, "ERROR", "Could not write " + HISTORY_FILE);
		}
	}
}


// =====================================================================
// ClientHandler: ONE thread for ONE player (like in MultiEchoServer)
// =====================================================================
class ClientHandler implements Runnable {

	private Socket client;
	private Scanner input;
	private PrintWriter output;
	private SpyServer server;

	// information about this player
	// (not private, so the SpyServer class in this file can read/change them)
	String name = "";
	String address = "";
	Avatar avatar = Avatar.preset(0);
	boolean inRound = false;      // is this player in the current round?
	String votedFor = null;       // who this player voted for (null = not yet)
	int points = 0;               // points in this game (3 rounds)
	int caught = 0;               // times this player voted for the outsider
	int outsiderCount = 0;        // times this player was the outsider
	long recordNum = 0;           // position of the record in players.dat
	int games = 0, wins = 0, totalPoints = 0;   // saved profile

	public ClientHandler(Socket socket, SpyServer gui) {
		client = socket;
		server = gui;
		address = client.getInetAddress().getHostAddress();
		try {
			// set up input and output streams (UTF-8 for the Arabic words)
			input = new Scanner(client.getInputStream(), "UTF-8");
			output = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true);
		} catch (IOException ioEx) {
			server.log("Error setting up client streams.");
		}
	}

	// sends one line to this player
	public void send(String message) {
		output.println(message);
	}

	// closes the connection (used by Kick)
	public void close() {
		try {
			client.close();
		} catch (IOException ioEx) {
			// already closed
		}
	}

	public void run() {
		String received = "";
		try {
			// the FIRST message from a client is the nickname
			name = input.nextLine().trim().replaceAll("[,=>|:]", "");

			if (name.equals("#WHO")) {
				// the sign-in screen only asks who is online
				server.whoIsOnline(this);
				return;                      // "finally" below closes the socket
			}

			if (name.equals("#HOST")) {
				// a TV screen: it only watches
				server.addDisplay(this);
				do {
					input.nextLine();        // a TV sends nothing; wait until it closes
				} while (true);
			}

			avatar = Avatar.forName(name);
			if (!server.addPlayer(this)) {
				return;                      // name taken or game full
			}

			do {
				// receive the next message from this player
				received = input.nextLine();

				// look at the prefix to know what kind of message it is
				if (received.startsWith("CHAT:")) {
					server.chat(this, received.substring(5));          // text after "CHAT:"
				} else if (received.equals("START_GAME")) {
					server.startGame(this);
				} else if (received.startsWith("VOTE:")) {
					server.vote(this, received.substring(5));          // name after "VOTE:"
				} else if (received.startsWith("GUESS:")) {
					server.guess(this, received.substring(6));         // word after "GUESS:"
				} else if (received.startsWith("PICK:")) {
					server.pickCategory(this, received.substring(5));  // topic after "PICK:"
				} else if (received.startsWith("AVATAR:")) {
					server.setAvatar(this, received.substring(7));     // e.g. "4-2-0"
				}
			} while (true);
		} catch (Exception ex) {
			// NoSuchElementException: the player closed the window (empty catch, as the brief says)
		} finally {
			server.removePlayer(this);     // remove from the Vector and tell everyone
			close();
		}
	}
}

// =====================================================================
// GameTimer: a thread that counts down and tells everyone the seconds
// (uses sleep, like the thread examples in Chapter 3)
// =====================================================================
class GameTimer extends Thread {

	private SpyServer server;
	private int seconds;          // how long to count
	private String phase;         // which phase this timer is for
	private int round;            // which round this timer is for

	public GameTimer(SpyServer theServer, int theSeconds, String thePhase, int theRound) {
		server = theServer;
		seconds = theSeconds;
		phase = thePhase;
		round = theRound;
	}

	public void run() {
		try {
			for (int s = seconds; s > 0; s--) {
				// stop if the phase already ended (e.g. everyone voted early)
				if (!server.isPhaseActive(phase, round)) {
					return;
				}
				server.broadcast("TIMER:" + s);
				server.tick(s);
				sleep(1000);          // wait 1 second
			}
			if (server.isPhaseActive(phase, round)) {
				server.broadcast("TIMER:0");
				server.timeUp(phase, round);
			}
		} catch (InterruptedException e) {
			// the thread was interrupted, nothing to do
		}
	}
}

// =====================================================================
// VoiceRelay: push-to-talk voice over UDP (DatagramSocket)
//  Players send small packets of microphone sound to the server, and the
//  server sends each packet to every OTHER player. UDP is used because a
//  late sound packet is useless: it is better to skip it than to wait.
//
//  packet = [type byte][name (writeUTF)][audio bytes]
//    HELLO (1): "this address belongs to <name>"  (must be a real player)
//    AUDIO (2): sound from the player at this address
//    STOP  (3): the player let go of the talk button
// =====================================================================
class VoiceRelay implements Runnable {

	static final byte HELLO = 1, AUDIO = 2, STOP = 3;

	private SpyServer server;
	private DatagramSocket socket;
	// which address belongs to which player
	private HashMap<SocketAddress, String> speakers = new HashMap<SocketAddress, String>();

	public VoiceRelay(SpyServer theServer) {
		server = theServer;
	}

	public void run() {
		try {
			socket = new DatagramSocket(SpyServer.VOICE_PORT);
			server.log("Voice relay started on UDP port " + SpyServer.VOICE_PORT + ".");
			byte[] buffer = new byte[4096];
			do {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				handle(packet);
			} while (true);
		} catch (IOException ioEx) {
			server.log("Voice relay stopped: " + ioEx.getMessage());
		}
	}

	private void handle(DatagramPacket packet) {
		try {
			DataInputStream in = new DataInputStream(
					new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
			byte type = in.readByte();
			String name = in.readUTF();
			SocketAddress from = packet.getSocketAddress();

			if (type == HELLO) {
				// only a connected player may register (no strangers on the line)
				if (server.isPlayerName(name)) {
					register(from, name);
				}
				return;
			}

			// AUDIO or STOP: use the REGISTERED name, not the one in the packet
			String speaker = nameOf(from);
			if (speaker == null) {
				return;
			}
			byte[] audio = new byte[in.available()];
			in.readFully(audio);

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(type);
			out.writeUTF(speaker);
			out.write(audio);
			byte[] data = bytes.toByteArray();

			// send it to everyone except the speaker
			ArrayList<SocketAddress> others = othersThan(from);
			for (int i = 0; i < others.size(); i++) {
				socket.send(new DatagramPacket(data, data.length, others.get(i)));
			}
		} catch (IOException ioEx) {
			// a broken packet: ignore it
		}
	}

	private synchronized void register(SocketAddress address, String name) {
		speakers.put(address, name);
	}

	private synchronized String nameOf(SocketAddress address) {
		return speakers.get(address);
	}

	private synchronized ArrayList<SocketAddress> othersThan(SocketAddress address) {
		ArrayList<SocketAddress> list = new ArrayList<SocketAddress>();
		for (SocketAddress a : speakers.keySet()) {
			if (!a.equals(address)) {
				list.add(a);
			}
		}
		return list;
	}

	// a player left: forget their address
	public synchronized void forget(String name) {
		speakers.values().removeAll(Collections.singleton(name));
	}
}
