/* =====================================================================
 *  Xp.java  -  the design kit of the game (Windows XP style)
 * ---------------------------------------------------------------------
 *  Everything about the LOOK lives here, so the other classes only
 *  say "make a title", "make a button", "draw a face":
 *
 *   [X1] Colors          the XP blue, the window beige, orange, green...
 *   [X2] Fonts           loaded from the fonts folder (.ttf files)
 *   [X3] Images          the wallpaper (images/wallpaper.png)
 *   [X4] Pixel art       small pictures stored as text (one letter = one
 *                        colored square): smiley, microphone, topic icons
 *   [X5] Helpers         labels, text fields, scroll panes, borders
 *   [X6] Pixel logo      draws "برا السالفة" as big square pixels
 *
 *  None of this changes how the game or the networking works.
 * ===================================================================== */

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.*;
import java.io.File;
import javax.imageio.ImageIO;

public class Xp {

	// =================================================================
	// [X1] COLORS
	// =================================================================
	public static final Color BLUE = new Color(0x0050EE);        // title bars, taskbar
	public static final Color BLUE_DARK = new Color(0x0831D9);   // window border
	public static final Color BLUE_TEXT = new Color(0x1D3F8F);   // headings inside windows
	public static final Color FACE = new Color(0xECE9D8);        // window body (XP beige)
	public static final Color WHITE = Color.WHITE;
	public static final Color FIELD_BORDER = new Color(0x7F9DB9); // around fields and lists
	public static final Color LINE = new Color(0xE3E0D2);        // thin lines between rows
	public static final Color INK = new Color(0x1B1838);         // normal text
	public static final Color GREY = new Color(0x6B6B6B);        // quiet text
	public static final Color ORANGE = new Color(0xF0782B);      // the main action + logo
	public static final Color ORANGE_DARK = new Color(0xD65F17);
	public static final Color ORANGE_SOFT = new Color(0xFFF1E6); // selected row / card
	public static final Color GREEN = new Color(0x3A9C35);       // start, progress, success
	public static final Color RED = new Color(0xE2532F);         // close button, wrong answer
	public static final Color NAVY = new Color(0x2B2763);        // behind the characters
	public static final Color BALLOON = new Color(0xFFFFE1);     // yellow notice balloon
	public static final Color CMD_BLACK = new Color(0x0C0C0C);   // server CMD window

	// colors used by the pixel art (one letter per color)
	public static final Color P_INK = new Color(0x1B1838);
	public static final Color P_CREAM = new Color(0xF4E7C8);
	public static final Color P_TEAL = new Color(0x2BB1A6);
	public static final Color P_TEAL_DARK = new Color(0x1A7D76);
	public static final Color P_ORANGE = new Color(0xF0782B);
	public static final Color P_ORANGE_DARK = new Color(0xB9531A);
	public static final Color P_YELLOW = new Color(0xF6C945);
	public static final Color P_PURPLE = new Color(0x6A4FC6);
	public static final Color P_PURPLE_DARK = new Color(0x4A3591);
	public static final Color P_PINK = new Color(0xE86F8F);

	// =================================================================
	// [X2] FONTS  (Google Fonts, free licenses)
	// =================================================================
	public static final Font TITLE = load("FiraSans-Bold.ttf", Font.BOLD);        // window titles
	public static final Font TITLE_ITALIC = load("FiraSans-BoldItalic.ttf", Font.BOLD); // "start"
	public static final Font UI = load("FiraSans-Regular.ttf", Font.PLAIN);       // taskbar, menus
	public static final Font PIXEL = load("PixelifySans.ttf", Font.PLAIN);        // headings, buttons, names
	public static final Font MONO = load("IBMPlexMono-Regular.ttf", Font.PLAIN);  // body text, chat
	public static final Font MONO_BOLD = load("IBMPlexMono-SemiBold.ttf", Font.BOLD);
	public static final Font WORD = load("PressStart2P-Regular.ttf", Font.PLAIN); // the secret word
	public static final Font CMD = load("VT323-Regular.ttf", Font.PLAIN);         // server log
	public static final Font ARABIC = load("Tajawal-Bold.ttf", Font.BOLD);        // the pixel logo "برا السالفة"
	// Arabic partners of the English fonts, chosen to keep the same feel:
	//   Pixelify Sans (headings)  ->  Changa (square, playful Kufi letters)
	//   IBM Plex Mono (text)      ->  IBM Plex Sans Arabic (same family)
	public static final Font AR_HEAD = load("Changa-Medium.ttf", Font.PLAIN);
	public static final Font AR_HEAD_BOLD = load("Changa-SemiBold.ttf", Font.BOLD);
	public static final Font AR_TEXT = load("IBMPlexSansArabic-Regular.ttf", Font.PLAIN);
	public static final Font AR_TEXT_BOLD = load("IBMPlexSansArabic-SemiBold.ttf", Font.BOLD);

