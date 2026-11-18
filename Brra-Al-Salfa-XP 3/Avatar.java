/* =====================================================================
 *  Avatar.java  -  the pixel characters (bonus: character picker)
 * ---------------------------------------------------------------------
 *  A character is 4 numbers written as "face-hair-shirt-back", e.g. "4-2-0-3":
 *    face  = which of the 16 characters (face shape + skin color)
 *    hair  = which of the 6 hair colors
 *    shirt = which of the 6 shirt colors
 *    back  = which of the 6 background colors (behind the face)
 *  The client sends it to the server as "AVATAR:4-2-0-3", and the server
 *  tells everyone with "AVATARS:Meryem=4-2-0-3,Nouri=1-1-1-0".
 *
 *  A face is a 10 x 10 pixel picture. The letters H (hair), C (shirt)
 *  and s (skin) get their color from the character's numbers.
 * ===================================================================== */

import java.awt.*;
import javax.swing.*;

public class Avatar {

	public static final int COUNT = 16;      // how many characters to pick from

	// two face shapes: short hair and long hair
	private static final String[] SHORT_HAIR = {
		"..kkkkkk..", ".kHHHHHHk.", "kHHHHHHHHk", "kHssssssHk", "kskssssksk",
		"kssssssssk", "ksssrrsssk", ".kssssssk.", "..kCCCCk..", ".kCCCCCCk." };
	private static final String[] LONG_HAIR = {
		"..kkkkkk..", ".kHHHHHHk.", "kHHHHHHHHk", "kHssssssHk", "kHkssssksH",
		"kHssssssHk", "kHssrrssHk", "kHkssssHkk", "..kCCCCk..", ".kCCCCCCk." };
	// the "?" face of Brra Al-Salfa (nobody knows who it is)
	private static final String[] MYSTERY = {
		"..kkkkkk..", ".kPPPPPPk.", "kPPyyyyPPk", "kPyPPPPyPk", "kPPPPPyPPk",
		"kPPPPyPPPk", "kPPPPyPPPk", ".kPPPPPPk.", "..kPyPPk..", ".kPPPPPPk." };

	public static final Color[] HAIR = {
		new Color(0x3A2A1E), new Color(0x1B1838), new Color(0x5A3A22),
		new Color(0xB9531A), new Color(0xD9A93A), new Color(0x2B2763) };
	public static final Color[] SHIRT = {
		new Color(0xF0782B), new Color(0x2BB1A6), new Color(0x6A4FC6),
		new Color(0xF6C945), new Color(0xE86F8F), new Color(0x3A9C35) };
	// the square behind the face (navy is the first one, the default)
	public static final Color[] BACK = {
		new Color(0x2B2763), new Color(0x1A7D76), new Color(0x4A3591),
		new Color(0x9C2F5A), new Color(0x3F7A28), new Color(0x2F6FD0) };
	private static final Color[] SKIN = {
		new Color(0xE9B48A), new Color(0xC98E62), new Color(0xF3C9A3), new Color(0xA86F4A) };

	public int face, hair, shirt, back;

	public Avatar(int face, int hair, int shirt) {
		this(face, hair, shirt, 0);
	}

	public Avatar(int face, int hair, int shirt, int back) {
		this.face = face;
		this.hair = hair;
		this.shirt = shirt;
		this.back = back;
	}

	// the 16 ready-made characters (each one has its own colors)
	public static Avatar preset(int i) {
		return new Avatar(i, (i + (i >> 3)) % HAIR.length, (i * 5 + (i >> 2)) % SHIRT.length);
	}

	// "4-2-0-3" -> Avatar   (a bad text gives character 0)
	public static Avatar parse(String code) {
		try {
			String[] parts = code.trim().split("-");
			int f = Integer.parseInt(parts[0]) % COUNT;
			int h = Integer.parseInt(parts[1]) % HAIR.length;
			int s = Integer.parseInt(parts[2]) % SHIRT.length;
			int b = parts.length > 3 ? Integer.parseInt(parts[3]) % BACK.length : 0;
			return new Avatar(f, h, s, b);
		} catch (Exception ex) {
			return preset(0);
		}
	}

	// Avatar -> "4-2-0-3"
	public String code() {
		return face + "-" + hair + "-" + shirt + "-" + back;
	}

	// a player who has not chosen yet gets a character from their name
	public static Avatar forName(String name) {
		int sum = 0;
		for (int i = 0; i < name.length(); i++) {
			sum = sum + name.charAt(i);
		}
		return preset(sum % COUNT);
	}

	// draws the face: every letter is a px x px square
	public void draw(Graphics g, int x, int y, int px) {
		String[] rows = (face % 2 == 0) ? LONG_HAIR : SHORT_HAIR;
		Color skin = SKIN[(face >> 1) % SKIN.length];
		for (int row = 0; row < rows.length; row++) {
			for (int col = 0; col < rows[row].length(); col++) {
				char letter = rows[row].charAt(col);
				Color c;
				if (letter == 'H') c = HAIR[hair];
				else if (letter == 'C') c = SHIRT[shirt];
				else if (letter == 's') c = skin;
				else c = Xp.pixelColor(letter);
				if (c != null) {
					g.setColor(c);
					g.fillRect(x + col * px, y + row * px, px, px);
				}
			}
		}
	}

	public static void drawMystery(Graphics g, int x, int y, int px) {
		Xp.drawPixels(g, MYSTERY, x, y, px);
	}

	// =================================================================
	// Portrait: the face on its navy square with a white frame.
	// ring = true draws the orange frame (selected / asking now).
	// A null avatar draws the "?" face.
	// =================================================================
	public static class Portrait extends JComponent {
		private Avatar avatar;
		private int px;
		private boolean ring;

		public Portrait(Avatar avatar, int size, boolean ring) {
			this.avatar = avatar;
			this.ring = ring;
			px = Math.max(1, Math.round(size / 11f));
			int full = px * 10 + 2 * (px + 3) + 4;
			setPreferredSize(new Dimension(full, full));
			setMinimumSize(getPreferredSize());
			setMaximumSize(getPreferredSize());
		}

		public void setAvatar(Avatar a) { avatar = a; repaint(); }
		public void setRing(boolean on)  { ring = on; repaint(); }

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Xp.smooth(g);
			int full = px * 10 + 2 * (px + 3) + 4;
			int ox = (getWidth() - full) / 2;
			int oy = (getHeight() - full) / 2;
			// thin blue outline, then the white (or orange) frame, then navy
			g2.setColor(Xp.BLUE_TEXT);
			g2.fillRoundRect(ox, oy, full, full, 9, 9);
			g2.setColor(ring ? Xp.ORANGE : Color.WHITE);
			g2.fillRoundRect(ox + 1, oy + 1, full - 2, full - 2, 8, 8);
			g2.setColor(avatar == null ? Xp.NAVY : BACK[avatar.back]);   // the chosen background
			g2.fillRoundRect(ox + 4, oy + 4, full - 8, full - 8, 6, 6);
			g2.dispose();
			int fx = ox + 4 + px + 1;
			int fy = oy + 4 + px + 1;
			if (avatar == null) {
				drawMystery(g, fx, fy, px);
			} else {
				avatar.draw(g, fx, fy, px);
			}
		}
	}

	// a small face as an Icon (for lists and chat)
	public Icon icon(final int px) {
		final Avatar me = this;
		return new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) { me.draw(g, x, y, px); }
			public int getIconWidth()  { return 10 * px; }
			public int getIconHeight() { return 10 * px; }
		};
	}
}
