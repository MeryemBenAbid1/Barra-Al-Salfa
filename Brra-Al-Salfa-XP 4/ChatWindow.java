/* =====================================================================
 *  ChatWindow.java  -  the chat (Messenger style) + push-to-talk voice
 * ---------------------------------------------------------------------
 *  - the white box shows the chat lines, like old MSN Messenger:
 *        Nouri says:
 *          hi everyone!
 *    (a JTextPane is a text area that can use colors)
 *  - the text field + Send button send "CHAT:<text>" (see SpyClient)
 *  - HOLD TO TALK: while the mouse button is held down, VoiceChat sends
 *    the microphone over UDP. A green line shows who is talking.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

public class ChatWindow extends XpWindow {

	// what the window asks SpyClient to do
	public interface Listener {
		void send(String text);
		boolean startTalking();
		void stopTalking();
	}

	private Listener listener;
	// each line finds its own direction (English left to right, Arabic right to
	// left), so the box itself never switches: old English lines stay readable
	private final JTextPane lines = new JTextPane() {
		public void setComponentOrientation(ComponentOrientation o) {
			// on purpose: do nothing
		}
	};
	private final XpParts.HintField input = new XpParts.HintField("Type a message...");
	private final XpButton sendButton = new XpButton("Send", XpButton.NORMAL);
	private final XpButton talkButton = new XpButton("Hold to talk", XpButton.NORMAL);
	private final JLabel talking = new JLabel(" ");
	private String myName = "";
	// each player's name is written in the (darker) color of their shirt
	private final java.util.HashMap<String, Color> nameColors = new java.util.HashMap<String, Color>();
	// a color for each name, so the same player always has the same color
	private static final Color[] NAME_COLORS = {
		new Color(0x1A7D76), new Color(0x6A4FC6), new Color(0x2B6FD6), new Color(0xB9531A),
		new Color(0x3A9C35), new Color(0xC2457A) };

	public ChatWindow() {
		super("Chat", Xp.pixelIcon(Xp.CHAT, 2));
		helpMenu();

		// the chat lines
		lines.setEditable(false);
		// no fixed direction for the whole box: every line takes the
		// direction of its first letter (a \u200F mark = right to left)
		lines.getDocument().putProperty(java.awt.font.TextAttribute.RUN_DIRECTION, null);
		lines.setFont(Xp.MONO.deriveFont(15f));
		lines.setBorder(new EmptyBorder(8, 10, 8, 10));
		JScrollPane scroll = Xp.scroll(lines);

		// text field + Send
		input.setFont(Xp.MONO.deriveFont(15f));
		sendButton.setTextSize(16f);
		sendButton.setMargin(new Insets(6, 14, 6, 14));
		JPanel typeRow = Xp.clear(new BorderLayout(8, 0));
		typeRow.add(input, BorderLayout.CENTER);
		typeRow.add(sendButton, BorderLayout.LINE_END);

		// hold to talk + who is talking
		talkButton.setIcon(Xp.pixelIcon(Xp.MIC, 3));
		talkButton.setTextSize(15f);
		talkButton.setMargin(new Insets(5, 12, 5, 12));
		talking.setFont(Xp.MONO.deriveFont(13f));
		talking.setForeground(Xp.GREEN);
		talking.setIconTextGap(7);
		JPanel talkRow = Xp.clear(new FlowLayout(FlowLayout.LEADING, 0, 0));
		talkRow.add(talkButton);
		talkRow.add(Box.createHorizontalStrut(10));
		talkRow.add(talking);

		JPanel bottom = Xp.clear(new BorderLayout(0, 8));
		bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
		bottom.add(typeRow, BorderLayout.NORTH);
		bottom.add(talkRow, BorderLayout.SOUTH);

		body().add(scroll, BorderLayout.CENTER);
		body().add(bottom, BorderLayout.SOUTH);

		// Send button and the Enter key both send the text
		ActionListener sendAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = input.getText().trim();
				if (text.length() > 0 && listener != null) {
					listener.send(text);
					input.setText("");
				}
			}
		};
		sendButton.addActionListener(sendAction);
		input.addActionListener(sendAction);

		// push-to-talk: talk while the mouse button is DOWN
		talkButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (listener != null && talkButton.isEnabled() && listener.startTalking()) {
					talking.setIcon(XpParts.dot(Xp.GREEN));
					talking.setText(Lang.t("you_talking"));
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (listener != null) {
					listener.stopTalking();
					talking.setIcon(null);
					talking.setText(" ");
				}
			}
		});
	}

	public void setListener(Listener l) { listener = l; }
	public void setMyName(String name)  { myName = name; }
	public void setNameColor(String name, Color shirt) { nameColors.put(name, shirt.darker()); }

	// "Nouri: hi everyone!"  ->  Nouri says:  /  hi everyone!
	public void addChat(String line) {
		int colon = line.indexOf(": ");
		if (colon < 0) {
			addInfo(line);
			return;
		}
		String name = line.substring(0, colon);
		String text = line.substring(colon + 2);
		Color color = nameColors.get(name);
		if (color == null) {
			color = NAME_COLORS[Math.abs(name.hashCode()) % NAME_COLORS.length];
		}
		append(name + " " + Lang.t("says") + "\n", color, false);
		append("  " + text + "\n", Xp.INK, false);
	}

	// a grey line from the server (order, round started...)
	public void addInfo(String text) {
		append(text + "\n", new Color(0x7A7A7A), false);
	}

	// an important line (orange)
	public void addAlert(String text) {
		append(text + "\n", Xp.ORANGE_DARK, true);
	}

	public void clear() {
		lines.setText("");
	}

	// adds colored text at the end and scrolls down
	private void append(String text, Color color, boolean bold) {
		StyledDocument doc = lines.getStyledDocument();
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, color);
		StyleConstants.setBold(style, bold);
		StyleConstants.setFontFamily(style, Xp.hasArabic(text) ? Xp.AR_TEXT.getFamily() : Xp.MONO.getFamily());
		StyleConstants.setFontSize(style, Xp.hasArabic(text) ? 16 : 15);
		if (Lang.arabic() && Xp.hasArabic(text)) {
			text = "\u200F" + text;              // this line reads right to left
		}
		int start = doc.getLength();
		try {
			doc.insertString(start, text, style);
			// in Arabic, new lines sit on the right side of the box
			SimpleAttributeSet side = new SimpleAttributeSet();
			StyleConstants.setAlignment(side, Lang.arabic() ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
			doc.setParagraphAttributes(start, text.length(), side, false);
		} catch (BadLocationException ex) {
			// cannot happen: we always add at the end
		}
		lines.setCaretPosition(doc.getLength());
	}

	// a player started / stopped talking (from VoiceChat)
	public void setTalking(String name, boolean on) {
		talking.setIcon(on ? XpParts.dot(Xp.GREEN) : null);
		talking.setText(on ? Lang.f("is_talking", name) : " ");
	}

	public void setInputEnabled(boolean on) {
		input.setEnabled(on);
		sendButton.setEnabled(on);
		talkButton.setEnabled(on);
	}

	public void focusInput() {
		input.requestFocusInWindow();
	}

	public void applyLanguage() {
		setTitle(Lang.t("chat"));
		sendButton.setText(Lang.t("send"));
		input.setHint(Lang.t("type_msg"));
		talkButton.setText(Lang.t("hold_talk"));
		helpMenu();
	}
}
