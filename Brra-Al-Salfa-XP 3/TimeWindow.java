/* =====================================================================
 *  TimeWindow.java  -  "Time left": round number + the green time bar
 * ---------------------------------------------------------------------
 *  The server's GameTimer thread sends TIMER:<seconds> every second.
 *  SpyClient calls setTime(secondsLeft, totalSeconds) each time.
 *      Round 1 of 3                         1:14 remaining
 *      [##########################........]
 * ===================================================================== */

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TimeWindow extends XpWindow {

	private final JLabel remaining = new JLabel();
	private final XpParts.Blocks bar = new XpParts.Blocks(28);

	public TimeWindow(int round, int rounds) {
		super(Lang.t("time_left"), Xp.pixelIcon(Xp.SMILE, 2));
		body().setBorder(new EmptyBorder(16, 18, 14, 18));
		JPanel top = Xp.clear(new BorderLayout());
		top.add(Xp.label(Lang.f("round_of", round, rounds), Xp.PIXEL, 17f, Xp.INK), BorderLayout.LINE_START);
		remaining.setForeground(Xp.INK);
		top.add(remaining, BorderLayout.LINE_END);
		top.setBorder(new EmptyBorder(0, 0, 10, 0));
		body().add(top, BorderLayout.NORTH);
		JPanel barRow = Xp.clear(new BorderLayout());
		barRow.add(bar, BorderLayout.NORTH);
		body().add(barRow, BorderLayout.CENTER);
	}

	public void setTime(int left, int total) {
		String text = Lang.f("remaining", XpParts.clock(left));
		remaining.setText(text);
		remaining.setFont(Xp.fontFor(text, Xp.PIXEL, 17f));
		bar.setValue(left, total);
	}
}
