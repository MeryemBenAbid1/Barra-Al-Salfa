/* =====================================================================
 *  ChatPanel.java  -  "INTERCEPTED" chat with a typewriter effect
 * ---------------------------------------------------------------------
 *  New lines are typed out letter by letter. If many lines arrive at
 *  once, it types faster so it never falls behind.
 *  The same ChatPanel object is moved between the lobby and the round
 *  screen, so the conversation is never lost.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class ChatPanel extends JPanel {

	public interface SendListener {
		void send(String text);
	}

	// push-to-talk: start() returns false if the microphone could not start
	public interface TalkListener {
		boolean start();
		void stop();
	}

	private final JLabel title;
	private final JTextPane log;
	private final JScrollPane scroll;
	private final HintField field;
	private final DossierButton sendButton;
	private SendListener listener;
	private TalkListener talkListener;
	private final DossierButton talkButton;
	private final JLabel talkingLabel = new JLabel(" ");
	private final java.util.LinkedHashSet<String> talkingNow = new java.util.LinkedHashSet<String>();
	private boolean onAir = false;

	// typewriter queue
	private static class Piece {
		String text;
		AttributeSet style;
		int done = 0;
		Piece(String text, AttributeSet style) { this.text = text; this.style = style; }
	}
	private final LinkedList<Piece> queue = new LinkedList<Piece>();
	private final Timer typer;
	private String myName = "";
	private boolean guessMode = false;

	public ChatPanel() {
		setOpaque(false);
		setLayout(new BorderLayout(0, 10));

		title = new JLabel("INTERCEPTED");
		title.setFont(Theme.stencil(18f));
		title.setForeground(Theme.INK);
		title.setBorder(new CompoundBorder(new MatteBorder(0, 0, 2, 0, Theme.INK), new EmptyBorder(0, 0, 4, 0)));
		add(title, BorderLayout.NORTH);

		log = new JTextPane();
		log.setEditable(false);
		log.setOpaque(false);
		log.setBorder(new EmptyBorder(2, 0, 2, 0));
		scroll = new JScrollPane(log);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		add(scroll, BorderLayout.CENTER);

		JPanel inputRow = new JPanel(new BorderLayout(8, 0));
		inputRow.setOpaque(false);
		field = new HintField("");
		sendButton = new DossierButton("", DossierButton.INK, 14f, 0);
		sendButton.setPreferredSize(new Dimension(84, 46));
		inputRow.add(field, BorderLayout.CENTER);
		inputRow.add(sendButton, BorderLayout.LINE_END);

		// push-to-talk: hold the button to talk, let go to stop
		talkButton = new DossierButton("", DossierButton.OUTLINE, 14f, 0);
		talkButton.setPreferredSize(new Dimension(200, 44));
		talkButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (talkButton.isEnabled() && talkListener != null && talkListener.start()) {
					onAir = true;
					talkButton.setStyle(DossierButton.RED);
					talkButton.setText(Lang.t("on_air"));
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (onAir) {
					onAir = false;
					talkListener.stop();
					talkButton.setStyle(DossierButton.OUTLINE);
					talkButton.setText(Lang.t("talk"));
				}
			}
		});
		talkingLabel.setForeground(Theme.RED);

		JPanel bottom = new JPanel(new BorderLayout(0, 8));
		bottom.setOpaque(false);
		bottom.add(talkingLabel, BorderLayout.NORTH);
		bottom.add(inputRow, BorderLayout.CENTER);
		bottom.add(talkButton, BorderLayout.SOUTH);
		add(bottom, BorderLayout.SOUTH);

		ActionListener sendAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = field.getText().trim();
				if (text.length() > 0 && listener != null) {
					listener.send(text);
					field.setText("");
				}
			}
		};
		field.addActionListener(sendAction);     // Enter key
		sendButton.addActionListener(sendAction);

		applyLanguage();

		typer = new Timer(16, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				typeSome();
			}
		});
	}

	public void setSendListener(SendListener listener) { this.listener = listener; }
	public void setTalkListener(TalkListener listener) { this.talkListener = listener; }

	public void setVoiceEnabled(boolean on) {
		talkButton.setVisible(on);
	}

	// someone started / stopped talking
	public void setTalking(String name, boolean on) {
		if (on) {
			talkingNow.add(name);
		} else {
			talkingNow.remove(name);
		}
		updateTalkingLabel();
	}

	private void updateTalkingLabel() {
		if (talkingNow.isEmpty()) {
			talkingLabel.setText(" ");
			return;
		}
		String names = "";
		for (String n : talkingNow) {
			names = names.length() == 0 ? n : names + ", " + n;
		}
		Theme.text(talkingLabel, Theme.monoBold(14f), "● " + Lang.f("talking", Lang.rtl() ? names : names.toUpperCase()));
	}
	public void setMyName(String name) { myName = name; }
	public void focusInput() { field.requestFocusInWindow(); }

	public void setInputEnabled(boolean on) {
		field.setEnabled(on);
		sendButton.setEnabled(on);
	}

	// in the last-chance phase the outsider types a guess instead of a message
	public void setGuessMode(boolean guess) {
		guessMode = guess;
		applyLanguage();
	}

	public void applyLanguage() {
		field.setHint(Lang.t(guessMode ? "guess_hint" : "msg_hint"));
		talkButton.setText(Lang.t(onAir ? "on_air" : "talk"));
		updateTalkingLabel();
		sendButton.setText(Lang.t(guessMode ? "guess" : "send"));
		Theme.text(title, Theme.stencil(18f), Lang.t(guessMode ? "last_chance" : "intercepted"));
		title.setForeground(guessMode ? Theme.RED : Theme.INK);
		// line up the chat on the right in Arabic
		SimpleAttributeSet align = new SimpleAttributeSet();
		StyleConstants.setAlignment(align, Lang.rtl() ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
		StyledDocument doc = log.getStyledDocument();
		doc.setParagraphAttributes(0, doc.getLength() + 1, align, false);
	}

	public void applyComponentOrientation(ComponentOrientation o) {
		super.applyComponentOrientation(o);
		// the text pane stays left-to-right: Swing clips wrapped right-to-left text in a
		// scroll pane. Arabic lines are still right-aligned (see applyLanguage).
		scroll.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
	}

	public void clear() {
		queue.clear();
		log.setText("");
	}

	// ---------- adding lines ----------
	public void addChat(String line) {
		int split = line.indexOf(": ");
		if (split > 0 && split < 30) {
			String name = line.substring(0, split);
			boolean me = name.equals(myName);
			queueText(name.toUpperCase() + ": ", style(Theme.monoBold(15f), me ? Theme.RED : Theme.INK, name));
			String body = line.substring(split + 2);
			queueText(body + "\n", style(Theme.type(15f), Theme.INK, body));
		} else {
			queueText(line + "\n", style(Theme.type(15f), Theme.INK, line));
		}
		Theme.play("type");
	}

	public void addInfo(String text) {
		queueText("> " + text.toUpperCase() + "\n", style(Theme.type(14f), new Color(110, 98, 82), text));
	}

	public void addAlert(String text) {
		queueText("> " + text.toUpperCase() + "\n", style(Theme.monoBold(14f), Theme.RED, text));
	}

	private AttributeSet style(Font font, Color color, String text) {
		Font f = Theme.safe(font, text);   // Arabic text uses the system font
		SimpleAttributeSet s = new SimpleAttributeSet();
		StyleConstants.setFontFamily(s, f.getFamily());
		StyleConstants.setFontSize(s, f.getSize());
		StyleConstants.setBold(s, f.isBold());
		StyleConstants.setForeground(s, color);
		StyleConstants.setLineSpacing(s, 0.15f);
		return s;
	}

	private void queueText(String text, AttributeSet style) {
		queue.add(new Piece(text, style));
		if (!typer.isRunning()) {
			typer.start();
		}
	}

	// called by the timer: types a few letters
	private void typeSome() {
		if (queue.isEmpty()) {
			typer.stop();
			return;
		}
		int waiting = 0;
		for (Piece p : queue) {
			waiting += p.text.length() - p.done;
		}
		int letters = 1 + waiting / 25;      // faster when there is a backlog
		StyledDocument doc = log.getStyledDocument();
		try {
			while (letters > 0 && !queue.isEmpty()) {
				Piece p = queue.getFirst();
				int n = Math.min(letters, p.text.length() - p.done);
				doc.insertString(doc.getLength(), p.text.substring(p.done, p.done + n), p.style);
				p.done += n;
				letters -= n;
				if (p.done >= p.text.length()) {
					queue.removeFirst();
				}
			}
		} catch (BadLocationException ex) {
			queue.clear();
		}
		log.setCaretPosition(doc.getLength());
	}

	// ---------- text field with a grey hint ----------
	private static class HintField extends JTextField {
		private String hint;

		HintField(String hint) {
			this.hint = hint;
			setOpaque(false);
			setFont(Theme.type(16f));
			setForeground(Theme.INK);
			setCaretColor(Theme.RED);
			setBorder(new CompoundBorder(new MatteBorder(0, 0, 2, 0, Theme.INK), new EmptyBorder(8, 4, 6, 4)));
			// switch to the system font while the text has Arabic letters
			getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
				public void insertUpdate(javax.swing.event.DocumentEvent e)  { pickFont(); }
				public void removeUpdate(javax.swing.event.DocumentEvent e)  { pickFont(); }
				public void changedUpdate(javax.swing.event.DocumentEvent e) { }
			});
		}

		private void pickFont() {
			final Font wanted = Theme.safe(Theme.type(16f), getText());
			if (!wanted.getFamily().equals(getFont().getFamily())) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() { setFont(wanted); }
				});
			}
		}

		void setHint(String hint) {
			this.hint = hint;
			repaint();
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (getText().length() == 0) {
				Graphics2D g2 = Theme.smooth(g);
				g2.setColor(new Color(140, 127, 106));
				g2.setFont(getFont());
				g2.setFont(Theme.safe(getFont(), hint));
				Insets in = getInsets();
				int x = Lang.rtl() ? getWidth() - in.right - g2.getFontMetrics().stringWidth(hint) : in.left;
				g2.drawString(hint, x, in.top + g2.getFontMetrics().getAscent());
				g2.dispose();
			}
		}
	}
}
