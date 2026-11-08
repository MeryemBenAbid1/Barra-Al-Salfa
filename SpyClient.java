/* =====================================================================
 *  SpyClient.java  -  "Brra Al-Salfa" (برا السالفة) Game Client
 *  CpE 458 - Network Programming - Dr. Mohamad Awad
 * ---------------------------------------------------------------------
 *  BIG PICTURE
 *   1. Pop-up windows ask for the nickname and the server IP.
 *   2. We connect a Socket to the server (port 1202) and send the
 *      nickname as the FIRST message.
 *   3. A separate thread (run method) receives messages from the
 *      server and updates the window.
 *   4. The buttons send messages to the server (CHAT:, START_GAME, VOTE:).
 *
 *  SECTION MAP (search for the tag, e.g. "[C6]")
 *   [C1] Settings and colors          [C6] Buttons (actionPerformed)
 *   [C2] Variables                    [C7] Receiving thread (run)
 *   [C3] main                         [C8] Handling each message type
 *   [C4] Building the window          [C9] Phases (enable / disable buttons)
 *   [C5] Connecting to the server     [C10] Profile, XP bar, leaderboard
 *                                     [C11] Small helper methods
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class SpyClient extends JFrame implements ActionListener, Runnable {

	// =================================================================
	// [C1] SETTINGS AND COLORS
	// =================================================================
	private static final int PORT = 1202;                              // same port as the server
	private static final String OUTSIDER_TEXT = "You are Brra Al-Salfa!";

	// theme: "Midnight Diwaniya"
	private static final Color BG = new Color(14, 20, 38);              // deep navy background
	private static final Color PANEL = new Color(24, 33, 58);           // cards / panels
	private static final Color FIELD = new Color(34, 46, 78);           // text fields
	private static final Color GOLD = new Color(212, 175, 55);          // main accent
	private static final Color CREAM = new Color(245, 236, 215);        // normal text
	private static final Color MUTED = new Color(150, 160, 190);        // small grey text
	private static final Color RED = new Color(196, 64, 64);            // outsider / lose
	private static final Color GREEN = new Color(46, 150, 96);          // win

	// colors for the letter avatars (chosen from the first letter of the name)
	private static final Color[] AVATAR_COLORS = {
		new Color(212, 175, 55), new Color(196, 64, 64), new Color(46, 150, 96),
		new Color(70, 130, 200), new Color(150, 90, 190), new Color(220, 120, 60) };

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
	private int myLevel = 0;              // to detect "LEVEL UP!"

	// top
	private JLabel titleLabel, arabicLabel, categoryLabel, phaseLabel, timerLabel;
	private JLabel roleLabel;             // shows the secret word or the outsider message
	// center
	private JTextArea chatArea;
	// right side
	private JLabel avatarLabel, nameLabel, rankLabel, statsLabel, votesLabel;
	private JProgressBar xpBar;
	private JTextArea leaderboard;
	private JComboBox<String> playerBox;  // drop-down list of the other players
	private JButton voteButton;
	// bottom
	private JTextField messageField;
	private JButton sendButton, startButton;

	// =================================================================
	// [C3] MAIN
	// =================================================================
	public static void main(String[] args) {
		SpyClient frame = new SpyClient();
		frame.setSize(1050, 700);
		frame.setVisible(true);

		// window listener: close the socket when the window is closed (like GetRemoteTime)
		frame.addWindowListener(
			new WindowAdapter() {
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
			}
		);

		frame.connectToServer();
	}

	// =================================================================
	// [C4] BUILDING THE WINDOW
	//   NORTH  = title bar + role banner
	//   CENTER = chat
	//   EAST   = profile card + leaderboard + voting
	//   SOUTH  = message field + Send + Start Game
	// =================================================================
	public SpyClient() {
		setTitle("Brra Al-Salfa");
		setLayout(new BorderLayout());
		getContentPane().setBackground(BG);

		add(buildTopPanel(), BorderLayout.NORTH);
		add(buildChatPanel(), BorderLayout.CENTER);
		add(buildSidePanel(), BorderLayout.EAST);
		add(buildBottomPanel(), BorderLayout.SOUTH);

		// nothing can be clicked before the game starts
		voteButton.setEnabled(false);
	}

	// ---------- NORTH: title bar + role banner ----------
	private JPanel buildTopPanel() {
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(BG);
		topPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 6, 18));

		// left: game title
		JPanel titlePanel = new JPanel(new GridLayout(2, 1));
		titlePanel.setBackground(BG);
		titleLabel = makeLabel("BRRA AL-SALFA", 26, Font.BOLD, GOLD);
		arabicLabel = makeLabel("برا السالفة   •   Find the outsider before they find the word", 13, Font.PLAIN, MUTED);
		titlePanel.add(titleLabel);
		titlePanel.add(arabicLabel);
		topPanel.add(titlePanel, BorderLayout.WEST);

		// right: category, phase and the countdown
		JPanel timerPanel = new JPanel(new GridLayout(1, 3, 18, 0));
		timerPanel.setBackground(BG);
		categoryLabel = makeLabel("Category: -", 14, Font.BOLD, CREAM);
		phaseLabel = makeLabel("LOBBY", 14, Font.BOLD, GOLD);
		timerLabel = makeLabel("--:--", 30, Font.BOLD, CREAM);
		categoryLabel.setHorizontalAlignment(JLabel.RIGHT);
		phaseLabel.setHorizontalAlignment(JLabel.RIGHT);
		timerLabel.setHorizontalAlignment(JLabel.RIGHT);
		timerPanel.add(categoryLabel);
		timerPanel.add(phaseLabel);
		timerPanel.add(timerLabel);
		topPanel.add(timerPanel, BorderLayout.EAST);

		// below: the role banner (the label the brief asks for)
		roleLabel = new JLabel("Waiting for a player to press Start Game...");
		roleLabel.setHorizontalAlignment(JLabel.CENTER);
		roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
		roleLabel.setOpaque(true);                   // so the background color shows
		roleLabel.setPreferredSize(new Dimension(100, 70));
		setBanner("Waiting for a player to press Start Game...", PANEL, CREAM);
		topPanel.add(roleLabel, BorderLayout.SOUTH);

		return topPanel;
	}

	// ---------- CENTER: chat ----------
	private JPanel buildChatPanel() {
		JPanel chatPanel = new JPanel(new BorderLayout());
		chatPanel.setBackground(BG);
		chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 8));

		chatPanel.add(makeLabel("CHAT", 12, Font.BOLD, MUTED), BorderLayout.NORTH);

		chatArea = new JTextArea();
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);
		chatArea.setBackground(PANEL);
		chatArea.setForeground(CREAM);
		chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		chatArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
		JScrollPane scroll = new JScrollPane(chatArea);
		scroll.setBorder(BorderFactory.createLineBorder(FIELD, 1));
		chatPanel.add(scroll, BorderLayout.CENTER);
		return chatPanel;
	}

	// ---------- EAST: profile card + leaderboard + voting ----------
	private JPanel buildSidePanel() {
		JPanel sidePanel = new JPanel(new BorderLayout(0, 10));
		sidePanel.setBackground(BG);
		sidePanel.setPreferredSize(new Dimension(300, 100));
		sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 18));

		// --- profile card ---
		JPanel profileCard = new JPanel(new BorderLayout(12, 0));
		profileCard.setBackground(PANEL);
		profileCard.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(GOLD, 1),
				BorderFactory.createEmptyBorder(12, 12, 12, 12)));

		avatarLabel = makeLabel("?", 28, Font.BOLD, BG);      // letter avatar
		avatarLabel.setOpaque(true);
		avatarLabel.setBackground(GOLD);
		avatarLabel.setHorizontalAlignment(JLabel.CENTER);
		avatarLabel.setPreferredSize(new Dimension(56, 56));
		profileCard.add(avatarLabel, BorderLayout.WEST);

		JPanel infoPanel = new JPanel(new GridLayout(4, 1, 0, 3));
		infoPanel.setBackground(PANEL);
		nameLabel = makeLabel("Connecting...", 16, Font.BOLD, CREAM);
		rankLabel = makeLabel("Rookie  •  Level 1", 13, Font.BOLD, GOLD);
		xpBar = new JProgressBar(0, 100);                     // XP toward the next level
		xpBar.setStringPainted(true);
		xpBar.setString("0 / 100 XP");
		xpBar.setForeground(GOLD);
		xpBar.setBackground(FIELD);
		xpBar.setBorderPainted(false);
		statsLabel = makeLabel("Games 0   Wins 0", 12, Font.PLAIN, MUTED);
		infoPanel.add(nameLabel);
		infoPanel.add(rankLabel);
		infoPanel.add(xpBar);
		infoPanel.add(statsLabel);
		profileCard.add(infoPanel, BorderLayout.CENTER);
		sidePanel.add(profileCard, BorderLayout.NORTH);

		// --- leaderboard ---
		JPanel boardPanel = new JPanel(new BorderLayout(0, 6));
		boardPanel.setBackground(BG);
		boardPanel.add(makeLabel("LEADERBOARD", 12, Font.BOLD, MUTED), BorderLayout.NORTH);
		leaderboard = new JTextArea();
		leaderboard.setEditable(false);
		leaderboard.setBackground(PANEL);
		leaderboard.setForeground(CREAM);
		leaderboard.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		leaderboard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		JScrollPane boardScroll = new JScrollPane(leaderboard);
		boardScroll.setBorder(BorderFactory.createLineBorder(FIELD, 1));
		boardPanel.add(boardScroll, BorderLayout.CENTER);
		sidePanel.add(boardPanel, BorderLayout.CENTER);

		// --- voting ---
		JPanel votePanel = new JPanel(new GridLayout(4, 1, 0, 6));
		votePanel.setBackground(PANEL);
		votePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(FIELD, 1),
				BorderFactory.createEmptyBorder(10, 12, 10, 12)));
		votePanel.add(makeLabel("WHO IS BRRA AL-SALFA?", 12, Font.BOLD, MUTED));
		playerBox = new JComboBox<String>();
		playerBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		playerBox.setBackground(FIELD);
		playerBox.setForeground(CREAM);
		votePanel.add(playerBox);
		voteButton = makeButton("Vote", RED, CREAM);
		votePanel.add(voteButton);
		votesLabel = makeLabel("Voting opens after the questions", 12, Font.PLAIN, MUTED);
		votePanel.add(votesLabel);
		sidePanel.add(votePanel, BorderLayout.SOUTH);

		return sidePanel;
	}

	// ---------- SOUTH: message field + Send + Start Game ----------
	private JPanel buildBottomPanel() {
		JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
		bottomPanel.setBackground(BG);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 18, 16, 18));

		messageField = new JTextField();
		messageField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		messageField.setBackground(FIELD);
		messageField.setForeground(CREAM);
		messageField.setCaretColor(GOLD);
		messageField.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		messageField.addActionListener(this);          // pressing Enter also sends
		bottomPanel.add(messageField, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
		buttons.setBackground(BG);
		sendButton = makeButton("Send", FIELD, CREAM);
		startButton = makeButton("Start Game", GOLD, BG);
		buttons.add(sendButton);
		buttons.add(startButton);
		bottomPanel.add(buttons, BorderLayout.EAST);
		return bottomPanel;
	}

	// =================================================================
	// [C5] CONNECTING TO THE SERVER
	// =================================================================
	private void connectToServer() {
		// 1. ask for the nickname in a pop-up (the hint from the brief)
		String handle = JOptionPane.showInputDialog(this, "Enter your nickname: ",
				"Nickname", JOptionPane.PLAIN_MESSAGE);
		if (handle == null || handle.trim().equals("")) {
			System.exit(0);                            // Cancel was pressed
		}
		// a comma would break the "PLAYERS:a,b,c" list, so we remove it
		myName = handle.replace(",", "").trim();

		// 2. ask for the server address (empty = this same computer)
		String host = JOptionPane.showInputDialog(this,
				"Enter the server IP address\n(leave it empty if the server is on this computer):",
				"Connect to server", JOptionPane.PLAIN_MESSAGE);
		if (host == null) {
			System.exit(0);
		}
		if (host.trim().equals("")) {
			host = "localhost";
		}

		try {
			// 3. create the socket and the streams
			socket = new Socket(host.trim(), PORT);
			input = new Scanner(socket.getInputStream());
			output = new PrintWriter(socket.getOutputStream(), true);

			// 4. the FIRST message is the nickname
			output.println(myName);

			setTitle("Brra Al-Salfa  -  " + myName);
			nameLabel.setText(myName);
			avatarLabel.setText(myName.substring(0, 1).toUpperCase());
			avatarLabel.setBackground(AVATAR_COLORS[myName.charAt(0) % AVATAR_COLORS.length]);
			addInfo("Connected to " + host + " on port " + PORT);

			// 5. start the thread that receives messages from the server
			Thread receiveThread = new Thread(this);
			receiveThread.start();
		} catch (UnknownHostException uhEx) {
			JOptionPane.showMessageDialog(this, "No such host!", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} catch (IOException ioEx) {
			JOptionPane.showMessageDialog(this, "Could not connect. Is the server running?",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	// =================================================================
	// [C6] BUTTONS
	// =================================================================
	public void actionPerformed(ActionEvent event) {
		// Send button or Enter key in the text field
		if (event.getSource() == sendButton || event.getSource() == messageField) {
			String text = messageField.getText().trim();
			if (text.equals("")) {
				return;                                // nothing to send
			}
			if (guessMode) {
				// last chance: this text is our guess, not a chat message
				output.println("GUESS:" + text);
				guessMode = false;
				sendButton.setText("Send");
			} else {
				output.println("CHAT:" + text);
			}
			messageField.setText("");
		}
		// Start Game button
		else if (event.getSource() == startButton) {
			output.println("START_GAME");
		}
		// Vote button
		else if (event.getSource() == voteButton) {
			String choice = (String) playerBox.getSelectedItem();
			if (choice == null) {
				return;                                // the list is empty
			}
			output.println("VOTE:" + choice);
			voteButton.setEnabled(false);              // only ONE vote per round
			addInfo("You voted for " + choice + ".");
		}
	}

	// =================================================================
	// [C7] RECEIVING THREAD
	//   runs at the same time as the window, and waits for messages
	// =================================================================
	public void run() {
		try {
			do {
				String message = input.nextLine();     // wait for the next line
				handleMessage(message);
			} while (true);
		} catch (Exception ex) {
			// NoSuchElementException when the socket is closed (empty catch, as the brief says)
		}
		// the server is gone
		addInfo("Disconnected from the server.");
		startButton.setEnabled(false);
		voteButton.setEnabled(false);
		sendButton.setEnabled(false);
	}

	// =================================================================
	// [C8] HANDLING EACH MESSAGE TYPE (look at the prefix)
	// =================================================================
	private void handleMessage(String message) {
		if (message.startsWith("CHAT:")) {
			addChat(message.substring(5));
		}
		else if (message.startsWith("INFO:")) {
			addInfo(message.substring(5));
		}
		else if (message.startsWith("PLAYERS:")) {
			updatePlayers(message.substring(8));
		}
		else if (message.startsWith("ROLE:")) {
			String role = message.substring(5);
			inRound = true;
			if (role.equals(OUTSIDER_TEXT)) {
				setBanner(role + "   Blend in and find the word!", RED, CREAM);
			} else {
				setBanner(role, GOLD, BG);
			}
		}
		else if (message.startsWith("CATEGORY:")) {
			categoryLabel.setText("Category: " + message.substring(9));
		}
		else if (message.startsWith("PHASE:")) {
			setPhase(message.substring(6));
		}
		else if (message.startsWith("TIMER:")) {
			showTime(Integer.parseInt(message.substring(6)));
		}
		else if (message.startsWith("VOTES:")) {
			votesLabel.setText(message.substring(6) + " players have voted");
		}
		else if (message.equals("GUESS_TURN")) {
			guessMode = true;
			sendButton.setText("Guess");
			setBanner("LAST CHANCE!  Type the secret word below and press Guess", RED, CREAM);
			messageField.requestFocus();
		}
		else if (message.startsWith("RESULT:")) {
			chatArea.append("\n   ==========  ROUND OVER  ==========\n");
			chatArea.append("   " + message.substring(7) + "\n\n");
			scrollChat();
			setBanner("Round over!", PANEL, CREAM);       // spectators keep this banner
		}
		else if (message.startsWith("XP:")) {
			showMyResult(message.substring(3));
		}
		else if (message.startsWith("PROFILE:")) {
			updateProfile(message.substring(8));
		}
		else if (message.startsWith("SCORES:")) {
			updateLeaderboard(message.substring(7));
		}
	}

	// refreshes the drop-down list with the OTHER players
	private void updatePlayers(String list) {
		playerBox.removeAllItems();
		String[] names = list.split(",");
		for (int i = 0; i < names.length; i++) {
			if (!names[i].equals(myName) && !names[i].equals("")) {
				playerBox.addItem(names[i]);
			}
		}
	}

	// shows "+160 XP" and win / lose in the banner
	private void showMyResult(String data) {
		String[] parts = data.split(",");             // e.g. "160,WIN"
		if (parts[1].equals("WIN")) {
			setBanner("YOU WIN!   +" + parts[0] + " XP", GREEN, CREAM);
		} else {
			setBanner("YOU LOSE   +" + parts[0] + " XP", RED, CREAM);
		}
	}

	// =================================================================
	// [C9] PHASES: enable / disable the buttons
	// =================================================================
	private void setPhase(String phase) {
		if (phase.equals("LOBBY")) {
			phaseLabel.setText("LOBBY");
			timerLabel.setText("--:--");
			timerLabel.setForeground(CREAM);
			startButton.setEnabled(true);
			voteButton.setEnabled(false);
			votesLabel.setText("Voting opens after the questions");
			inRound = false;
			guessMode = false;
			sendButton.setText("Send");
		}
		else if (phase.equals("DISCUSS")) {
			phaseLabel.setText("QUESTIONS");
			startButton.setEnabled(false);
			voteButton.setEnabled(false);
			votesLabel.setText("Voting opens after the questions");
		}
		else if (phase.equals("VOTE")) {
			phaseLabel.setText("VOTING");
			startButton.setEnabled(false);
			voteButton.setEnabled(inRound);           // only players of this round can vote
		}
		else if (phase.equals("GUESS")) {
			phaseLabel.setText("LAST CHANCE");
			startButton.setEnabled(false);
			voteButton.setEnabled(false);
		}
	}

	// shows the seconds as m:ss, and turns red in the last 10 seconds
	private void showTime(int seconds) {
		int minutes = seconds / 60;
		int secs = seconds % 60;
		if (secs < 10) {
			timerLabel.setText(minutes + ":0" + secs);
		} else {
			timerLabel.setText(minutes + ":" + secs);
		}
		if (seconds <= 10) {
			timerLabel.setForeground(RED);
		} else {
			timerLabel.setForeground(CREAM);
		}
	}

	// =================================================================
	// [C10] PROFILE, XP BAR, LEADERBOARD
	// =================================================================
	// data looks like "Detective,4,12,7,340"  (rank, level, games, wins, xp)
	private void updateProfile(String data) {
		String[] parts = data.split(",");
		String rank = parts[0];
		int level = Integer.parseInt(parts[1]);
		int games = Integer.parseInt(parts[2]);
		int wins = Integer.parseInt(parts[3]);
		int xp = Integer.parseInt(parts[4]);

		// LEVEL UP message (but not the first time we receive our profile)
		if (myLevel != 0 && level > myLevel) {
			addInfo("LEVEL UP! You reached level " + level + "  (" + rank + ")");
		}
		myLevel = level;

		rankLabel.setText(rank + "  •  Level " + level);
		xpBar.setValue(xp % 100);                     // progress inside this level
		xpBar.setString(xp % 100 + " / 100 XP   (total " + xp + ")");
		int winRate = 0;
		if (games > 0) {
			winRate = wins * 100 / games;
		}
		statsLabel.setText("Games " + games + "   Wins " + wins + "   Win rate " + winRate + "%");
	}

	// data looks like "meryem   260 pts,ali   110 pts"
	private void updateLeaderboard(String data) {
		String[] lines = data.split(",");
		leaderboard.setText("");
		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				leaderboard.append("#1   " + lines[i] + "   (leader)\n");
			} else {
				leaderboard.append("#" + (i + 1) + "   " + lines[i] + "\n");
			}
		}
	}

	// =================================================================
	// [C11] SMALL HELPER METHODS
	// =================================================================
	// a normal chat line
	private void addChat(String text) {
		chatArea.append("  " + text + "\n");
		scrollChat();
	}

	// a message from the server (announcement)
	private void addInfo(String text) {
		chatArea.append("  >> " + text + "\n");
		scrollChat();
	}

	// always show the newest line
	private void scrollChat() {
		chatArea.setCaretPosition(chatArea.getDocument().getLength());
	}

	// changes the text and the colors of the role banner
	private void setBanner(String text, Color background, Color textColor) {
		roleLabel.setText(text);
		roleLabel.setBackground(background);
		roleLabel.setForeground(textColor);
	}

	// creates a label with a font size, style and color
	private JLabel makeLabel(String text, int size, int style, Color color) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("Segoe UI", style, size));
		label.setForeground(color);
		return label;
	}

	// creates a styled button and makes this window listen to it
	private JButton makeButton(String text, Color background, Color textColor) {
		JButton button = new JButton(text);
		button.setBackground(background);
		button.setForeground(textColor);
		button.setFont(new Font("Segoe UI", Font.BOLD, 14));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
		button.addActionListener(this);
		return button;
	}
}
