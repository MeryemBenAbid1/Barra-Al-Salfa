/* =====================================================================
 *  FileCabinet.java  -  the host picks the topic from a drawer of files
 * ---------------------------------------------------------------------
 *  An open filing-cabinet drawer, seen from the front. Inside, the
 *  folders stand one BEHIND the other, like real hanging files: you
 *  only see the top of each folder and its TAG (the little tab).
 *  The topic is written on the tag. The tags sit at 3 different places
 *  (left, middle, right) so every tag can be seen.
 *
 *      [ FOOD ]                 <- folder at the back
 *         ____[ PLACES ]____
 *                 ____[ JOBS ]  <- ...
 *      [ ? RANDOM ]____________ <- folder at the front
 *     |=========( handle )=========|   <- drawer front
 *
 *  ANIMATIONS (all with Anim, so they run on the Swing thread)
 *   dealIn()    the drawer opens: the folders pop up one by one
 *   hover       the folder under the mouse lifts a little
 *   setPicked() the chosen folder slides UP out of the drawer and a
 *               red CHOSEN stamp slams on it; the old one slides back
 *
 *  Only the host can click (setClickable). Everybody else still sees
 *  the animation, because it runs when the server's PICKED message
 *  arrives, not when the host clicks.
 *
 *  The whole drawer is ONE painted component, drawn in 3 passes:
 *   1. the folders, from the back to the front (front ones cover back ones)
 *   2. ALL the tags on top, so a pulled-out folder never hides a tag
 *   3. the drawer front, then the CHOSEN stamp
 *  Clicks: the tags are checked first, then the folders front to back.
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.JComponent;

public class FileCabinet extends JComponent {

	public interface PickListener {
		void pick(String category);      // a category name, or "RANDOM"
	}

	private static final Color DRAWER      = new Color(78, 84, 80);    // grey-green metal
	private static final Color DRAWER_DARK = new Color(46, 50, 47);
	private static final Color INSIDE      = new Color(28, 24, 20);    // inside of the drawer
	private static final Color HANDLE      = new Color(196, 190, 176);

	// the folders from the BACK of the drawer to the FRONT
	// (the categories in file order, RANDOM at the very front)
	private final ArrayList<String> files = new ArrayList<String>();
	private Rectangle[] tabs = new Rectangle[0];     // where each tag was drawn
	private Rectangle[] bodies = new Rectangle[0];   // the visible top of each folder
	private float[] lift = new float[0];             // 0..1 hover lift
	private float[] rise = new float[0];             // 0..1 pulled out of the drawer
	private float[] pop = new float[0];              // 0..1 popped up when the drawer opens
	private int hovered = -1;
	private int chosen = -1;
	private float stampT = 1f;                       // 0..1 the CHOSEN stamp animation
	private boolean clickable = false;
	private PickListener listener;

	public FileCabinet() {
		setOpaque(false);
		setPreferredSize(new Dimension(600, 340));

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				setHovered(clickable ? fileAt(e.getPoint()) : -1);
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseExited(MouseEvent e) {
				setHovered(-1);
			}
			public void mouseClicked(MouseEvent e) {
				int i = fileAt(e.getPoint());
				if (clickable && i >= 0 && listener != null) {
					Theme.play("click");
					listener.pick(files.get(i));    // the server answers with PICKED
				}
			}
		});
	}

	public void setPickListener(PickListener l) { listener = l; }

	// only the host may click the tags
	public void setClickable(boolean on) {
		clickable = on;
		setCursor(Cursor.getPredefinedCursor(on ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
		if (!on) {
			setHovered(-1);
		}
		repaint();
	}

	// CATEGORIES message: the files on the server
	public void setCategories(ArrayList<String> categoryNames) {
		String pickedName = chosen >= 0 && chosen < files.size() ? files.get(chosen) : "RANDOM";
		files.clear();
		files.addAll(categoryNames);
		files.add("RANDOM");
		lift = new float[files.size()];
		rise = new float[files.size()];
		pop = new float[files.size()];
		for (int i = 0; i < pop.length; i++) {
			pop[i] = 1f;
		}
		chosen = files.indexOf(pickedName);
		if (chosen >= 0) {
			rise[chosen] = 1f;          // already out of the drawer, no animation
		}
		hovered = -1;
		repaint();
	}

	// PICKED message: pull the chosen file out (everyone sees this)
	public void setPicked(String name) {
		int index = files.indexOf(name);
		if (index == chosen) {
			return;
		}
		int old = chosen;
		chosen = index;
		animate(rise, old, 0f, 300);             // the old file slides back in
		animate(rise, index, 1f, 450);           // the new one slides out
		Theme.play("swoosh");
		Anim.later(380, new Runnable() {
			public void run() {
				Theme.play("stamp");
				Anim.run(260, new Anim.Step() {
					public void update(float t) {
						stampT = t;
						repaint();
					}
				}, null);
			}
		});
		stampT = 0f;
	}

	// the drawer opens: the folders pop up from back to front
	public void dealIn() {
		for (int i = 0; i < pop.length; i++) {
			pop[i] = 0f;
			final int index = i;
			Anim.later(80 + i * 70, new Runnable() {
				public void run() {
					animate(pop, index, 1f, 300);
				}
			});
		}
		Theme.play("swoosh");
		repaint();
	}

	private void setHovered(int index) {
		if (index == hovered) {
			return;
		}
		animate(lift, hovered, 0f, 140);
		hovered = index;
		animate(lift, index, 1f, 140);
	}

	// moves values[index] smoothly to the target (0 or 1)
	private void animate(final float[] values, final int index, final float target, int millis) {
		if (index < 0 || index >= values.length) {
			return;
		}
		final float start = values[index];
		Anim.run(millis, new Anim.Step() {
			public void update(float t) {
				if (index < values.length) {
					values[index] = start + (target - start) * Anim.easeOut(t);
					repaint();
				}
			}
		}, null);
	}

	// which file is under the mouse? the FRONT files are checked first,
	// because they cover the ones behind them
	private int fileAt(Point p) {
		for (int i = tabs.length - 1; i >= 0; i--) {
			if (tabs[i] != null && tabs[i].contains(p)) {
				return i;                       // the tags are drawn on top
			}
		}
		for (int i = bodies.length - 1; i >= 0; i--) {
			if (bodies[i] != null && bodies[i].contains(p)) {
				return i;
			}
		}
		return -1;
	}

	// ================= drawing =================
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Theme.smooth(g);
		int n = files.size();
		if (n == 0) {
			g2.dispose();
			return;
		}
		int w = getWidth();
		int h = getHeight();

		// sizes: the drawer is centered, the space on top is for pulled-out files
		int drawerW = Math.min(w - 20, 660);
		int dx = (w - drawerW) / 2;
		int tabH = 30;
		int frontH = Math.max(52, h / 6);              // the drawer front
		int showFront = 34;                             // how much of the front folder shows
		int riseMax = tabH + 12;                        // room on top for the chosen file's tag
		int frontTop = h - frontH;                      // where the drawer front starts
		int firstTop = riseMax + tabH + 6;              // top of the back folder
		int lastTop = frontTop - showFront;             // top of the front folder
		float step = n > 1 ? (lastTop - firstTop) / (float) (n - 1) : 0;
		// real hanging files stand close together: at most 46 px apart,
		// the extra space goes above the drawer
		if (step > tabH + 16) {
			step = tabH + 16;
			firstTop = Math.round(lastTop - step * (n - 1));
		}

		// ---- inside of the drawer (dark), a bit narrower at the back ----
		int insideTop = firstTop - 8;
		Polygon inside = new Polygon();
		inside.addPoint(dx + 18, insideTop);
		inside.addPoint(dx + drawerW - 18, insideTop);
		inside.addPoint(dx + drawerW - 4, frontTop);
		inside.addPoint(dx + 4, frontTop);
		g2.setColor(INSIDE);
		g2.fillPolygon(inside);

		tabs = new Rectangle[n];
		bodies = new Rectangle[n];
		int folderX = dx + 22;
		int folderW = drawerW - 44;
		int tabW = Math.max(110, (int) (folderW * 0.30));

		// ---- pass 1: the folders, from the back to the front ----
		int[] tops = new int[n];
		for (int i = 0; i < n; i++) {
			// the chosen file comes out ABOVE all the others, so its tag
			// gets the top row to itself and never covers another tag
			float outOfDrawer = firstTop + step * i - (firstTop - tabH - 8);
			float up = outOfDrawer * rise[i]              // pulled out of the drawer
					+ 8f * lift[i]                          // mouse over it
					- 40f * (1f - pop[i]);                 // still down in the drawer
			int top = Math.round(firstTop + step * i - up);

			// the tag: left, middle or right (mirrored in Arabic)
			int slot = i % 3;
			if (Lang.rtl()) {
				slot = 2 - slot;
			}
			int tabX = folderX + 8 + slot * ((folderW - 16 - tabW) / 2);
			tabs[i] = new Rectangle(tabX, top - tabH, tabW, tabH + 4);
			int nextTop = i < n - 1 ? Math.round(firstTop + step * (i + 1)) : frontTop;
			bodies[i] = new Rectangle(folderX, top, folderW, Math.max(0, Math.min(nextTop, frontTop) - top));

			tops[i] = top;
			drawFolder(g2, i, folderX, top, folderW, frontTop - top + 20);
		}

		// ---- pass 2: every tag on top (the tag IS the topic you click) ----
		for (int i = 0; i < n; i++) {
			drawTab(g2, i, tabs[i]);
		}

		// ---- pass 3: the drawer front, then the CHOSEN stamp ----
		drawDrawerFront(g2, dx, frontTop, drawerW, frontH);
		if (chosen >= 0 && chosen < n && stampT > 0f) {
			Rectangle t = tabs[chosen];
			// next to the tag, on the side where there is room
			boolean roomRight = t.x + t.width + 140 < dx + drawerW;
			int sx = roomRight ? t.x + t.width + 70 : t.x - 70;
			drawStamp(g2, sx, t.y + t.height / 2, 2f - Anim.easeOut(stampT), Math.min(1f, stampT * 3f));
		}
		g2.dispose();
	}

	// the color of folder i: the ones at the back are a little darker
	private Color manila(int i) {
		float depth = files.size() > 1 ? i / (float) (files.size() - 1) : 1f;
		Color c = mix(new Color(176, 152, 108), Theme.FOLDER, 0.55f + 0.45f * depth);
		if (chosen >= 0 && i != chosen) {
			c = mix(c, INSIDE, 0.18f);           // not chosen: a bit darker
		}
		return c;
	}

	private Graphics2D fadeIn(Graphics2D g, int i) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, pop[i]))));
		return g2;
	}

	// the tag sticking up from folder i, with the topic on a white label
	private void drawTab(Graphics2D g, int i, Rectangle tab) {
		Graphics2D g2 = fadeIn(g, i);
		boolean isChosen = i == chosen;
		g2.setColor(manila(i));
		g2.fillRoundRect(tab.x, tab.y, tab.width, tab.height + 8, 10, 10);
		// a white label on the tag with the topic
		int lx = tab.x + 7, ly = tab.y + 5, lw = tab.width - 14, lh = tab.height - 9;
		g2.setColor(isChosen ? Theme.RED : new Color(250, 246, 236));
		g2.fillRect(lx, ly, lw, lh);
		String name = files.get(i);
		String label = name.equals("RANDOM") ? "?  " + Lang.category(name) : Lang.category(name).toUpperCase();
		g2.setColor(isChosen ? Theme.PAPER : Theme.INK);
		g2.setFont(Theme.fit(g2, Theme.stencil(15f), label, lw - 8, 9f));
		Theme.centerText(g2, label, lx, ly, lw, lh);
		g2.dispose();
	}

	// folder i (its bottom is hidden by the drawer front)
	private void drawFolder(Graphics2D g, int i, int x, int y, int w, int h) {
		Graphics2D g2 = fadeIn(g, i);
		boolean isChosen = i == chosen;
		Color manila = manila(i);
		g2.setColor(new Color(0, 0, 0, 60));
		g2.fillRect(x, y - 3, w, 4);                       // shadow line on the folder behind
		g2.setColor(manila);
		g2.fillRect(x, y, w, h);
		g2.setColor(new Color(140, 118, 80));
		g2.setStroke(new BasicStroke(isChosen ? 2.5f : 1.2f));
		g2.drawLine(x, y, x + w, y);
		if (isChosen) {
			g2.setColor(Theme.INK);
			g2.drawRect(x, y, w, h);
		}
		g2.dispose();
	}

	private void drawDrawerFront(Graphics2D g2, int x, int y, int w, int h) {
		// the metal front with a darker bottom edge
		g2.setColor(DRAWER);
		g2.fillRoundRect(x, y, w, h, 8, 8);
		g2.setColor(DRAWER_DARK);
		g2.fillRect(x, y + h - 6, w, 6);
		g2.setColor(new Color(255, 255, 255, 40));
		g2.drawLine(x + 4, y + 2, x + w - 4, y + 2);

		// a long handle in the middle
		int hw = Math.min(220, w / 3);
		int hy = y + h / 2 + 4;
		g2.setColor(DRAWER_DARK);
		g2.fillRoundRect(x + (w - hw) / 2, hy - 4, hw, 14, 14, 14);
		g2.setColor(HANDLE);
		g2.fillRoundRect(x + (w - hw) / 2, hy - 7, hw, 12, 12, 12);

		// the little card holder with "CASE FILES"
		String text = Lang.t("drawer_label");
		g2.setFont(Theme.safe(Theme.stencil(12f), text));
		int cw = g2.getFontMetrics().stringWidth(text) + 20;
		int cx = x + (w - cw) / 2;
		int cy = y + 6;
		g2.setColor(HANDLE);
		g2.fillRect(cx - 2, cy - 2, cw + 4, 20);
		g2.setColor(new Color(250, 246, 236));
		g2.fillRect(cx, cy, cw, 16);
		g2.setColor(Theme.INK);
		Theme.centerText(g2, text, cx, cy, cw, 16);
	}

	private void drawStamp(Graphics2D g, int cx, int cy, float scale, float alpha) {
		Graphics2D s = (Graphics2D) g.create();
		String text = Lang.t("chosen");
		s.setFont(Theme.safe(Theme.stencil(20f), text));
		FontMetrics fm = s.getFontMetrics();
		int sw = fm.stringWidth(text) + 20;
		int sh = fm.getAscent() + 10;
		s.translate(cx, cy);
		s.rotate(Math.toRadians(-6));
		s.scale(scale, scale);
		s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		s.setColor(Theme.RED);
		s.setStroke(new BasicStroke(3f));
		s.drawRect(-sw / 2, -sh / 2, sw, sh);
		s.drawString(text, -fm.stringWidth(text) / 2, fm.getAscent() / 2 - 1);
		s.dispose();
	}

	// a color between a and b (t = 0 -> a, t = 1 -> b)
	private static Color mix(Color a, Color b, float t) {
		return new Color(
				(int) (a.getRed() + (b.getRed() - a.getRed()) * t),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}
}
