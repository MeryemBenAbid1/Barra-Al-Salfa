/* =====================================================================
 *  FilePopup.java  -  the drawer of files pops up for the host
 * ---------------------------------------------------------------------
 *  When the room is created (this player / screen becomes the host),
 *  the screen gets darker and the drawer (FileCabinet) slides up from
 *  the bottom. The host clicks a tag; when the server's PICKED message
 *  comes back, the CHOSEN stamp lands and the pop-up slides away.
 *
 *  It lies ON TOP of the lobby: FilePopup.over(lobby, popup) puts the
 *  two in one panel, the pop-up first (in Swing the first child is
 *  painted on top). While it is open it catches every mouse click, so
 *  nothing behind it can be pressed.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FilePopup extends JPanel {

	private final FileCabinet cabinet = new FileCabinet();
	private final JLabel title = new JLabel();
	private final DossierButton closeButton = new DossierButton("", DossierButton.OUTLINE, 14f, 0);
	private final JPanel card;
	private float shown = 0f;          // 0 = hidden, 1 = fully open
	private boolean open = false;

	public FilePopup() {
		setOpaque(false);
		setLayout(null);                 // we place the card ourselves (doLayout)
		setVisible(false);

		// the paper card in the middle
		card = new JPanel(new BorderLayout(0, 10)) {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Theme.smooth(g);
				g2.setColor(Theme.FOLDER);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
				g2.setColor(Theme.PAPER);
				g2.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 12, 12);
				g2.dispose();
			}
		};
		card.setOpaque(false);
		card.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		title.setForeground(Theme.INK);
		closeButton.setPreferredSize(new Dimension(110, 36));
		top.add(title, BorderLayout.LINE_START);
		top.add(closeButton, BorderLayout.LINE_END);
		card.add(top, BorderLayout.NORTH);
		card.add(cabinet, BorderLayout.CENTER);
		add(card);

		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		// eat the clicks so the lobby behind cannot be pressed
		addMouseListener(new MouseAdapter() { });
		applyLanguage();
	}

	public FileCabinet getCabinet() { return cabinet; }

	public boolean isOpen() { return open; }

	public void applyLanguage() {
		String text = Lang.t("choose_file");
		Theme.text(title, Theme.stencil(24f), Lang.rtl() ? text : text.toUpperCase());
		closeButton.setText(Lang.t("close"));
		applyComponentOrientation(Lang.orientation());
		cabinet.repaint();
	}

	// slide up and open the drawer
	public void open() {
		if (open) {
			return;
		}
		open = true;
		setVisible(true);
		cabinet.setClickable(true);
		cabinet.dealIn();
		Anim.run(380, new Anim.Step() {
			public void update(float t) {
				shown = Anim.easeOut(t);
				doLayout();
				repaint();
			}
		}, null);
	}

	// slide down and hide
	public void close() {
		if (!open) {
			return;
		}
		open = false;
		cabinet.setClickable(false);
		Anim.run(300, new Anim.Step() {
			public void update(float t) {
				shown = 1f - Anim.easeOut(t);
				doLayout();
				repaint();
			}
		}, new Runnable() {
			public void run() {
				if (!open) {
					setVisible(false);
				}
			}
		});
	}

	// PICKED message: stamp the file, then close by itself
	public void setPicked(String name) {
		cabinet.setPicked(name);
		if (open) {
			Anim.later(1300, new Runnable() {
				public void run() {
					close();
				}
			});
		}
	}

	// the card is centered and slides in from below
	public void doLayout() {
		int w = getWidth();
		int h = getHeight();
		int cw = Math.min(w - 40, 820);
		int ch = Math.min(h - 30, 470);
		int y = (h - ch) / 2 + (int) ((1f - shown) * h * 0.7f);
		card.setBounds((w - cw) / 2, y, cw, ch);
		card.validate();
	}

	// the dark see-through backdrop
	protected void paintComponent(Graphics g) {
		g.setColor(new Color(18, 13, 9, (int) (170 * shown)));
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	// ---------------------------------------------------------------
	// puts the pop-up on top of a screen (both get the whole space)
	// ---------------------------------------------------------------
	public static JPanel over(final JComponent screen, final FilePopup popup) {
		JPanel stack = new JPanel(null) {
			// children overlap, so Swing must not take drawing shortcuts
			public boolean isOptimizedDrawingEnabled() {
				return false;
			}
			public void doLayout() {
				popup.setBounds(0, 0, getWidth(), getHeight());
				screen.setBounds(0, 0, getWidth(), getHeight());
			}
			public Dimension getPreferredSize() {
				return screen.getPreferredSize();
			}
		};
		stack.setOpaque(false);
		stack.add(popup);      // first child = painted on top
		stack.add(screen);
		return stack;
	}
}