	// loads a .ttf file from the fonts folder (a normal font if it is missing)
	private static Font load(String file, int fallbackStyle) {
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, new File("fonts", file));
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
			return font.deriveFont(14f);
		} catch (Exception ex) {
			return new Font("SansSerif", fallbackStyle, 14);
		}
	}

	// the font for a text: if the text has Arabic letters, the Arabic
	// partner of the English font (so Arabic keeps the same style)
	public static Font fontFor(String text, Font latin, float size) {
		if (hasArabic(text)) {
			return arabicFor(latin).deriveFont(size + 1);
		}
		return latin.deriveFont(size);
	}

	public static Font arabicFor(Font latin) {
		if (latin == PIXEL) {
			return AR_HEAD;                      // headings, names, buttons
		} else if (latin == WORD || latin == TITLE || latin == TITLE_ITALIC) {
			return AR_HEAD_BOLD;                 // big words, window titles
		} else if (latin == MONO_BOLD) {
			return AR_TEXT_BOLD;
		}
		return AR_TEXT;                          // normal text (MONO, UI, CMD)
	}

	// Some fonts have no arrow or triangle (like -> or >). Those letters
	// are drawn with the pixel "Press Start" font instead, which has them.
	// The text is cut in "runs": letters next to each other that the
	// font can (or cannot) draw.
	private static Font runFont(Font font, boolean has) {
		return has ? font : WORD.deriveFont(font.getSize2D() * 0.65f);
	}

	// the width of a text drawn with drawText
	public static int textWidth(Component c, String text, Font font) {
		int width = 0;
		int start = 0;
		for (int i = 1; i <= text.length(); i++) {
			boolean has = font.canDisplay(text.charAt(start));
			if (i == text.length() || font.canDisplay(text.charAt(i)) != has) {
				width = width + c.getFontMetrics(runFont(font, has)).stringWidth(text.substring(start, i));
				start = i;
			}
		}
		return width;
	}

	// draws a text; the letters the font does not have use the pixel font
	// (Arabic text: the runs go from right to left, so an arrow after
	//  the word ends up on the left side)
	public static void drawText(Component c, Graphics g, String text, Font font, int x, int y) {
		boolean rtl = hasArabic(text);
		if (rtl) {
			x = x + textWidth(c, text, font);           // start at the right end
		}
		int start = 0;
		for (int i = 1; i <= text.length(); i++) {
			boolean has = font.canDisplay(text.charAt(start));
			if (i == text.length() || font.canDisplay(text.charAt(i)) != has) {
				Font f = runFont(font, has);
				String run = text.substring(start, i);
				int w = c.getFontMetrics(f).stringWidth(run);
				g.setFont(f);
				if (rtl) {
					x = x - w;
					g.drawString(run, x, y - (has ? 0 : 1));
				} else {
					g.drawString(run, x, y - (has ? 0 : 1));
					x = x + w;
				}
				start = i;
			}
		}
		g.setFont(font);
	}

	// arrows that point the way we read: -> in English, <- in Arabic
	public static String nextArrow() {
		return Lang.arabic() ? "←" : "→";
	}

	public static String nextTriangle() {
		return Lang.arabic() ? "◀" : "▶";
	}

	public static String backTriangle() {
		return Lang.arabic() ? "▶" : "◀";
	}

	// Arabic: a right-to-left mark first, so a line that starts with an
	// English name still reads right to left ("Meryem يقول:")
	public static String rtlLine(String text) {
		return Lang.arabic() ? "\u200F" + text : text;
	}

	// English words inside an Arabic line keep their own order ("hi everyone!")
	public static String keepLtr(String text) {
		if (!Lang.arabic() || hasArabic(text)) {
			return text;
		}
		return "\u202A" + text + "\u202C";
	}

	public static boolean hasArabic(String text) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c >= 0x0600 && c <= 0x06FF) {
				return true;
			}
		}
		return false;
	}

	// =================================================================
	// [X3] IMAGES
	// =================================================================
	private static Image wallpaper = null;

	public static Image wallpaper() {
		if (wallpaper == null) {
			try {
				wallpaper = ImageIO.read(new File("images", "wallpaper.png"));
			} catch (Exception ex) {
				wallpaper = null;           // no file: the desktop paints a blue sky instead
			}
		}
		return wallpaper;
	}

	// =================================================================
	// [X4] PIXEL ART
	//  Each picture is an array of text rows. Every letter is one square:
	//    k ink   c cream   t teal   T dark teal   o orange   O dark orange
	//    y yellow   p purple   P dark purple   r pink   w white   . nothing
	// =================================================================
	public static final String[] SMILE = {
		"..kkkkkk..", ".kyyyyyyk.", "kyykyykyyk", "kyykyykyyk", "kyyyyyyyyk",
		"kykyyyykyk", "kyykkkkyyk", ".kyyyyyyk.", "..kkkkkk.." };
	public static final String[] MIC = {
		"..kk..", ".krrk.", ".krrk.", ".krrk.", "k.kk.k", ".k..k.", "..kk..", ".kkkk." };
	public static final String[] TV = {
		"kkkkkkkk", "kcccccck", "kcttttck", "kcttTTck", "kcccccck", "kkkkkkkk", "..kcck..", ".kkkkkk." };
	public static final String[] PAPER = {
		"kkkkkk..", "kcccckk.", "kckkcckk", "kcccccck", "kckkkkck", "kcccccck", "kckkkcck", "kkkkkkkk" };
	public static final String[] CHAT = {
		"..kkkk..", ".kcccck.", "kckcckck", "kcckkcck", "kcckkcck", "kckcckck", ".kcccck.", "..kkkk.." };

	// the topic icons (8 x 8). The topic name decides the picture.
	public static final String[] FOOD = {
		"..kkk...", ".kOOOk..", "kOooOOk.", "kOoOOOk.", ".kOOOkk.", "..kkkcck", ".....kck", "......k." };
	public static final String[] PLACES = {
		"...kk...", "..kook..", ".kooook.", "kkkkkkkk", ".kcckck.", ".kcckck.", ".kcckkk.", ".kkkkkk." };
	public static final String[] JOBS = {
		".kkkkk..", "kpppppk.", "kpkkkpk.", "...kk...", "...kk...", "...kk...", "...kk...", "...kk..." };
	public static final String[] ANIMALS = {
		".k....k.", "kok..kok", ".k.kk.k.", "..kook..", ".kooook.", ".kooook.", "..kkkk..", "........" };
	public static final String[] SPORTS = {
		"..kkkk..", ".kcccck.", "kckcckck", "kcckkcck", "kcckkcck", "kckcckck", ".kcccck.", "..kkkk.." };
	public static final String[] EVENTS = {
		".kk..kk.", "kook.kok", ".kkkkkk.", "krrrkrrk", "krrrkrrk", "kkkkkkkk", "krrrkrrk", "kkkkkkkk" };
	public static final String[] RANDOM = {
		".kkkkk..", "kyyyyyk.", "kykkkyk.", "...kyyk.", "..kyyk..", "..kyk...", "........", "..kyk..." };

	// which icon belongs to a topic name ("Food", "Places"...)
	public static String[] topicIcon(String topic) {
		String t = topic.toLowerCase();
		if (t.startsWith("food")) return FOOD;
		if (t.startsWith("place")) return PLACES;
		if (t.startsWith("job")) return JOBS;
		if (t.startsWith("animal")) return ANIMALS;
		if (t.startsWith("sport") || t.startsWith("activit")) return SPORTS;
		if (t.startsWith("event") || t.startsWith("occasion")) return EVENTS;
		return RANDOM;
	}

	// the color of one pixel-art letter
	public static Color pixelColor(char letter) {
		switch (letter) {
			case 'k': return P_INK;
			case 'c': return P_CREAM;
			case 't': return P_TEAL;
			case 'T': return P_TEAL_DARK;
			case 'o': return P_ORANGE;
			case 'O': return P_ORANGE_DARK;
			case 'y': return P_YELLOW;
			case 'p': return P_PURPLE;
			case 'P': return P_PURPLE_DARK;
			case 'r': return P_PINK;
			case 'w': return Color.WHITE;
			default:  return null;           // '.' = transparent
		}
	}

	// draws a picture: every letter becomes a px x px square
	public static void drawPixels(Graphics g, String[] rows, int x, int y, int px) {
		for (int row = 0; row < rows.length; row++) {
			for (int col = 0; col < rows[row].length(); col++) {
				Color c = pixelColor(rows[row].charAt(col));
				if (c != null) {
					g.setColor(c);
					g.fillRect(x + col * px, y + row * px, px, px);
				}
			}
		}
	}

	// a picture as an Icon (for JLabels and buttons)
	public static Icon pixelIcon(final String[] rows, final int px) {
		return new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				drawPixels(g, rows, x, y, px);
			}
			public int getIconWidth()  { return rows[0].length() * px; }
			public int getIconHeight() { return rows.length * px; }
		};
	}

	// =================================================================
	// [X5] HELPERS
	// =================================================================

	// ONE CLICK = ONE ACTION.
	// mouseClicked only fires if the mouse does not move AT ALL between
	// press and release, so on a laptop touchpad many clicks were lost and
	// people had to click twice. Here the action runs when the button is
	// released over the component, even if the mouse moved a little.
	public static void onClick(final Component c, final java.awt.event.ActionListener action) {
		c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.addMouseListener(new java.awt.event.MouseAdapter() {
			private boolean down = false;
			public void mousePressed(java.awt.event.MouseEvent e) {
				down = SwingUtilities.isLeftMouseButton(e);
			}
			public void mouseReleased(java.awt.event.MouseEvent e) {
				if (down && c.isEnabled() && c.contains(e.getPoint())) {
					action.actionPerformed(new java.awt.event.ActionEvent(c, 0, "click"));
				}
				down = false;
			}
		});
	}

	// Arabic is read from right to left: the whole window is mirrored
	// (title on the right, lists on the right...). English = left to right.
	public static void orient(Component c) {
		c.applyComponentOrientation(Lang.arabic() ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
	}

	// the start of a BoxLayout column: the left edge, or the right edge in Arabic
	public static float start() {
		return Lang.arabic() ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT;
	}

	public static boolean rtl(Component c) {
		return !c.getComponentOrientation().isLeftToRight();
	}

	// a text label in one of the kit fonts
	public static JLabel label(String text, Font font, float size, Color color) {
		JLabel label = new JLabel(hasArabic(text) ? rtlLine(text) : text);
		label.setFont(fontFor(text, font, size));
		label.setForeground(color);
		return label;
	}

	// a white text field with the thin XP blue-grey border
	public static JTextField field(String text) {
		JTextField field = new JTextField(text);
		field.setFont(MONO.deriveFont(17f));
		field.setForeground(INK);
		field.setBackground(Color.WHITE);
		field.setCaretColor(INK);
		field.setBorder(new CompoundBorder(new LineBorder(FIELD_BORDER, 2), new EmptyBorder(6, 10, 6, 10)));
		return field;
	}

	// the white "sunken" box used for lists, chat and the suspects
	public static Border whiteBox() {
		return new LineBorder(FIELD_BORDER, 2);
	}

	// a transparent panel (only its children are painted)
	public static JPanel clear(LayoutManager layout) {
		JPanel panel = new JPanel(layout);
		panel.setOpaque(false);
		return panel;
	}

	// a scroll pane with no extra border
	public static JScrollPane scroll(Component view) {
		JScrollPane scroll = new JScrollPane(view);
		scroll.setBorder(whiteBox());
		scroll.getVerticalScrollBar().setUnitIncrement(12);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return scroll;
	}

	// smooth (anti-aliased) drawing for curves and text
	public static Graphics2D smooth(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g2;
	}

	// =================================================================
	// [X6] PIXEL LOGO  "برا السالفة" drawn with big square pixels
	//  1. write the text small (size 30) on an empty picture
	//  2. every pixel is either fully colored or empty (no soft edges)
	//  3. the picture is drawn bigger, so every pixel becomes a square
	// =================================================================
	public static ImageIcon pixelText(String text, int fontSize, int scale, Color color, Color shadow) {
		Font font = ARABIC.deriveFont((float) fontSize);
		// measure the text
		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D pg = probe.createGraphics();
		FontMetrics fm = pg.getFontMetrics(font);
		int w = fm.stringWidth(text) + 4;
		int h = (int) (fontSize * 1.6);
		pg.dispose();

		// 1. draw the text (and its shadow one pixel lower)
		BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = small.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(font);
		int baseline = (h + fm.getAscent() - fm.getDescent()) / 2;
		if (shadow != null) {
			g.setColor(shadow);
			g.drawString(text, 1, baseline + 1);
		}
		g.setColor(color);
		g.drawString(text, 2, baseline);
		g.dispose();

		// 2. hard edges: a pixel is visible only if it is more than ~half covered
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = small.getRGB(x, y);
				int alpha = (argb >>> 24);
				if (alpha > 110) {
					small.setRGB(x, y, argb | 0xFF000000);
				} else {
					small.setRGB(x, y, 0);
				}
			}
		}

		// 3. draw it "scale" times bigger with no smoothing = square pixels
		BufferedImage big = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_ARGB);
		Graphics2D bg = big.createGraphics();
		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		bg.drawImage(small, 0, 0, w * scale, h * scale, null);
		bg.dispose();
		return new ImageIcon(big);
	}
}
