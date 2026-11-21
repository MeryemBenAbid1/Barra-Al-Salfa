/* =====================================================================
 *  XpDialog.java  -  a real pop-up (JDialog) that looks like an XP window
 * ---------------------------------------------------------------------
 *  The brief asks for a pop-up for the nickname. A JDialog is the same
 *  kind of window that JOptionPane uses, so here:
 *    - setUndecorated(true) removes the normal title bar
 *    - our XpWindow (blue title bar, beige body) becomes its content
 *    - the dialog is placed on top of the game desktop
 *  modal = true  -> setVisible(true) WAITS until the dialog is closed
 *                   (exactly like JOptionPane.showMessageDialog)
 * ===================================================================== */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class XpDialog extends JDialog {

	private final Desktop desktop;
	private int x, y;                   // position on the desktop

	public XpDialog(Desktop desktop, XpWindow window, boolean modal) {
		super(SwingUtilities.getWindowAncestor(desktop), modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
		this.desktop = desktop;
		setUndecorated(true);
		// an opaque beige window, cut to the round XP shape in showAt():
		// (a see-through window can flash black for a moment while it opens)
		setBackground(Xp.FACE);
		setContentPane(window);
		Xp.orient(window);                // Arabic: right to left

		// a pop-up that waits for an answer must be in FRONT: on a Mac it can
		// open behind the game window, and then the game looks frozen
		if (modal) {
			setAlwaysOnTop(true);
		}
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				toFront();
				requestFocus();
			}
		});

		// if the game window moves, the dialog moves with it
		getOwner().addComponentListener(new ComponentAdapter() {
			public void componentMoved(ComponentEvent e) {
				if (isVisible()) {
					move();
				}
			}
		});
	}

	// shows the dialog at x, y on the desktop (height 0 = as tall as needed)
	public void showAt(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		if (height <= 0) {
			setSize(width, 100);
			height = getContentPane().getPreferredSize().height;
		}
		setSize(width, height);
		// round top corners, like the windows on the desktop (if supported)
		GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
			setShape(new java.awt.geom.RoundRectangle2D.Float(0, 0, width - 4, height - 4, 16, 16));
		}
		getContentPane().validate();      // laid out before the first paint
		move();
		setVisible(true);                // a modal dialog waits here
	}

	public Desktop desktop() {
		return desktop;
	}

	private void move() {
		Point p = desktop.getLocationOnScreen();
		setLocation(p.x + x, p.y + y);
	}
}
