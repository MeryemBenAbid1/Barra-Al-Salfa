/* =====================================================================
 *  CharacterWindow.java  -  "Choose your character"  (bonus feature)
 * ---------------------------------------------------------------------
 *  LEFT  (task pane): the big preview of your character + the colors
 *                     (shirt, hair and background)
 *  RIGHT (white)    : 16 characters to click (4 x 4)
 *  A character is saved as a short code "face-hair-shirt-back" (e.g.
 *  "4-2-0-3", see Avatar.java). After joining, SpyClient sends "AVATAR:4-2-0-3" and
 *  the server tells everyone, so every screen draws the same face.
 *  Characters already used by players in the lobby are hidden.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class CharacterWindow extends XpWindow {

	public interface Listener {
		void back();
		void join(Avatar chosen);
	}

	private Avatar chosen;
	private final Avatar.Portrait big;
	private final Avatar.Portrait[] faces = new Avatar.Portrait[Avatar.COUNT];
	private final JPanel[] cells = new JPanel[Avatar.COUNT];
	private final JPanel shirtRow = Xp.clear(new FlowLayout(FlowLayout.LEADING, 6, 0));
	private final JPanel hairRow = Xp.clear(new FlowLayout(FlowLayout.LEADING, 6, 0));
	private final JPanel backRow = Xp.clear(new FlowLayout(FlowLayout.LEADING, 6, 0));
	private static final int SHIRT = 0, HAIR = 1, BACK = 2;     // which color row
	private PreviewWindow preview;

	public CharacterWindow(String name, int playerNumber, Avatar start, Set<Integer> takenFaces, final Listener listener) {
		super(Lang.t("choose_char"), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(0, 0, 10, 0));
		chosen = start;

		// =============================================================
		// LEFT: your character + colors
		// =============================================================
		XpParts.TaskPane pane = new XpParts.TaskPane(250);
		XpParts.Section mine = new XpParts.Section(Lang.t("your_char"));
		JPanel mineBox = Xp.clear(null);
		mineBox.setLayout(new BoxLayout(mineBox, BoxLayout.Y_AXIS));
		mineBox.setBorder(new EmptyBorder(14, 10, 14, 10));
		big = new Avatar.Portrait(chosen, 150, true);
		big.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel nameLabel = Xp.label(name, Xp.PIXEL, 22f, Xp.INK);
		JLabel number = Xp.label(Lang.f("player_n", playerNumber), Xp.MONO, 14f, Xp.GREY);
		nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		number.setAlignmentX(Component.CENTER_ALIGNMENT);
		mineBox.add(big);
		mineBox.add(Box.createVerticalStrut(10));
		mineBox.add(nameLabel);
		mineBox.add(number);
		mine.content().add(mineBox, BorderLayout.CENTER);
		pane.addSection(mine);

		XpParts.Section colors = new XpParts.Section(Lang.t("colors"));
		JPanel colorBox = Xp.clear(new GridLayout(6, 1, 0, 4));
		colorBox.setBorder(new EmptyBorder(10, 10, 12, 10));
		colorBox.add(Xp.label(Lang.t("shirt"), Xp.MONO, 14f, Xp.GREY));
		colorBox.add(shirtRow);
		colorBox.add(Xp.label(Lang.t("hair"), Xp.MONO, 14f, Xp.GREY));
		colorBox.add(hairRow);
		colorBox.add(Xp.label(Lang.t("background"), Xp.MONO, 14f, Xp.GREY));
		colorBox.add(backRow);
		colors.content().add(colorBox, BorderLayout.CENTER);
		pane.addSection(colors);
		body().add(pane, BorderLayout.LINE_START);

		// =============================================================
		// RIGHT: the 16 characters
		// =============================================================
		JPanel right = XpParts.whiteArea(18, 20);
		right.setLayout(new BorderLayout(0, 14));
		right.add(new XpParts.Heading(Lang.t("pick_char")), BorderLayout.NORTH);

		JPanel grid = Xp.clear(new GridLayout(4, 4, 10, 10));
		for (int i = 0; i < Avatar.COUNT; i++) {
			final int index = i;
			// each character sits in a small panel that turns blue when selected
			cells[i] = new JPanel(new BorderLayout());
			cells[i].setBackground(Color.WHITE);
			cells[i].setBorder(new EmptyBorder(4, 4, 4, 4));
			faces[i] = new Avatar.Portrait(Avatar.preset(i), 80, false);
			Xp.onClick(faces[i], new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Avatar p = Avatar.preset(index);
					select(new Avatar(index, p.hair, p.shirt, chosen.back));   // keep the background
				}
			});
			cells[i].add(faces[i], BorderLayout.CENTER);
			JPanel holder = Xp.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
			holder.add(cells[i]);
			// a character used by another player is hidden (its place stays empty)
			if (takenFaces.contains(i)) {
				cells[i].setVisible(false);
			}
			grid.add(holder);
		}
		right.add(grid, BorderLayout.CENTER);

		// bottom: hint + Back + Join lobby
		XpButton back = new XpButton(Lang.t("back"), XpButton.NORMAL);
		XpButton join = new XpButton(Lang.t("join_lobby") + " " + Xp.nextArrow(), XpButton.GO);
		JPanel buttons = Xp.clear(new FlowLayout(FlowLayout.TRAILING, 10, 0));
		buttons.add(back);
		buttons.add(join);
		JPanel bottom = Xp.clear(new BorderLayout());
		bottom.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, Xp.LINE), new EmptyBorder(12, 0, 0, 0)));
		bottom.add(XpParts.hint(Lang.t("taken_hidden")), BorderLayout.LINE_START);
		bottom.add(buttons, BorderLayout.LINE_END);
		right.add(bottom, BorderLayout.SOUTH);
		body().add(right, BorderLayout.CENTER);

		back.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listener.back();
			}
		});
		join.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listener.join(chosen);
			}
		});

		preview = new PreviewWindow(name);
		select(chosen);
	}

	public PreviewWindow preview() {
		return preview;
	}

	// a character (or a color) was clicked: update everything
	private void select(Avatar a) {
		chosen = a;
		big.setAvatar(a);
		for (int i = 0; i < Avatar.COUNT; i++) {
			boolean on = (i == a.face);
			Avatar p = Avatar.preset(i);
			faces[i].setAvatar(on ? a : p);
			faces[i].setRing(on);
			cells[i].setBackground(on ? new Color(0x316AC5) : Color.WHITE);   // XP selection blue
		}
		makeSwatches(shirtRow, Avatar.SHIRT, a.shirt, SHIRT);
		makeSwatches(hairRow, Avatar.HAIR, a.hair, HAIR);
		makeSwatches(backRow, Avatar.BACK, a.back, BACK);
		preview.setColor(Avatar.SHIRT[a.shirt]);
	}

	// one small square per color; the chosen one has a dark frame
	private void makeSwatches(JPanel row, Color[] colors, int selected, final int which) {
		row.removeAll();
		for (int i = 0; i < colors.length; i++) {
			final int index = i;
			JLabel swatch = new JLabel();
			swatch.setOpaque(true);
			swatch.setBackground(colors[i]);
			swatch.setPreferredSize(new Dimension(24, 24));
			swatch.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(i == selected ? Xp.INK : new Color(0xBDBDBD), i == selected ? 2 : 1),
					BorderFactory.createLineBorder(Color.WHITE, 1)));
			Xp.onClick(swatch, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (which == SHIRT) {
						select(new Avatar(chosen.face, chosen.hair, index, chosen.back));
					} else if (which == HAIR) {
						select(new Avatar(chosen.face, index, chosen.shirt, chosen.back));
					} else {
						select(new Avatar(chosen.face, chosen.hair, chosen.shirt, index));
					}
				}
			});
			row.add(swatch);
		}
		Xp.orient(row);                  // Arabic: the squares go right to left
		row.revalidate();
		row.repaint();
	}

	// =================================================================
	// The small "Preview" window: how your name looks in the chat
	// =================================================================
	public static class PreviewWindow extends XpWindow {
		private final JLabel says;

		public PreviewWindow(String name) {
			super(Lang.t("preview"), Xp.pixelIcon(Xp.CHAT, 2));
			JPanel box = XpParts.listBox(new GridLayout(2, 1));
			box.setBorder(BorderFactory.createCompoundBorder(Xp.whiteBox(), new EmptyBorder(8, 10, 8, 10)));
			says = Xp.label(name + " " + Lang.t("says"), Xp.MONO, 14f, Xp.ORANGE_DARK);
			box.add(says);
			box.add(Xp.label(Xp.keepLtr("  hi everyone!"), Xp.MONO, 14f, Xp.INK));
			body().add(box, BorderLayout.CENTER);
			JLabel hint = new JLabel(XpParts.wrap(Lang.t("preview_text"), 230));
			hint.setFont(Xp.fontFor(Lang.t("preview_text"), Xp.MONO, 13f));
			hint.setForeground(Xp.GREY);
			hint.setBorder(new EmptyBorder(6, 0, 0, 0));
			body().add(hint, BorderLayout.SOUTH);
		}

		// your name is shown in the color of your shirt
		public void setColor(Color c) {
			says.setForeground(c.darker());
		}
	}
}
