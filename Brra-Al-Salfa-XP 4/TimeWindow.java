/* =====================================================================
 *  TimeWindow.java  -  "Time left": round number + the green time bar
 * ---------------------------------------------------------------------
 *  The server's GameTimer thread sends TIMER:<seconds> every second.
 *  SpyClient calls setTime(secondsLeft, totalSeconds) each time.
 *  Every question has its own countdown (setQuestion shows which one).
 *      Round 1 of 3 · Question 3 of 8              0:14 remaining
 *      [##########################........]
 * ===================================================================== */

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TimeWindow extends XpWindow {

	private final JLabel remaining = new JLabel();
	private final JLabel title = new JLabel();
	private final int round, rounds;
	private final XpParts.Blocks bar = new XpParts.Blocks(28);

	public TimeWindow(int round, int rounds) {
		super(Lang.t("time_left"), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(16, 18, 14, 18));
		JPanel top = Xp.clear(new BorderLayout());
		this.round = round;
		this.rounds = rounds;
		title.setForeground(Xp.INK);
		setQuestion(0, 0);
		top.add(title, BorderLayout.LINE_START);
		remaining.setForeground(Xp.INK);
		top.add(remaining, BorderLayout.LINE_END);
		top.setBorder(new EmptyBorder(0, 0, 10, 0));
		body().add(top, BorderLayout.NORTH);
		JPanel barRow = Xp.clear(new BorderLayout());
		barRow.add(bar, BorderLayout.NORTH);
		body().add(barRow, BorderLayout.CENTER);
	}

	// "Round 1 of 3 · Question 3 of 8"  (q = 0: just the round)
	public void setQuestion(int q, int total) {
		String text = Lang.f("round_of", round, rounds);
		if (q > 0) {
			text = text + " · " + Lang.f("question_of", q, total);
		}
		title.setText(text);
		title.setFont(Xp.fontFor(text, Xp.PIXEL, 17f));
	}

	public void setTime(int left, int total) {
		String text = Lang.f("remaining", XpParts.clock(left));
		remaining.setText(text);
		remaining.setFont(Xp.fontFor(text, Xp.PIXEL, 17f));
		bar.setValue(left, total);
	}
}
