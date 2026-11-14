/* =====================================================================
 *  DashedBorder.java  -  a solid or dashed line on one side of a panel
 *  (used for the ruled lines between sections of the case file)
 * ===================================================================== */

import java.awt.*;
import javax.swing.border.AbstractBorder;

public class DashedBorder extends AbstractBorder {

	public static final int TOP = 0, BOTTOM = 1, LEFT = 2, RIGHT = 3;

	private final int side;
	private final boolean dashed;
	private final Color color;
	private final int gap;      // space between the line and the content

	public DashedBorder(int side, boolean dashed, Color color, int gap) {
		this.side = side;
		this.dashed = dashed;
		this.color = color;
		this.gap = gap;
	}

	public Insets getBorderInsets(Component c) {
		int t = side == TOP ? gap + 2 : 0;
		int b = side == BOTTOM ? gap + 2 : 0;
		int l = side == LEFT ? gap + 2 : 0;
		int r = side == RIGHT ? gap + 2 : 0;
		return new Insets(t, l, b, r);
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setColor(color);
		if (dashed) {
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 8f, 6f }, 0f));
		} else {
			g2.setStroke(new BasicStroke(2f));
		}
		if (side == TOP) {
			g2.drawLine(x, y + 1, x + w, y + 1);
		} else if (side == BOTTOM) {
			g2.drawLine(x, y + h - 1, x + w, y + h - 1);
		} else if (side == LEFT) {
			g2.drawLine(x + 1, y, x + 1, y + h);
		} else {
			g2.drawLine(x + w - 2, y, x + w - 2, y + h);
		}
		g2.dispose();
	}
}
