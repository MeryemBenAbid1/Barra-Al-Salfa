/* =====================================================================
 *  Anim.java  -  tiny animation helper
 * ---------------------------------------------------------------------
 *  Anim.run(300, step, done) calls step.update(t) about 60 times a
 *  second, with t going from 0.0 to 1.0 over 300 milliseconds.
 *  It uses javax.swing.Timer, so the code runs on the Swing thread and
 *  can change components safely.
 * ===================================================================== */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public final class Anim {

	public interface Step {
		void update(float t);   // t goes from 0 to 1
	}

	public static Timer run(final int millis, final Step step, final Runnable done) {
		final long start = System.currentTimeMillis();
		final Timer timer = new Timer(15, null);
		timer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float t = (System.currentTimeMillis() - start) / (float) millis;
				if (t >= 1f) {
					t = 1f;
					timer.stop();
				}
				step.update(t);
				if (t >= 1f && done != null) {
					done.run();
				}
			}
		});
		step.update(0f);
		timer.start();
		return timer;
	}

	// runs something once after a delay
	public static void later(int millis, final Runnable action) {
		Timer timer = new Timer(millis, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	// fast at the start, slow at the end
	public static float easeOut(float t) {
		float u = 1f - t;
		return 1f - u * u * u;
	}

	// goes a little past the end, then settles (for things that "pop")
	public static float easeBack(float t) {
		float c = 1.7f;
		float u = t - 1f;
		return 1f + (c + 1f) * u * u * u + c * u * u;
	}

	private Anim() { }
}
