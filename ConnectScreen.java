/* =====================================================================
 *  ConnectScreen.java  -  screen 0: agent sign-in
 * ---------------------------------------------------------------------
 *  Replaces the two JOptionPane pop-ups: the player types a codename
 *  (nickname) and the server IP, then presses REPORT FOR DUTY.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ConnectScreen extends JPanel {

	public interface ConnectListener {
		void connect(String nickname, String host);
		void openHostScreen(String host);       // a TV / projector screen, not a player
	}

	private final JTextField nameField = field();
	private final JTextField hostField = field();
	private final JLabel error = new JLabel(" ");
	private final JLabel tag = new JLabel();
	private final JLabel nameLabel = new JLabel();
	private final JLabel hostLabel = new JLabel();
	private final JLabel hint = new JLabel();
	private final JPanel left = new JPanel();
	private final JPanel card;
	private final DossierButton go;
	private final DossierButton hostButton;
	private final JLabel hostHint = new JLabel();
	private final Polaroid photo;
	private ConnectListener listener;
	private String errorKey = "";
	private String errorValue = "";
	private boolean busy = false;

	public ConnectScreen() {
		setOpaque(false);
		setLayout(new GridBagLayout());

		// ---------- title side ----------
		left.setOpaque(false);
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("AL-DAKHEEL");
		title.setFont(Theme.stencil(66f));
		title.setForeground(Theme.INK);
		JLabel arabic = new JLabel("الدخيل");
		arabic.setFont(Theme.arabic(56f));
		arabic.setForeground(Theme.RED);
		tag.setFont(Theme.type(19f));
		tag.setForeground(Theme.INK_SOFT);
		photo = new Polaroid("mascot", "the spy mascot", "", -4, false);
		photo.setOverlayStamp("top_secret");
		photo.setPreferredSize(new Dimension(250, 280));
		photo.setMaximumSize(new Dimension(250, 280));
		left.add(title);
		left.add(arabic);
		left.add(Box.createVerticalStrut(6));
		left.add(tag);
		left.add(Box.createVerticalStrut(18));
		left.add(photo);

		// ---------- the sign-in card ----------
		card = new JPanel() {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Theme.smooth(g);
				g2.setColor(new Color(0, 0, 0, 45));
				g2.fillRect(4, 6, getWidth() - 4, getHeight() - 6);
				g2.setColor(Color.WHITE);
				g2.fillRect(0, 0, getWidth() - 4, getHeight() - 6);
				g2.setColor(Theme.RED);
				g2.fillRect(0, 0, getWidth() - 4, 40);
				g2.setColor(Theme.PAPER);
				String header = Lang.t("signin_header");
				g2.setFont(Theme.safe(Theme.stencil(17f), header));
				g2.drawString(header, Theme.lineX(g2, header, 22, getWidth() - 48), 27);
				g2.dispose();
			}
		};
		card.setOpaque(false);
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBorder(new EmptyBorder(64, 26, 30, 30));
		card.setPreferredSize(new Dimension(440, 500));

		for (JLabel l : new JLabel[] { nameLabel, hostLabel }) {
			l.setForeground(Theme.MUTED);
		}
		hint.setForeground(Theme.MUTED);
		card.add(nameLabel);
		card.add(nameField);
		card.add(Box.createVerticalStrut(22));
		card.add(hostLabel);
		card.add(hostField);
		card.add(Box.createVerticalStrut(4));
		card.add(hint);
		card.add(Box.createVerticalStrut(26));
		go = new DossierButton("", DossierButton.RED, 20f, -1.5);
		go.setPreferredSize(new Dimension(320, 70));
		go.setMaximumSize(new Dimension(320, 70));
		card.add(go);
		card.add(Box.createVerticalStrut(10));
		error.setForeground(Theme.RED);
		card.add(error);
		card.add(Box.createVerticalStrut(14));
		hostButton = new DossierButton("", DossierButton.OUTLINE, 15f, 0);
		hostButton.setPreferredSize(new Dimension(320, 50));
		hostButton.setMaximumSize(new Dimension(320, 50));
		card.add(hostButton);
		card.add(Box.createVerticalStrut(4));
		hostHint.setForeground(Theme.MUTED);
		card.add(hostHint);

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 30, 0, 30);
		add(left, c);
		add(card, c);

		ActionListener submit = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// commas and colons would break the server's messages, so we remove them
				String name = nameField.getText().replace(",", "").replace(":", "").trim();
				if (name.length() == 0) {
					showError("err_name", "");
					nameField.requestFocusInWindow();
					return;
				}
				String host = hostField.getText().trim();
				if (host.length() == 0) {
					host = "localhost";
				}
				if (listener != null) {
					showError("", "");
					setBusy(true);
					listener.connect(name, host);
				}
			}
		};
		nameField.addActionListener(submit);
		hostField.addActionListener(submit);
		go.addActionListener(submit);
		hostButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String host = hostField.getText().trim();
				if (listener != null) {
					showError("", "");
					setBusy(true);
					listener.openHostScreen(host.length() == 0 ? "localhost" : host);
				}
			}
		});
		applyLanguage();
	}

	public void setConnectListener(ConnectListener l) { listener = l; }

	// errorKey is a Lang key; value fills in a %s (for example the host name)
	public void showError(String key, String value) {
		errorKey = key;
		errorValue = value;
		if (key.length() == 0) {
			error.setText(" ");
		} else {
			Theme.text(error, Theme.monoBold(14f), Lang.f(key, value));
		}
	}

	public void setCodename(String name) {
		nameField.setText(name);
		nameField.selectAll();
		nameField.requestFocusInWindow();
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
		go.setEnabled(!busy);
		hostButton.setEnabled(!busy);
		go.setText(Lang.t(busy ? "connecting" : "report_duty"));
	}

	public void applyLanguage() {
		tag.setFont(Theme.safe(Theme.type(19f), Lang.t("tagline")));
		tag.setText(Lang.html("tagline"));
		Theme.text(nameLabel, Theme.stencil(15f), Lang.t("codename"));
		Theme.text(hostLabel, Theme.stencil(15f), Lang.t("server_address"));
		Theme.text(hint, Theme.mono(13f), Lang.t("server_hint"));
		go.setText(Lang.t(busy ? "connecting" : "report_duty"));
		hostButton.setText(Lang.t("open_host"));
		hostHint.setFont(Theme.safe(Theme.mono(12f), Lang.t("host_hint")));
		hostHint.setText(Lang.html("host_hint"));
		showError(errorKey, errorValue);
		Lang.alignColumn(left);
		Lang.alignColumn(card);
		revalidate();
		repaint();
	}

	public void playIntro() {
		photo.dropIn();
		Anim.later(200, new Runnable() {
			public void run() {
				nameField.requestFocusInWindow();
			}
		});
	}

	private static JTextField field() {
		final JTextField f = new JTextField();
		f.setOpaque(false);
		f.setFont(Theme.type(24f));
		f.setForeground(Theme.INK);
		f.setCaretColor(Theme.RED);
		f.setBorder(new CompoundBorder(new MatteBorder(0, 0, 2, 0, Theme.INK), new EmptyBorder(6, 2, 4, 2)));
		f.setMaximumSize(new Dimension(2000, 46));
		// Arabic codenames: switch to the system font so letters show
		f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			void check() {
				final Font wanted = Theme.safe(Theme.type(24f), f.getText());
				if (!wanted.getFamily().equals(f.getFont().getFamily())) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() { f.setFont(wanted); }
					});
				}
			}
			public void insertUpdate(javax.swing.event.DocumentEvent e)  { check(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e)  { check(); }
			public void changedUpdate(javax.swing.event.DocumentEvent e) { }
		});
		return f;
	}
}
