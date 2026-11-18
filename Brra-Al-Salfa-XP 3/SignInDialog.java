/* =====================================================================
 *  SignInDialog.java  -  the first pop-up: "Your name" + "Server"
 * ---------------------------------------------------------------------
 *  This is the nickname pop-up of the brief, in the XP style:
 *     [ the pixel logo  برا السالفة ]
 *     Everyone knows the word. Except one.
 *     Sign in:  Your name [______]   Server [______]
 *     [How to play]  [Next ->]
 *  It is a JDialog (see XpDialog), not a window inside the desktop.
 *  "Next" checks the name and tells SpyClient through the Listener.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class SignInDialog {

	// what the dialog asks SpyClient to do
	public interface Listener {
		void next(String name, String server);
		void help();
	}

	private final XpWindow window = new XpWindow(Lang.t("game"), Xp.pixelIcon(Xp.SMILE, 2));
	private final XpDialog dialog;
	private final Desktop desktop;
	private final JTextField nameField = Xp.field("");
	private final XpParts.HintField serverField = new XpParts.HintField(Lang.t("server_hint"));

	public SignInDialog(Desktop theDesktop, String name, String server, final Listener listener) {
		desktop = theDesktop;
		window.helpMenu();
		JPanel body = window.body();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBorder(new EmptyBorder(14, 18, 16, 18));

		// ---- the logo and the tagline ----
		JLabel logo = new JLabel(Xp.pixelText("برا السالفة", 22, 4, Xp.ORANGE, Xp.ORANGE_DARK));
		JLabel tagline = Xp.label(Lang.t("tagline"), Xp.PIXEL, 15f, Xp.BLUE_TEXT);
		logo.setAlignmentX(Component.CENTER_ALIGNMENT);
		tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
		body.add(logo);
		body.add(Box.createVerticalStrut(6));
		body.add(tagline);
		body.add(Box.createVerticalStrut(16));

		// ---- the "Sign in" group: a titled border around the two fields ----
		JPanel group = Xp.clear(new GridLayout(4, 1, 0, 6));
		TitledBorder titled = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0xD0D0BF), 1, true), " " + Lang.t("sign_in") + " ");
		titled.setTitleFont(Xp.fontFor(Lang.t("sign_in"), Xp.PIXEL, 15f));
		titled.setTitleColor(Xp.BLUE_TEXT);
		group.setBorder(new CompoundBorder(titled, new EmptyBorder(4, 14, 14, 14)));
		nameField.setText(name);
		serverField.setText(server);
		group.add(Xp.label(Lang.t("your_name"), Xp.PIXEL, 16f, Xp.INK));
		group.add(nameField);
		group.add(Xp.label(Lang.t("server"), Xp.PIXEL, 16f, Xp.INK));
		group.add(serverField);
		group.setAlignmentX(Component.CENTER_ALIGNMENT);
		body.add(group);
		body.add(Box.createVerticalStrut(16));

		// ---- the two buttons ----
		XpButton help = new XpButton(Lang.t("i_help"), XpButton.NORMAL);
		XpButton next = new XpButton(Lang.t("next") + " " + Xp.nextArrow(), XpButton.GO);
		JPanel buttons = Xp.clear(new GridLayout(1, 2, 10, 0));
		buttons.add(help);
		buttons.add(next);
		buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
		body.add(buttons);

		// "Next" (or Enter in a field): check the name, then tell SpyClient
		ActionListener nextAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// , = > | : are used by the messages, so they are removed
				String typed = nameField.getText().trim().replaceAll("[,=>|:#]", "");
				if (typed.length() > 15) {
					typed = typed.substring(0, 15);
				}
				if (typed.length() == 0) {
					nameField.requestFocusInWindow();
					window.setActive(false);
					ErrorDialog.show(desktop, Lang.t("sign_in"),
							Lang.t("err_name"), "");
					window.setActive(true);
					return;
				}
				listener.next(typed, serverField.getText().trim());
			}
		};
		next.addActionListener(nextAction);
		nameField.addActionListener(nextAction);
		serverField.addActionListener(nextAction);
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listener.help();
			}
		});

		// the X button of the title bar closes the game
		window.setCloseListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		dialog = new XpDialog(desktop, window, false);
	}

	public void show() {
		window.setActive(true);
		dialog.showAt(300, 90, 480, 0);
		nameField.requestFocusInWindow();
	}

	public void hide() {
		dialog.setVisible(false);
	}

	public void dispose() {
		dialog.dispose();
	}

	public void setActive(boolean on) {
		window.setActive(on);
	}

	// the text typed in the Server field (used by "Who is online")
	public String server() {
		return serverField.getText().trim();
	}

	public String name() {
		return nameField.getText().trim();
	}
}
