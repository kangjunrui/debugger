package debugger.shapes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayeredPane;
import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gef.editparts.GridLayer;
import org.eclipse.gef.editparts.GuideLayer;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.SimpleRootEditPart;
import org.eclipse.gef.editparts.ViewportAutoexposeHelper;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.MarqueeDragTracker;

public class ShapesRootEditPart  extends SimpleRootEditPart implements
LayerConstants, LayerManager {

	private LayeredPane innerLayers;
	private LayeredPane printableLayers;
	private ScalableFreeformLayeredPane scaledLayers;
	private ZoomManager zoomManager;
	
	private PropertyChangeListener gridListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			String property = evt.getPropertyName();
			if (property.equals(SnapToGrid.PROPERTY_GRID_ORIGIN)
					|| property.equals(SnapToGrid.PROPERTY_GRID_SPACING)
					|| property.equals(SnapToGrid.PROPERTY_GRID_VISIBLE))
				refreshGridLayer();
		}
	};

	/**
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
	 */
	protected IFigure createFigure() {
		final ShapesViewport viewport = new ShapesViewport();
		innerLayers = new FreeformLayeredPane(){
			@Override
			public Rectangle getFreeformExtent() {
				Rectangle bounds = super.getFreeformExtent();
				Point temp=viewport.getExtendedXY();
				int inc=temp.y-bounds.bottom();
				if (inc>0) bounds.height+=inc;
				inc=temp.x-bounds.right();
				if (inc>0) bounds.width+=inc;
				return bounds;
				
			}
		};
		createLayers(innerLayers);
		viewport.setContents(innerLayers);
		return viewport;
	}

	/**
	 * Creates a {@link GridLayer grid}. Sub-classes can override this method to
	 * customize the appearance of the grid. The grid layer should be the first
	 * layer (i.e., beneath the primary layer) if it is not to cover up parts on
	 * the primary layer. In that case, the primary layer should be transparent
	 * so that the grid is visible.
	 * 
	 * @return the newly created GridLayer
	 */
	protected GridLayer createGridLayer() {
		return new GridLayer();
	}

	/**
	 * Creates the top-most set of layers on the given layered pane.
	 * 
	 * @param layeredPane
	 *            the parent for the created layers
	 */
	protected void createLayers(LayeredPane layeredPane) {
		layeredPane.add(getScaledLayers(), SCALABLE_LAYERS);
		layeredPane.add(new FreeformLayer(), HANDLE_LAYER);
		layeredPane.add(new FeedbackLayer(), FEEDBACK_LAYER);
		layeredPane.add(new GuideLayer(), GUIDE_LAYER);
	}

	/**
	 * Creates a layered pane and the layers that should be printed.
	 * 
	 * @see org.eclipse.gef.print.PrintGraphicalViewerOperation
	 * @return a new LayeredPane containing the printable layers
	 */
	protected LayeredPane createPrintableLayers() {
		FreeformLayeredPane layeredPane = new FreeformLayeredPane();
		layeredPane.add(new FreeformLayer(), PRIMARY_LAYER);
		layeredPane.add(new ConnectionLayer(), CONNECTION_LAYER);
		return layeredPane;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == AutoexposeHelper.class)
			return new ViewportAutoexposeHelper(this);
		return super.getAdapter(adapter);
	}

	/**
	 * The contents' Figure will be added to the PRIMARY_LAYER.
	 * 
	 * @see org.eclipse.gef.GraphicalEditPart#getContentPane()
	 */
	public IFigure getContentPane() {
		return getLayer(PRIMARY_LAYER);
	}

	DragTracker dragtracker;
	public DragTracker getDragTracker(Request req) {
		if (dragtracker==null)
			dragtracker=new RootEditPartDragTracker((GraphicalEditPart)this);
		return dragtracker;
	}

	/**
	 * Returns the layer indicated by the key. Searches all layered panes.
	 * 
	 * @see LayerManager#getLayer(Object)
	 */
	public IFigure getLayer(Object key) {
		IFigure layer = scaledLayers.getLayer(key);
		if (layer != null)
			return layer;
		if (innerLayers == null)
			return null;
		layer = innerLayers.getLayer(key);
		if (layer != null)
			return layer;
		if (printableLayers == null)
			return null;
		return printableLayers.getLayer(key);
	}

	/**
	 * The root editpart does not have a real model. The LayerManager ID is
	 * returned so that this editpart gets registered using that key.
	 * 
	 * @see org.eclipse.gef.EditPart#getModel()
	 */
	public Object getModel() {
		return LayerManager.ID;
	}

	/**
	 * Returns the LayeredPane that should be used during printing. This layer
	 * will be identified using {@link LayerConstants#PRINTABLE_LAYERS}.
	 * 
	 * @return the layered pane containing all printable content
	 */
	protected LayeredPane getPrintableLayers() {
		if (printableLayers == null)
			printableLayers = createPrintableLayers();
		return printableLayers;
	}

	/**
	 * Updates the {@link GridLayer grid} based on properties set on the
	 * {@link #getViewer() graphical viewer}:
	 * {@link SnapToGrid#PROPERTY_GRID_VISIBLE},
	 * {@link SnapToGrid#PROPERTY_GRID_SPACING}, and
	 * {@link SnapToGrid#PROPERTY_GRID_ORIGIN}.
	 * <p>
	 * This method is invoked initially when the GridLayer is created, and when
	 * any of the above-mentioned properties are changed on the viewer.
	 */
	protected void refreshGridLayer() {
		boolean visible = false;
		GridLayer grid = (GridLayer) getLayer(GRID_LAYER);
		Boolean val = (Boolean) getViewer().getProperty(
				SnapToGrid.PROPERTY_GRID_VISIBLE);
		if (val != null)
			visible = val.booleanValue();
		grid.setOrigin((Point) getViewer().getProperty(
				SnapToGrid.PROPERTY_GRID_ORIGIN));
		grid.setSpacing((Dimension) getViewer().getProperty(
				SnapToGrid.PROPERTY_GRID_SPACING));
		grid.setVisible(visible);
	}

	/**
	 * @see org.eclipse.gef.editparts.AbstractEditPart#register()
	 */
	protected void register() {
		super.register();
		if (getLayer(GRID_LAYER) != null) {
			getViewer().addPropertyChangeListener(gridListener);
			refreshGridLayer();
		}
		getViewer().setProperty(ZoomManager.class.toString(), getZoomManager());
	}

	/**
	 * @see AbstractEditPart#unregister()
	 */
	protected void unregister() {
		getViewer().removePropertyChangeListener(gridListener);
		super.unregister();
		getViewer().setProperty(ZoomManager.class.toString(), null);
	}

	class FeedbackLayer extends FreeformLayer {
		FeedbackLayer() {
			setEnabled(false);
		}
	}

	/**
	 * Constructor for ScalableFreeformRootEditPart
	 */
	public ShapesRootEditPart() {
		zoomManager = new ZoomManager((ScalableFigure) getScaledLayers(),
				((Viewport) getFigure()));
	}

	/**
	 * Creates a layered pane and the layers that should be scaled.
	 * 
	 * @return a new freeform layered pane containing the scalable layers
	 */
	protected ScalableFreeformLayeredPane createScaledLayers() {
		ScalableFreeformLayeredPane layers = new ScalableFreeformLayeredPane();
		layers.add(createGridLayer(), GRID_LAYER);
		layers.add(getPrintableLayers(), PRINTABLE_LAYERS);
		layers.add(new FeedbackLayer(), SCALED_FEEDBACK_LAYER);
		return layers;
	}

	/**
	 * Returns the scalable layers of this EditPart
	 * 
	 * @return LayeredPane
	 */
	protected LayeredPane getScaledLayers() {
		if (scaledLayers == null)
			scaledLayers = createScaledLayers();
		return scaledLayers;
	}

	/**
	 * Returns the zoomManager.
	 * 
	 * @return ZoomManager
	 */
	public ZoomManager getZoomManager() {
		return zoomManager;
	}
}
