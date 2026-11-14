/* =====================================================================
 *  SecretCard.java  -  the "CONFIDENTIAL" index card with your role
 * ---------------------------------------------------------------------
 *  It arrives face-down (a sealed envelope) and flips over to show
 *  either the secret word or "YOU ARE AL-DAKHEEL".
 * ===================================================================== */

import java.awt.*;
import javax.swing.JComponent;

public class SecretCard extends JComponent {

	public static final int SEALED = 0, WORD = 1, OUTSIDER = 2, OBSERVER = 3, GUESS = 4;

	private int state = SEALED;
	private String word = "";
	private float flip = 1f;     // 0 = showing the back, 1 = showing the front

	public SecretCard() {
		setOpaque(false);
		setPreferredSize(new Dimension(520, 220));
	}

	public int getState() {
		return state;
	}

	// shows a new role with the flip animation
	public void reveal(int newState, String newWord) {
		state = newState;
		word = newWord == null ? "" : newWord;
		flip = 0f;
		repaint();
		Anim.later(450, new Runnable() {
			public void run() {
				Theme.play("flip");
				Anim.run(520, new Anim.Step() {
					public void update(float t) {
						flip = Anim.easeOut(t);
						repaint();
					}
				}, null);
			}
		});
	}

	// changes the card without the animation
	public void set(int newState, String newWord) {
		state = newState;
		word = newWord == null ? "" : newWord;
		flip = 1f;
		repaint();
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = Theme.smooth(g);
		int w = getWidth() - 16;
		int h = getHeight() - 16;

		// flip = squash horizontally: back side for the first half, front for the second
		boolean front = flip >= 0.5f || state == SEALED;
		float squash = state == SEALED ? 1f : Math.abs(flip * 2f - 1f);
		squash = Math.max(squash, 0.02f);

		g2.translate(getWidth() / 2.0, getHeight() / 2.0);
		g2.rotate(Math.toRadians(-1));
		g2.scale(squash, 1);
		int x = -w / 2;
		int y = -h / 2;

		// shadow
		g2.setColor(new Color(0, 0, 0, 45));
		g2.fillRect(x + 3, y + 6, w, h);

		if (!front || state == SEALED) {
			paintBack(g2, x, y, w, h);
		} else {
			paintFront(g2, x, y, w, h);
		}
		g2.dispose();
	}

	// manila envelope with a red seal
	private void paintBack(Graphics2D g2, int x, int y, int w, int h) {
		g2.setColor(new Color(201, 168, 106));
		g2.fillRect(x, y, w, h);
		g2.setColor(new Color(176, 145, 88));
		g2.setStroke(new BasicStroke(2f));
		g2.drawLine(x, y, x + w / 2, y + h / 2);
		g2.drawLine(x + w, y, x + w / 2, y + h / 2);
		g2.setColor(Theme.RED);
		g2.fillOval(-26, -26, 52, 52);
		g2.setColor(Theme.PAPER);
		g2.setFont(Theme.stencil(16f));
		Theme.centerText(g2, "458", -26, -26, 52, 52);
		g2.setColor(Theme.INK_SOFT);
		String label = Lang.t(state == SEALED ? "awaiting" : "sealed");
		g2.setFont(Theme.safe(Theme.stencil(18f), label));
		Theme.centerText(g2, label, x, y + h - 46, w, 30);
	}

	private void paintFront(Graphics2D g2, int x, int y, int w, int h) {
		// white card with blue ruled lines
		g2.setColor(Color.WHITE);
		g2.fillRect(x, y, w, h);
		g2.setColor(Theme.RULE_BLUE);
		for (int ly = y + 70; ly < y + h; ly += 32) {
			g2.fillRect(x, ly, w, 1);
		}
		// red header
		boolean danger = state == OUTSIDER || state == GUESS;
		g2.setColor(state == OBSERVER ? Theme.INK_SOFT : Theme.RED);
		g2.fillRect(x, y, w, 36);
		String header = Lang.t(state == OBSERVER ? "hdr_observer" : (danger ? "hdr_classified" : "hdr_confidential"));
		g2.setColor(Theme.PAPER);
		g2.setFont(Theme.safe(Theme.stencil(16f), header));
		int inner = w - 44;
		g2.drawString(header, Theme.lineX(g2, header, x + 18, w - 36), y + 25);

		String label;
		String big;
		String second = "";      // the word in the other language, smaller
		String hint;
		Color bigColor = Theme.INK;
		if (state == WORD) {
			label = Lang.t("lbl_word");
			if (Lang.rtl()) {
				big = Lang.word(word);
				second = big.equals(word) ? "" : word.toUpperCase();
			} else {
				big = word.toUpperCase();
				second = Lang.arabicWord(word);
			}
			hint = Lang.t("hint_word");
		} else if (state == OUTSIDER) {
			label = Lang.t("lbl_role");
			big = Lang.t("big_outsider");
			hint = Lang.t("hint_outsider");
			bigColor = Theme.RED;
		} else if (state == GUESS) {
			label = Lang.t("lbl_last");
			big = Lang.t("big_guess");
			hint = Lang.t("hint_guess");
			bigColor = Theme.RED;
		} else {
			label = Lang.t("lbl_status");
			big = Lang.t("big_observing");
			hint = Lang.t("hint_observer");
		}

		g2.setColor(Theme.MUTED);
		g2.setFont(Theme.safe(Theme.type(16f), label));
		g2.drawString(label, Theme.lineX(g2, label, x + 22, inner), y + 64);

		g2.setColor(bigColor);
		g2.setFont(Theme.fit(g2, Theme.stencil(58f), big, inner - (second.length() > 0 ? 150 : 0), 20f));
		FontMetrics fm = g2.getFontMetrics();
		int bigY = y + 72 + fm.getAscent();
		int bigX = Theme.lineX(g2, big, x + 22, inner);
		g2.drawString(big, bigX, bigY);

		// the other language next to the big word
		if (second.length() > 0) {
			int bigW = fm.stringWidth(big);
			g2.setColor(Theme.INK_SOFT);
			g2.setFont(Theme.fit(g2, Lang.rtl() ? Theme.stencil(26f) : Theme.arabic(34f), second, 140, 12f));
			int sw = g2.getFontMetrics().stringWidth(second);
			int sx = Lang.rtl() ? bigX - 18 - sw : bigX + bigW + 18;
			g2.drawString(second, sx, bigY);
		}

		g2.setColor(Theme.INK_SOFT);
		g2.setFont(Theme.fit(g2, Theme.type(15f), hint, inner, 10f));
		g2.drawString(hint, Theme.lineX(g2, hint, x + 22, inner), y + h - 20);
	}
}
