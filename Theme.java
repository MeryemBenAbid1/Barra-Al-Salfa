/* =====================================================================
 *  Theme.java  -  colors, fonts, images and sounds for "Al-Dakheel"
 * ---------------------------------------------------------------------
 *  Everything is loaded from folders next to the .java files:
 *     fonts/   the .ttf files (if one is missing we use a normal font)
 *     images/  optional .png images  (mascot.png, mugshot.png, evidence.png)
 *     sounds/  optional .wav sounds  (stamp.wav, type.wav, tick.wav, ...)
 *  A missing image shows a dashed placeholder, a missing sound is silent,
 *  so the game always runs.
 * ===================================================================== */

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public final class Theme {

	// ---------------- colors (same as the design canvas) ----------------
	public static final Color DESK       = new Color(42, 32, 25);    // dark wooden desk
	public static final Color SHADOW     = new Color(18, 13, 9);     // shadow under the folder
	public static final Color FOLDER     = new Color(217, 196, 154); // manila folder
	public static final Color PAPER      = new Color(242, 234, 218); // the paper inside
	public static final Color PAPER_DARK = new Color(228, 220, 201); // placeholders, highlights
	public static final Color CARD_EDGE  = new Color(200, 187, 160); // edge of white cards
	public static final Color INK        = new Color(31, 27, 22);    // main text
	public static final Color INK_SOFT   = new Color(59, 48, 36);    // photo background
	public static final Color MUTED      = new Color(94, 82, 66);    // small grey-brown text
	public static final Color FAINT      = new Color(140, 127, 106); // dashed lines
	public static final Color RED        = new Color(179, 38, 30);   // stamps, danger
	public static final Color RED_LIGHT  = new Color(255, 107, 99);  // timer in the last seconds
	public static final Color RULE_BLUE  = new Color(220, 230, 242); // index-card lines

	// ---------------- fonts ----------------
	private static final Font STENCIL = load("BlackOpsOne-Regular.ttf", Font.SANS_SERIF, Font.BOLD);
	private static final Font TYPE    = load("SpecialElite-Regular.ttf", Font.MONOSPACED, Font.PLAIN);
	private static final Font MONO    = load("CourierPrime-Regular.ttf", Font.MONOSPACED, Font.PLAIN);
	private static final Font MONO_B  = load("CourierPrime-Bold.ttf", Font.MONOSPACED, Font.BOLD);
	private static final Font ARABIC  = load("Lalezar-Regular.ttf", Font.DIALOG, Font.BOLD);

	public static Font stencil(float size) { return STENCIL.deriveFont(size); } // titles, stamps
	public static Font type(float size)    { return TYPE.deriveFont(size); }    // typewriter
	public static Font mono(float size)    { return MONO.deriveFont(size); }    // normal text
	public static Font monoBold(float size){ return MONO_B.deriveFont(size); }
	public static Font arabic(float size)  { return ARABIC.deriveFont(size); }  // Arabic titles

	// our English fonts have no Arabic letters. If the text has a letter
	// the font cannot draw: titles/stamps use Lalezar (Arabic display font),
	// normal text uses the system font.
	public static Font safe(Font font, String text) {
		if (text == null || font.canDisplayUpTo(text) == -1) {
			return font;
		}
		if (font.getFamily().equals(STENCIL.getFamily()) && ARABIC.canDisplayUpTo(text) == -1) {
			return ARABIC.deriveFont(font.getSize2D() * 1.1f);
		}
		return new Font(Font.DIALOG, Font.BOLD, font.getSize());
	}

	// sets a label's text and switches to an Arabic-capable font if needed
	public static void text(javax.swing.JLabel label, Font base, String text) {
		label.setFont(safe(base, text));
		label.setText(text);
	}

	// x position for text inside a box: left in English, right in Arabic
	public static int lineX(Graphics2D g2, String text, int x, int width) {
		if (Lang.rtl()) {
			return x + width - g2.getFontMetrics().stringWidth(text);
		}
		return x;
	}

	private static Font load(String file, String fallbackName, int fallbackStyle) {
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, new File("fonts", file));
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
			return font.deriveFont(12f);
		} catch (Exception ex) {
			System.out.println("Font not found: fonts/" + file + " (using " + fallbackName + ")");
			return new Font(fallbackName, fallbackStyle, 12);
		}
	}

	// ---------------- images ----------------
	private static final HashMap<String, BufferedImage> images = new HashMap<String, BufferedImage>();

	// returns images/<name>.png, or null if the file is not there
	public static BufferedImage image(String name) {
		if (images.containsKey(name)) {
			return images.get(name);
		}
		BufferedImage img = null;
		try {
			File file = new File("images", name + ".png");
			if (file.exists()) {
				img = ImageIO.read(file);
			}
		} catch (Exception ex) {
			System.out.println("Could not read images/" + name + ".png");
		}
		images.put(name, img);
		return img;
	}

	// ---------------- sounds ----------------
	public static boolean soundOn = true;

	// plays sounds/<name>.wav in the background (does nothing if missing)
	public static void play(String name) {
		if (!soundOn) {
			return;
		}
		File file = new File("sounds", name + ".wav");
		if (!file.exists()) {
			return;
		}
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
			final Clip clip = AudioSystem.getClip();
			clip.open(stream);
			clip.addLineListener(new LineListener() {
				public void update(LineEvent event) {
					if (event.getType() == LineEvent.Type.STOP) {
						clip.close();
					}
				}
			});
			clip.start();
		} catch (Exception ex) {
			// a broken sound file should never stop the game
		}
	}

	// ---------------- drawing helpers ----------------
	// smooth edges and smooth text
	public static Graphics2D smooth(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		return g2;
	}

	// draws text centered inside a box
	public static void centerText(Graphics2D g2, String text, int x, int y, int w, int h) {
		FontMetrics fm = g2.getFontMetrics();
		int tx = x + (w - fm.stringWidth(text)) / 2;
		int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
		g2.drawString(text, tx, ty);
	}

	// makes the font smaller until the text fits in maxWidth
	public static Font fit(Graphics2D g2, Font font, String text, int maxWidth, float minSize) {
		Font f = safe(font, text);
		while (f.getSize2D() > minSize && g2.getFontMetrics(f).stringWidth(text) > maxWidth) {
			f = f.deriveFont(f.getSize2D() - 1f);
		}
		return f;
	}

	// a dashed rectangle with a label, used when an image file is missing
	public static void placeholder(Graphics2D g2, int x, int y, int w, int h, String line1, String line2, Color bg, Color fg) {
		g2.setColor(bg);
		g2.fillRect(x, y, w, h);
		g2.setColor(fg);
		g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 7f, 5f }, 0f));
		g2.drawRect(x + 1, y + 1, w - 3, h - 3);
		g2.setFont(mono(13f).deriveFont(Font.BOLD));
		centerText(g2, line1, x, y + h / 2 - 18, w, 18);
		centerText(g2, line2, x, y + h / 2 + 2, w, 18);
	}

	// draws an image so it covers the whole box (cuts the edges if needed)
	public static void cover(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
		double scale = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
		int iw = (int) (img.getWidth() * scale);
		int ih = (int) (img.getHeight() * scale);
		Shape oldClip = g2.getClip();
		g2.clipRect(x, y, w, h);
		g2.drawImage(img, x + (w - iw) / 2, y + (h - ih) / 2, iw, ih, null);
		g2.setClip(oldClip);
	}

	private Theme() { }
}
