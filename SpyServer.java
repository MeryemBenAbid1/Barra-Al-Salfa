/* =====================================================================
 *  SpyServer.java  -  "Brra Al-Salfa" (برا السالفة) Game Server
 *  CpE 458 - Network Programming - Dr. Mohamad Awad
 * ---------------------------------------------------------------------
 *  BIG PICTURE
 *   1. The server window opens. We press "Start Server".
 *   2. A server thread waits for players with serverSocket.accept().
 *   3. Every player that connects gets its OWN ClientHandler thread.
 *   4. All the handlers are kept in a Vector called "clients".
 *   5. Every line starts with a prefix (CHAT:, VOTE:, ROLE: ...)
 *      so the server and the client know what kind of message it is.
 *
 *  THE 3 CLASSES IN THIS FILE
 *   SpyServer     -> the window + the game rules (one object)
 *   ClientHandler -> one thread per connected player
 *   GameTimer     -> a thread that counts down the seconds
 *
 *  SECTION MAP (search for the tag, e.g. "[S7]")
 *   [S1] Settings, word bank, XP points     [S8]  Voting
 *   [S2] Variables                          [S9]  Counting the votes
 *   [S3] main + server window               [S10] Last chance guess
 *   [S4] Buttons (actionPerformed)          [S11] End of round + XP
 *   [S5] Server thread (accept loop)        [S12] Leaderboard
 *   [S6] Players join / leave / chat        [S13] Player profiles (RandomAccessFile)
 *   [S7] Start game (word + outsider)       [S14] Round history (text file)
 *
 *  MESSAGES  (Client -> Server)
 *   <nickname>       first line = the player's name
 *   CHAT:<text>      a chat message
 *   START_GAME       "Start Game" was clicked
 *   VOTE:<name>      vote for <name>
 *   GUESS:<word>     the caught outsider guesses the word (bonus)
 *
 *  MESSAGES  (Server -> Client)
 *   CHAT:<name>: <text>     chat line to show
 *   INFO:<text>             announcement from the server (bonus)
 *   PLAYERS:<n1>,<n2>,...   updated list of players (for the drop-down)
 *   ROLE:<text>             the secret word, or "You are Brra Al-Salfa!"
 *   CATEGORY:<text>         the word's category, e.g. Food (bonus)
 *   PHASE:<name>            LOBBY / DISCUSS / VOTE / GUESS (bonus)
 *   TIMER:<seconds>         seconds left in this phase (bonus)
 *   VOTES:<x>/<y>           x of y players have voted (bonus)
 *   GUESS_TURN              tells the caught outsider to guess (bonus)
 *   RESULT:<text>           who the outsider was, the word, who won
 *   XP:<points>,<WIN/LOSE>  points this player earned this round (bonus)
 *   PROFILE:<rank>,<level>,<games>,<wins>,<xp>   saved stats (bonus)
 *   SCORES:<line1>,<line2>,...                   leaderboard (bonus)
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class SpyServer extends JFrame implements ActionListener, Runnable {

	// =================================================================
	// [S1] SETTINGS, WORD BANK AND XP POINTS
	// =================================================================
	private static final int PORT = 1202;            // port the server listens on
	private static final int MIN_PLAYERS = 2;        // need at least 2 players to start

	// how long each phase lasts (in seconds)
	private static final int DISCUSS_TIME = 90;      // questions & answers
	private static final int VOTE_TIME = 30;         // voting
	private static final int GUESS_TIME = 20;        // outsider's last chance guess

	// word bank: WORDS[i] belongs to the category CATEGORIES[i]
	private static final String[] WORDS = {
		"Shawarma", "Machboos", "Falafel", "Kunafa", "Pizza", "Karak Tea",
		"Kuwait Towers", "Airport", "Hospital", "University", "Cinema", "Beach",
		"Doctor", "Pilot", "Teacher", "Chef", "Firefighter", "Engineer",
		"Camel", "Falcon", "Cat", "Shark", "Horse", "Penguin",
		"Football", "Swimming", "Chess", "Bowling", "Diwaniya", "Wedding" };
	private static final String[] CATEGORIES = {
		"Food", "Food", "Food", "Food", "Food", "Food",
		"Place", "Place", "Place", "Place", "Place", "Place",
		"Job", "Job", "Job", "Job", "Job", "Job",
		"Animal", "Animal", "Animal", "Animal", "Animal", "Animal",
		"Activity", "Activity", "Activity", "Activity", "Event", "Event" };

	// XP (experience points) given at the end of every round
	private static final int PLAY_XP = 10;           // everyone who played
	private static final int WIN_XP = 100;           // insiders win
	private static final int SHARP_EYE_XP = 50;      // insider voted for the real outsider
	private static final int ESCAPE_XP = 200;        // outsider was not caught
	private static final int STEAL_XP = 150;         // outsider caught, but guessed the word

	// files saved next to the server
	private static final String PROFILE_FILE = "players.dat";   // RandomAccessFile
	private static final String HISTORY_FILE = "history.txt";   // normal text file
	private static final int NAME_SIZE = 15;                    // 15 chars per name
	private static final int REC_SIZE = 42;                     // 30 (name) + 4 + 4 + 4 bytes

	// colors of the server window (same theme as the client)
	private static final Color BG = new Color(14, 20, 38);         // deep navy
	private static final Color PANEL = new Color(24, 33, 58);      // lighter navy
	private static final Color GOLD = new Color(212, 175, 55);     // gold accent
	private static final Color CREAM = new Color(245, 236, 215);   // text color
	private static final Color MUTED = new Color(150, 160, 190);   // grey-blue text

	// =================================================================
	// [S2] VARIABLES
	// =================================================================
	private ServerSocket serverSocket;
	private JTextArea display;                 // server log
	private JButton startButton, historyButton, exitButton;
	private JPanel topPanel, buttonPanel;
	private JLabel titleLabel, ipLabel, statusLabel;

	// the list of connected players (the research part: Vector)
	private Vector<ClientHandler> clients = new Vector<ClientHandler>();

	// the state of the game
	private boolean gameRunning = false;
	private String phase = "LOBBY";            // LOBBY, DISCUSS, VOTE or GUESS
	private int roundNumber = 0;
	private String secretWord = "";
	private String category = "";
	private ClientHandler outsider = null;     // the player who is Brra Al-Salfa

	// =================================================================
	// [S3] MAIN + SERVER WINDOW
	// =================================================================
	public static void main(String[] args) {
		SpyServer serverGUI = new SpyServer();
	}

	public SpyServer() {
		setTitle("Brra Al-Salfa - Server");
		setSize(650, 520);
		setLayout(new BorderLayout());
		getContentPane().setBackground(BG);

		// ---- top: title + IP address + status ----
		topPanel = new JPanel(new GridLayout(3, 1));
		topPanel.setBackground(PANEL);
		topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		titleLabel = new JLabel("BRRA AL-SALFA  |  GAME SERVER");
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
		titleLabel.setForeground(GOLD);
		topPanel.add(titleLabel);

		// show this computer's IP so players know where to connect (like MyIPAddress)
		try {
			InetAddress address = InetAddress.getLocalHost();
			ipLabel = new JLabel("Server IP: " + address.getHostAddress() + "    Port: " + PORT);
		} catch (UnknownHostException uhEx) {
			ipLabel = new JLabel("Server IP: unknown    Port: " + PORT);
		}
		ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		ipLabel.setForeground(CREAM);
		topPanel.add(ipLabel);

		statusLabel = new JLabel("Server is off. Press Start Server.");
		statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		statusLabel.setForeground(MUTED);
		topPanel.add(statusLabel);
		add(topPanel, BorderLayout.NORTH);

		// ---- center: the log ----
		display = new JTextArea();
		display.setEditable(false);
		display.setLineWrap(true);
		display.setWrapStyleWord(true);
		display.setBackground(BG);
		display.setForeground(CREAM);
		display.setFont(new Font("Consolas", Font.PLAIN, 13));
		display.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		JScrollPane scroll = new JScrollPane(display);
		scroll.setBorder(null);
		add(scroll, BorderLayout.CENTER);

		// ---- bottom: buttons ----
		buttonPanel = new JPanel();
		buttonPanel.setBackground(PANEL);
		startButton = makeButton("Start Server", GOLD, BG);
		historyButton = makeButton("Round History", PANEL, CREAM);
		exitButton = makeButton("Exit", PANEL, CREAM);
		buttonPanel.add(startButton);
		buttonPanel.add(historyButton);
		buttonPanel.add(exitButton);
		add(buttonPanel, BorderLayout.SOUTH);

		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	// creates a styled button and makes this window listen to it
	private JButton makeButton(String text, Color background, Color textColor) {
		JButton button = new JButton(text);
		button.setBackground(background);
		button.setForeground(textColor);
		button.setFont(new Font("Segoe UI", Font.BOLD, 13));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(GOLD, 1),
				BorderFactory.createEmptyBorder(8, 16, 8, 16)));
		button.addActionListener(this);
		return button;
	}

	// =================================================================
	// [S4] BUTTONS
	// =================================================================
	public void actionPerformed(ActionEvent event) {
		// if the start button is pressed
		if (event.getSource() == startButton) {
			startButton.setEnabled(false);
			showMessage("Starting server on port " + PORT + "...");
			// start the server in another thread so the window does not freeze
			Thread serverThread = new Thread(this);
			serverThread.start();
		}
		// if the history button is pressed, show the saved rounds
		else if (event.getSource() == historyButton) {
			showHistory();
		}
		// if exit button is pressed
		else if (event.getSource() == exitButton) {
			try {
				if (serverSocket != null) {
					serverSocket.close();
				}
			} catch (IOException ioEx) {
				showMessage("Unable to close server socket.");
			}
			System.exit(0);
		}
	}

	// =================================================================
	// [S5] SERVER THREAD: WAIT FOR PLAYERS
	// =================================================================
	public void run() {
		try {
			// create server socket
			serverSocket = new ServerSocket(PORT);
			showMessage("Server started. Waiting for players...");
			updateStatus();

			do {
				// wait for a player
				Socket client = serverSocket.accept();
				showMessage("New connection from " + client.getInetAddress().getHostAddress());

				// handler for this player
				ClientHandler handler = new ClientHandler(client, this);
				// start a thread for this player
				Thread clientThread = new Thread(handler);
				clientThread.start();

			} while (true);
		} catch (IOException ioEx) {
			showMessage("Server stopped.");
		}
	}

	// to show messages on the server window
	public void showMessage(String message) {
		display.append(message + "\n");
		display.setCaretPosition(display.getDocument().getLength());   // auto-scroll down
	}

	// shows the number of players and the phase at the top of the window
	private void updateStatus() {
		String text = "Players online: " + clients.size() + "    Phase: " + phase;
		if (gameRunning) {
			text = text + "    Round " + roundNumber + "    Word: " + secretWord;
		}
		statusLabel.setText(text);
	}

	// =================================================================
	// [S6] PLAYERS JOIN / LEAVE / CHAT
	//  "synchronized" = only ONE thread at a time can run these methods,
	//  so two players can never change the Vector at the same moment.
	// =================================================================

	// sends one message to EVERY connected player
	public synchronized void broadcast(String message) {
		for (int i = 0; i < clients.size(); i++) {
			clients.elementAt(i).send(message);
		}
	}

	// sends "PLAYERS:n1,n2,n3" to everyone (used for the voting drop-down)
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

	// called when a player sent their nickname
	public synchronized void addPlayer(ClientHandler player) {
		clients.add(player);                                   // add to the Vector
		showMessage(">> " + player.name + " joined the game.");
		broadcast("INFO:" + player.name + " joined the game!");
		sendPlayerList();

		// load (or create) the saved profile of this player
		boolean returning = loadProfile(player);
		if (returning) {
			player.send("INFO:Welcome back, " + player.name + "! You are a "
					+ getRank(player.xp) + " with " + player.wins + " wins.");
		} else {
			player.send("INFO:Welcome to Brra Al-Salfa, " + player.name + "! A new profile was created for you.");
		}
		sendProfile(player);
		sendLeaderboard();

		// if a round is already running, this player waits for the next one
		if (gameRunning) {
			player.send("INFO:A round is in progress. You will join the next round.");
			player.send("PHASE:" + phase);
		}
		updateStatus();
	}

	// called when a player closes the window or loses connection
	public synchronized void removePlayer(ClientHandler player) {
		if (!clients.contains(player)) {
			return;                         // never finished joining
		}
		clients.remove(player);             // remove from the Vector
		showMessage("<< " + player.name + " left the game.");
		broadcast("INFO:" + player.name + " has left the game.");
		sendPlayerList();
		sendLeaderboard();

		// what happens to the round when someone leaves?
		if (gameRunning) {
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
		// WORD FILTER (bonus): an insider is not allowed to type the secret word
		if (gameRunning && sender.inRound && sender != outsider
				&& text.toLowerCase().contains(secretWord.toLowerCase())) {
			sender.send("INFO:Careful! You cannot say the secret word. Your message was blocked.");
			showMessage("[blocked] " + sender.name + ": " + text);
			return;
		}
		showMessage(sender.name + ": " + text);
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
	// [S7] START GAME: PICK THE WORD AND THE OUTSIDER
	// =================================================================
	public synchronized void startGame(ClientHandler starter) {
		if (gameRunning) {
			starter.send("INFO:A round is already running!");
			return;
		}
		if (clients.size() < MIN_PLAYERS) {
			starter.send("INFO:You need at least " + MIN_PLAYERS + " players to start.");
			return;
		}

		// a. pick a random secret word from the word bank
		int wordIndex = (int) (Math.random() * WORDS.length);
		secretWord = WORDS[wordIndex];
		category = CATEGORIES[wordIndex];

		// b. randomly choose ONE player to be the outsider
		int outsiderIndex = (int) (Math.random() * clients.size());
		outsider = clients.elementAt(outsiderIndex);

		roundNumber++;
		gameRunning = true;
		phase = "DISCUSS";

		showMessage("\n===== ROUND " + roundNumber + " started by " + starter.name + " =====");
		showMessage("Secret word: " + secretWord + "   (Category: " + category + ")");
		showMessage("Outsider: " + outsider.name);

		// c. send the roles: the word to everyone except the outsider
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			p.inRound = true;              // this player is playing this round
			p.votedFor = null;             // nobody has voted yet
			if (p == outsider) {
				p.send("ROLE:You are Brra Al-Salfa!");
			} else {
				p.send("ROLE:The secret word is: " + secretWord);
			}
			p.send("CATEGORY:" + category);   // everyone (even the outsider) gets the category
		}

		broadcast("PHASE:DISCUSS");
		broadcast("INFO:Round " + roundNumber + " started! Category: " + category
				+ ". You have " + DISCUSS_TIME + " seconds to ask questions.");
		broadcast("INFO:Question order: " + makeQuestionOrder());

		// start the countdown thread for the discussion phase
		GameTimer timer = new GameTimer(this, DISCUSS_TIME, "DISCUSS", roundNumber);
		timer.start();
		updateStatus();
	}

	// QUESTION ORDER (bonus): starting from a random player, each one asks the next
	private String makeQuestionOrder() {
		int start = (int) (Math.random() * clients.size());
		String order = "";
		for (int i = 0; i < clients.size(); i++) {
			// % (remainder) makes the index go back to 0 after the last player
			String asker = clients.elementAt((start + i) % clients.size()).name;
			String answerer = clients.elementAt((start + i + 1) % clients.size()).name;
			order = order + asker + " asks " + answerer;
			if (i < clients.size() - 1) {
				order = order + ",  ";
			}
		}
		return order;
	}

	// the timer thread asks this: "is my phase still the current one?"
	public synchronized boolean isPhaseActive(String timerPhase, int timerRound) {
		return gameRunning && timerRound == roundNumber && phase.equals(timerPhase);
	}

	// the timer thread calls this when its countdown reaches 0
	public synchronized void timeUp(String timerPhase, int timerRound) {
		if (!isPhaseActive(timerPhase, timerRound)) {
			return;                                 // that phase already ended
		}
		if (timerPhase.equals("DISCUSS")) {
			openVoting();
		} else if (timerPhase.equals("VOTE")) {
			broadcast("INFO:Voting time is over!");
			countVotes();
		} else if (timerPhase.equals("GUESS")) {
			finishRound(false, false, outsider.name + " ran out of time and could not guess the word.");
		}
	}

	// =================================================================
	// [S8] VOTING
	// =================================================================
	private void openVoting() {
		phase = "VOTE";
		broadcast("PHASE:VOTE");
		broadcast("INFO:Time is up! Vote for the player you think is Brra Al-Salfa. ("
				+ VOTE_TIME + " seconds)");
		broadcast("VOTES:0/" + countInRound());
		GameTimer timer = new GameTimer(this, VOTE_TIME, "VOTE", roundNumber);
		timer.start();
		updateStatus();
	}

	// the server records each player's vote
	public synchronized void vote(ClientHandler voter, String name) {
		if (!phase.equals("VOTE") || !voter.inRound) {
			voter.send("INFO:You cannot vote right now.");
			return;
		}
		if (voter.votedFor != null) {
			voter.send("INFO:You already voted this round.");
			return;
		}
		voter.votedFor = name;
		showMessage(voter.name + " voted for " + name);
		broadcast("INFO:" + voter.name + " has voted.");          // we do not say for who!
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
		String summary = "Votes:  ";

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
			summary = summary + candidate.name + " (" + votes + ")   ";

			// remember the player with the most votes
			if (votes > maxVotes) {
				maxVotes = votes;
				mostVoted = candidate;
				tie = false;
			} else if (votes == maxVotes && votes > 0) {
				tie = true;                // two players have the same top number
			}
		}
		broadcast("INFO:" + summary);
		showMessage(summary);

		if (mostVoted == null) {
			finishRound(true, false, "Nobody voted, so the outsider escaped!");
		} else if (tie) {
			finishRound(true, false, "The vote was a tie, so the outsider escaped!");
		} else if (mostVoted == outsider) {
			// caught! the outsider gets one last chance (bonus)
			phase = "GUESS";
			broadcast("PHASE:GUESS");
			broadcast("INFO:" + outsider.name + " has been caught! But they get ONE last chance to guess the word... ("
					+ GUESS_TIME + " seconds)");
			outsider.send("GUESS_TURN");
			GameTimer timer = new GameTimer(this, GUESS_TIME, "GUESS", roundNumber);
			timer.start();
			updateStatus();
		} else {
			finishRound(true, false, mostVoted.name + " was innocent! The outsider fooled everyone.");
		}
	}

	// =================================================================
	// [S10] LAST CHANCE GUESS (bonus)
	// =================================================================
	public synchronized void guess(ClientHandler player, String word) {
		if (!phase.equals("GUESS") || player != outsider) {
			return;                               // only the caught outsider can guess
		}
		showMessage(player.name + " guessed: " + word);
		broadcast("INFO:" + player.name + " guesses... \"" + word + "\"");
		if (word.trim().equalsIgnoreCase(secretWord)) {
			finishRound(true, true, player.name + " guessed the word correctly and STOLE the win!");
		} else {
			finishRound(false, false, "Wrong guess! " + player.name + " could not escape.");
		}
	}

	// =================================================================
	// [S11] END OF ROUND + XP
	//  outsiderWins = true if the outsider won
	//  stolen       = true if the outsider won with the last chance guess
	// =================================================================
	private void finishRound(boolean outsiderWins, boolean stolen, String reason) {
		String winner;
		if (outsiderWins) {
			winner = "Brra Al-Salfa (" + outsider.name + ")";
		} else {
			winner = "The players";
		}

		// broadcast the result: who the outsider was, the word, and who won
		String result = "Outsider: " + outsider.name + "  |  Word: " + secretWord
				+ "  |  Winner: " + winner + "  |  " + reason;
		broadcast("RESULT:" + result);
		showMessage("RESULT -> " + result);

		// give XP to every player of this round and save their profile
		for (int i = 0; i < clients.size(); i++) {
			ClientHandler p = clients.elementAt(i);
			if (!p.inRound) {
				continue;
			}
			int points = PLAY_XP;
			boolean won;
			if (p == outsider) {
				won = outsiderWins;
				if (stolen) {
					points = points + STEAL_XP;
				} else if (outsiderWins) {
					points = points + ESCAPE_XP;
				}
			} else {
				won = !outsiderWins;
				if (won) {
					points = points + WIN_XP;
				}
				if (outsider.name.equals(p.votedFor)) {
					points = points + SHARP_EYE_XP;   // voted for the right person
				}
			}

			// update this player's numbers
			p.sessionPoints = p.sessionPoints + points;
			p.games++;
			p.xp = p.xp + points;
			if (won) {
				p.wins++;
				p.streak++;
			} else {
				p.streak = 0;
			}
			saveProfile(p);                       // write to players.dat
			if (won) {
				p.send("XP:" + points + ",WIN");
			} else {
				p.send("XP:" + points + ",LOSE");
			}
			sendProfile(p);
		}

		logHistory(result);                       // write to history.txt
		sendLeaderboard();
		endRound();
	}

	// stops a round without giving XP (someone left)
	private void cancelRound(String reason) {
		broadcast("INFO:" + reason);
		showMessage("Round cancelled: " + reason);
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
		updateStatus();
	}

	// =================================================================
	// [S12] LEADERBOARD (bonus)
	//  sorted from the highest points to the lowest (bubble sort)
	// =================================================================
	private void sendLeaderboard() {
		int n = clients.size();
		ClientHandler[] sorted = new ClientHandler[n];
		for (int i = 0; i < n; i++) {
			sorted[i] = clients.elementAt(i);
		}
		// bubble sort: swap neighbours until the biggest points are first
		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < n - 1 - i; j++) {
				if (sorted[j].sessionPoints < sorted[j + 1].sessionPoints) {
					ClientHandler temp = sorted[j];
					sorted[j] = sorted[j + 1];
					sorted[j + 1] = temp;
				}
			}
		}
		// build "SCORES:meryem  260 pts  (2 win streak),ali  110 pts"
		String scores = "SCORES:";
		for (int i = 0; i < n; i++) {
			scores = scores + sorted[i].name + "   " + sorted[i].sessionPoints + " pts";
			if (sorted[i].streak >= 2) {
				scores = scores + "   (" + sorted[i].streak + " win streak!)";
			}
			if (i < n - 1) {
				scores = scores + ",";
			}
		}
		broadcast(scores);
	}

	// =================================================================
	// [S13] PLAYER PROFILES  (RandomAccessFile, like RanFile1)
	//  Every record is 42 bytes:
	//    name  -> 15 chars x 2 bytes = 30 bytes
	//    games -> int = 4 bytes
	//    wins  -> int = 4 bytes
	//    xp    -> int = 4 bytes
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
					player.xp = file.readInt();
					found = true;
					break;
				}
			}
			file.close();

			// a new player: their record goes at the end of the file
			if (!found) {
				player.recordNum = numRecords;
				player.games = 0;
				player.wins = 0;
				player.xp = 0;
				saveProfile(player);
			}
		} catch (IOException ioEx) {
			showMessage("Could not read " + PROFILE_FILE);
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
			file.writeInt(player.xp);
			file.close();
		} catch (IOException ioEx) {
			showMessage("Could not save " + PROFILE_FILE);
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

	// the rank title depends on the total XP
	private String getRank(int xp) {
		if (xp >= 1500) {
			return "Legend";
		} else if (xp >= 800) {
			return "Mastermind";
		} else if (xp >= 300) {
			return "Detective";
		} else {
			return "Rookie";
		}
	}

	// sends "PROFILE:rank,level,games,wins,xp" to one player
	private void sendProfile(ClientHandler player) {
		int level = player.xp / 100 + 1;          // a new level every 100 XP
		player.send("PROFILE:" + getRank(player.xp) + "," + level + ","
				+ player.games + "," + player.wins + "," + player.xp);
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
			showMessage("Could not write " + HISTORY_FILE);
		}
	}

	// reads history.txt line by line and shows it in the log
	private void showHistory() {
		File historyFile = new File(HISTORY_FILE);
		if (!historyFile.exists()) {
			showMessage("No rounds have been played yet.");
			return;
		}
		try {
			Scanner input = new Scanner(historyFile);
			showMessage("\n========== ROUND HISTORY ==========");
			while (input.hasNextLine()) {
				showMessage(input.nextLine());
			}
			showMessage("===================================\n");
			input.close();
		} catch (IOException ioEx) {
			showMessage("Could not read " + HISTORY_FILE);
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
	boolean inRound = false;      // is this player in the current round?
	String votedFor = null;       // who this player voted for (null = not yet)
	int sessionPoints = 0;        // points since joining (leaderboard)
	int streak = 0;               // wins in a row
	long recordNum = 0;           // position of the record in players.dat
	int games = 0, wins = 0, xp = 0;   // saved profile

	public ClientHandler(Socket socket, SpyServer gui) {
		client = socket;
		server = gui;
		try {
			// set up input and output streams
			input = new Scanner(client.getInputStream());
			output = new PrintWriter(client.getOutputStream(), true);
		} catch (IOException ioEx) {
			server.showMessage("Error setting up client streams.");
		}
	}

	// sends one line to this player
	public void send(String message) {
		output.println(message);
	}

	public void run() {
		String received = "";
		try {
			// the FIRST message from a client is the nickname
			name = input.nextLine().trim();
			server.addPlayer(this);

			do {
				// receive the next message from this player
				received = input.nextLine();

				// look at the prefix to know what kind of message it is
				if (received.startsWith("CHAT:")) {
					server.chat(this, received.substring(5));        // text after "CHAT:"
				} else if (received.equals("START_GAME")) {
					server.startGame(this);
				} else if (received.startsWith("VOTE:")) {
					server.vote(this, received.substring(5));        // name after "VOTE:"
				} else if (received.startsWith("GUESS:")) {
					server.guess(this, received.substring(6));       // word after "GUESS:"
				}
			} while (true);
		} catch (Exception ex) {
			// NoSuchElementException: the player closed the window (empty catch, as the brief says)
		} finally {
			server.removePlayer(this);     // remove from the Vector and tell everyone
			try {
				if (client != null) {
					client.close();
				}
			} catch (IOException ioEx) {
				server.showMessage("Unable to disconnect client.");
			}
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
				sleep(1000);          // wait 1 second
			}
			server.broadcast("TIMER:0");
			server.timeUp(phase, round);
		} catch (InterruptedException e) {
			// the thread was interrupted, nothing to do
		}
	}
}
