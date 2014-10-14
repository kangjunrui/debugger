package debugger.shapes;

import org.eclipse.draw2d.FreeformFigure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.ViewportLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A viewport for {@link org.eclipse.draw2d.FreeformFigure FreeformFigures}.
 * FreeformFigures can only reside in this type of viewport.
 */
public class ShapesViewport extends Viewport {

	class FreeformViewportLayout extends ViewportLayout {
		protected Dimension calculatePreferredSize(IFigure parent, int wHint,
				int hHint) {
			getContents().validate();
			wHint = Math.max(0, wHint);
			hHint = Math.max(0, hHint);
			return ((FreeformFigure) getContents()).getFreeformExtent()
					.getExpanded(getInsets()).union(0, 0)
					.union(wHint - 1, hHint - 1).getSize();
		}

		protected boolean isSensitiveHorizontally(IFigure parent) {
			return true;
		}

		protected boolean isSensitiveVertically(IFigure parent) {
			return true;
		}

		public void layout(IFigure figure) {
			// Do nothing, contents updates itself.
		}
	}

	/**
	 * Constructs a new FreeformViewport. This viewport must use graphics
	 * translation to scroll the FreeformFigures inside of it.
	 */
	public ShapesViewport() {
		super(true); // Must use graphics translate to scroll freeforms.
		setLayoutManager(new FreeformViewportLayout());
	}

	/**
	 * Readjusts the scrollbars. In doing so, it gets the freeform extent of the
	 * contents and unions this rectangle with this viewport's client area, then
	 * sets the contents freeform bounds to be this unioned rectangle. Then
	 * proceeds to set the scrollbar values based on this new information.
	 * 
	 * @see Viewport#readjustScrollBars()
	 */
	protected void readjustScrollBars() {
		if (getContents() == null)
			return;
		if (!(getContents() instanceof FreeformFigure))
			return;
		FreeformFigure ff = (FreeformFigure) getContents();
		Rectangle clientArea = getClientArea();
		Rectangle bounds = ff.getFreeformExtent().getCopy();
		ff.setFreeformBounds(bounds);
		
		getVerticalRangeModel().setAll(bounds.y, clientArea.height, bounds.bottom());
		getHorizontalRangeModel().setAll(bounds.x, clientArea.width, bounds.right());
	}

	int maxset_x=0;
	int maxset_y=0;
	
	public Point getExtendedXY(){
		Rectangle clientArea = getClientArea();
		return new Point(clientArea.width+maxset_x, clientArea.height+maxset_y);
	}
	
	public void setTempXY(int x, int y){
		if (maxset_x<x) maxset_x = x;
		if (maxset_y<y) maxset_y = y;
		readjustScrollBars();System.out.println(maxset_x+":"+maxset_y);
	}
	
	public void restoreXY(){
		maxset_x = 0;
		maxset_y = 0;
		readjustScrollBars();
	}
	
	/**
	 * Returns <code>true</code>.
	 * 
	 * @see Figure#useLocalCoordinates()
	 */
	protected boolean useLocalCoordinates() {
		return true;
	}

}
