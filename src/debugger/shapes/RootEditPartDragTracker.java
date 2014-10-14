package debugger.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RangeModel;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SharedCursors;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gef.util.EditPartUtilities;

public class RootEditPartDragTracker extends AbstractTool implements
		DragTracker {
	public static class RootAutoexposeHelper implements AutoexposeHelper {

		// defines the range where autoscroll is active inside a viewer 
		private static final Insets DEFAULT_EXPOSE_THRESHOLD = new Insets(18);
		//the last time an auto expose was performed 
		private long lastStepTime = 0;
		private Insets threshold;

		protected GraphicalEditPart owner;
		public Viewport port;
		
		protected Viewport findViewport() {
			IFigure figure = null;
			port = null;
			do {
				if (figure == null)
					figure = owner.getContentPane();
				else
					figure = figure.getParent();
				if (figure instanceof Viewport) {
					port = (Viewport) figure;
					break;
				}
			} while (figure != owner.getFigure() && figure != null);
			return port;
		}


		public RootAutoexposeHelper(GraphicalEditPart owner) {
			this.owner = owner;
			threshold = DEFAULT_EXPOSE_THRESHOLD;
			findViewport();
		}

		public boolean detect(Point where) {
			lastStepTime = 0;
			Rectangle rect = Rectangle.SINGLETON;
			port.getClientArea(rect);
			//port.translateToParent(rect);
			//port.translateToAbsolute(rect);
			return rect.contains(where)
					&& !rect.crop(threshold).contains(where);
		}

		public boolean step(Point where) {
			Rectangle rect = Rectangle.SINGLETON;
			//return the virtual rect visible
			port.getClientArea(rect);
			//make the rect into canvas coordinates
			//port.translateToParent(rect);
			//may be unnecessary
			//port.translateToAbsolute(rect);
			Rectangle innerRect = new Rectangle(rect);
			innerRect.crop(threshold);
			if (!rect.contains(where) || innerRect.contains(where))
				return false;
			
			// set scroll offset (speed factor)
			int scrollOffset = 0;

			// calculate time based scroll offset
			if (lastStepTime == 0)
				lastStepTime = System.currentTimeMillis();

			long difference = System.currentTimeMillis() - lastStepTime;

			if (difference > 0) {
				scrollOffset = ((int) difference / 3);
				if (scrollOffset == 0)
					return true;
				lastStepTime = System.currentTimeMillis();
			}else{
				return true;
			}

			int region = innerRect.getPosition(where);
			Point loc = port.getViewLocation();

			if ((region & PositionConstants.SOUTH) != 0)
				loc.y += scrollOffset;
			else if ((region & PositionConstants.NORTH) != 0)
				loc.y -= scrollOffset;

			if ((region & PositionConstants.EAST) != 0)
				loc.x += scrollOffset;
			else if ((region & PositionConstants.WEST) != 0)
				loc.x -= scrollOffset;

			port.setViewLocation(loc);
			return true;
		}
	}

	private RootAutoexposeHelper exposeHelper;
	private boolean isAutoexpose;

	public RootEditPartDragTracker(GraphicalEditPart editpart) {
		exposeHelper = new RootAutoexposeHelper(editpart);
		isAutoexpose = false;
	}

	protected void doAutoexpose() {
		if (!isAutoexpose)
			return;
		if (exposeHelper.step(getLocation())) {
			//showMarqueeFeedback(); //should not in timer, when event is over, viewer is null.
			Display.getCurrent().asyncExec(new QueuedAutoexpose());
		} else
			isAutoexpose = false;
	}

	class QueuedAutoexpose implements Runnable {
		public void run() {
			if (exposeHelper != null)
				doAutoexpose();
		}
	}

	private boolean isInDragInProgress() {
		return isInState(STATE_DRAG_IN_PROGRESS
				| STATE_ACCESSIBLE_DRAG_IN_PROGRESS);
	}

	protected boolean handleHover() {
		if (isInDragInProgress()) {
			if (!isAutoexpose){
				if (exposeHelper.detect(getLocation())) {
					isAutoexpose = true;
					Display.getCurrent().asyncExec(new QueuedAutoexpose());
				}
			}else{
				showMarqueeFeedback();
			}
		}
		return true;
	}

	class MarqueeRectangleFigure extends Figure {

		private static final int DELAY = 110; // animation delay in millisecond
		private int offset = 0;
		private boolean schedulePaint = true;

		/**
		 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
		 */
		protected void paintFigure(Graphics graphics) {
			Rectangle bounds = getBounds().getCopy();
			graphics.translate(getLocation());

			graphics.setXORMode(true);
			graphics.setForegroundColor(ColorConstants.white);
			graphics.setBackgroundColor(ColorConstants.black);
			graphics.setLineStyle(Graphics.LINE_DOT);

			int[] points = new int[6];

			points[0] = 0 + offset;
			points[1] = 0;
			points[2] = bounds.width - 1;
			points[3] = 0;
			points[4] = bounds.width - 1;
			points[5] = bounds.height - 1;

			graphics.drawPolyline(points);

			points[0] = 0;
			points[1] = 0 + offset;
			points[2] = 0;
			points[3] = bounds.height - 1;
			points[4] = bounds.width - 1;
			points[5] = bounds.height - 1;

			graphics.drawPolyline(points);

			graphics.translate(getLocation().getNegated());

			if (schedulePaint) {
				Display.getCurrent().timerExec(DELAY, new Runnable() {
					public void run() {
						offset++;
						if (offset > 5)
							offset = 0;

						schedulePaint = true;
						repaint();
					}
				});
			}

			schedulePaint = false;
		}
	}

	public static final int BEHAVIOR_NODES_CONTAINED = new Integer(1)
			.intValue();

	public static final int BEHAVIOR_CONNECTIONS_TOUCHED = new Integer(2)
			.intValue();

	public static final int BEHAVIOR_NODES_CONTAINED_AND_RELATED_CONNECTIONS = new Integer(
			3).intValue();

	public static final int BEHAVIOR_NODES_AND_CONNECTIONS = BEHAVIOR_NODES_CONTAINED_AND_RELATED_CONNECTIONS;

	static final int DEFAULT_MODE = 0;
	static final int LINKSOURCE_MODE = 2;

	private static final Request MARQUEE_REQUEST = new Request(
			RequestConstants.REQ_SELECTION);

	private Set allChildren = new HashSet();
	private Figure marqueeRectangleFigure;
	private int mode;

	private Collection selectedEditParts;

	private Request targetRequest;


	public RootEditPartDragTracker() {
		setDefaultCursor(SharedCursors.CROSS);
		setUnloadWhenFinished(false);
	}

	protected Collection calculateMarqueeSelectedEditParts() {
		Collection marqueeSelectedEditParts = new HashSet();
		marqueeSelectedEditParts
				.addAll(calculatePrimaryMarqueeSelectedEditParts());
		marqueeSelectedEditParts
				.addAll(calculateSecondaryMarqueeSelectedEditParts(marqueeSelectedEditParts));
		return marqueeSelectedEditParts;
	}

	private Collection calculatePrimaryMarqueeSelectedEditParts() {
		Collection editPartsToProcess = new HashSet();

		Collection conenctionsToProcess = new HashSet();
		
		editPartsToProcess.addAll(EditPartUtilities
				.getAllChildren((GraphicalEditPart) getCurrentViewer()
						.getRootEditPart()));

		// process connections
		conenctionsToProcess
				.addAll(EditPartUtilities
						.getAllNestedConnectionEditParts((GraphicalEditPart) getCurrentViewer()
								.getRootEditPart()));


		// process all edit parts and determine which are affected by the
		// current marquee selection
		Collection marqueeSelectedEditParts = new ArrayList();
		boolean nodeSelected=false;
		for (Iterator iterator = editPartsToProcess.iterator(); iterator
				.hasNext();) {
			GraphicalEditPart editPart = (GraphicalEditPart) iterator.next();
			if (isMarqueeSelectable(editPart)
					&& isPrimaryMarqueeSelectedEditPart(editPart)) {
				nodeSelected=true;
				marqueeSelectedEditParts.add(editPart);
			}
		}
	
		if (!nodeSelected){
			for (Iterator iterator = conenctionsToProcess.iterator(); iterator
					.hasNext();) {
				GraphicalEditPart editPart = (GraphicalEditPart) iterator.next();
				if (isMarqueeSelectable(editPart)
						&& isPrimaryMarqueeSelectedEditPart(editPart)) {
					marqueeSelectedEditParts.add(editPart);
				}
			}
		}
		
		return marqueeSelectedEditParts;
	}

	private Collection calculateSecondaryMarqueeSelectedEditParts(
			Collection directlyMarqueeSelectedEditParts) {

		Collection secondaryMarqueeSelectedEditParts = new HashSet();
		if (mode == LINKSOURCE_MODE)
		for (Iterator iterator = directlyMarqueeSelectedEditParts.iterator(); iterator
				.hasNext();) {
			GraphicalEditPart marqueeSelectedEditPart = (GraphicalEditPart) iterator
					.next();
			List<ConnectionEditPart> connections = marqueeSelectedEditPart.getTargetConnections();
			for (ConnectionEditPart connection : connections){
				secondaryMarqueeSelectedEditParts.add(connection.getSource());
			}
		}
		return secondaryMarqueeSelectedEditParts;
	}

	private Request createTargetRequest() {
		return MARQUEE_REQUEST;
	}

	public void deactivate() {
		isAutoexpose = false;
		if (isInState(STATE_DRAG_IN_PROGRESS)) {
			eraseMarqueeFeedback();
			//eraseTargetFeedback();
		}
		super.deactivate();
		allChildren.clear();
		setState(STATE_TERMINAL);
	}

	private void eraseMarqueeFeedback() {
		if (marqueeRectangleFigure != null) {
			removeFeedback(marqueeRectangleFigure);
			marqueeRectangleFigure = null;
		}
	}

	private void eraseTargetFeedback() {
		if (selectedEditParts == null)
			return;
		Iterator oldEditParts = selectedEditParts.iterator();
		while (oldEditParts.hasNext()) {
			EditPart editPart = (EditPart) oldEditParts.next();
			editPart.eraseTargetFeedback(getTargetRequest());
		}
	}

	protected String getCommandName() {
		return REQ_SELECTION;
	}

	protected Rectangle getCurrentMarqueeSelectionRectangle() {
		return new Rectangle(getStartLocation(), getLocation());
	}

	@Override
	protected Point getLocation() {
		RangeModel hori = exposeHelper.port.getHorizontalRangeModel();
		RangeModel vert = exposeHelper.port.getVerticalRangeModel();
		Point p=super.getLocation();
		p.translate(hori.getValue(), vert.getValue());
		return p;
	}
	
	@Override
	protected void setStartLocation(Point p) {
		RangeModel hori = exposeHelper.port.getHorizontalRangeModel();
		RangeModel vert = exposeHelper.port.getVerticalRangeModel();
		p.translate(hori.getValue(), vert.getValue());
		super.setStartLocation(p);
	}

	protected int getCurrentSelectionMode() {
		return mode;
	}

	private IFigure getMarqueeFeedbackFigure() {
		if (marqueeRectangleFigure == null) {
			marqueeRectangleFigure = new MarqueeRectangleFigure();
			addFeedback(marqueeRectangleFigure);
		}
		return marqueeRectangleFigure;
	}

	private Request getTargetRequest() {
		if (targetRequest == null)
			targetRequest = createTargetRequest();
		return targetRequest;
	}

	protected boolean handleButtonDown(int button) {
		if (!isCurrentViewerGraphical())
			return true;
		if (button != 1) {
			setState(STATE_INVALID);
			handleInvalidInput();
		}
		if (stateTransition(STATE_INITIAL, STATE_DRAG_IN_PROGRESS)) {
			if (getCurrentInput().isShiftKeyDown())
				setSelectionMode(LINKSOURCE_MODE);
			else
				setSelectionMode(DEFAULT_MODE);
		}
		return true;
	}

	protected boolean handleButtonUp(int button) {
		if (stateTransition(STATE_DRAG_IN_PROGRESS, STATE_TERMINAL)) {
			//selectedEditParts = calculateMarqueeSelectedEditParts();
			//eraseTargetFeedback();
			eraseMarqueeFeedback();
			performMarqueeSelect();
		}
		handleFinished();
		return true;
	}

	protected boolean handleDragInProgress() {
		if (!isAutoexpose) {
			if (exposeHelper.detect(getLocation())) {
				isAutoexpose = true;
				Display.getCurrent().asyncExec(new QueuedAutoexpose());
			}
		}else{
			showMarqueeFeedback();
		}

		if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
			showMarqueeFeedback();
			//eraseTargetFeedback();
			//selectedEditParts = calculateMarqueeSelectedEditParts();
			//showTargetFeedback();
		}
		return true;
	}

	protected boolean handleFocusLost() {
		if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
			handleFinished();
			return true;
		}
		return false;
	}

	protected boolean handleInvalidInput() {
		//eraseTargetFeedback();
		eraseMarqueeFeedback();
		return true;
	}

	protected boolean handleKeyDown(KeyEvent e) {
		if (super.handleKeyDown(e))
			return true;
		if (getCurrentViewer().getKeyHandler() != null)
			return getCurrentViewer().getKeyHandler().keyPressed(e);
		return false;
	}

	protected boolean isMarqueeSelectable(GraphicalEditPart editPart) {
		// IMPORTANT: MarqueeSelectionTool is not a TargetingTool, thus the
		// pre-selection does not depend on hit-testing. Therefore, the visible
		// state of the edit part's figure has to be taken into consideration as
		// well.
		return editPart.getTargetEditPart(MARQUEE_REQUEST) == editPart
				&& editPart.isSelectable();
	}

	private boolean isPrimaryMarqueeSelectedEditPart(GraphicalEditPart editPart) {
		// figure bounds are used to determine if edit part is included in
		// selection
		IFigure figure = editPart.getFigure();
		Rectangle r = figure.getBounds().getCopy();
		//figure.translateToAbsolute(r);

		boolean included = false;
		Rectangle marqueeSelectionRectangle = getCurrentMarqueeSelectionRectangle();
		if (editPart instanceof ConnectionEditPart) {
				if (marqueeSelectionRectangle.intersects(r)) {
					// children will contain ConnectionEditParts only in case
					// behavior is BEHAVIOR_CONNECTIONS_TOUCHED or
					// BEHAVIOR_CONNECTIONS_CONTAINED
					Rectangle relMarqueeRect = Rectangle.SINGLETON;
					figure.translateToRelative(relMarqueeRect
							.setBounds(marqueeSelectionRectangle));
					included = ((Connection) figure).getPoints()
							.intersects(marqueeSelectionRectangle);
				}
		} else {
			included = marqueeSelectionRectangle.contains(r);
		}
		return included;
	}


	protected boolean isViewerImportant(EditPartViewer viewer) {
		return isCurrentViewerGraphical();
	}

	private boolean isCurrentViewerGraphical() {
		return getCurrentViewer() instanceof GraphicalViewer;
	}

	protected void performMarqueeSelect() {
		// determine which edit parts are affected by the current marquee
		// selection
		Collection marqueeSelectedEditParts = calculateMarqueeSelectedEditParts();

		// calculate nodes/connections that are to be selected/deselected,
		// dependent on the current mode of the tool
		Collection editPartsToSelect = new LinkedHashSet();
		for (Iterator iterator = marqueeSelectedEditParts.iterator(); iterator
				.hasNext();) {
			EditPart affectedEditPart = (EditPart) iterator.next();
			if (affectedEditPart.getSelected() == EditPart.SELECTED_NONE)
				editPartsToSelect.add(affectedEditPart);
		}

		// include the current viewer selection, if not in DEFAULT mode.
		if (getCurrentSelectionMode() != DEFAULT_MODE) {
			editPartsToSelect.addAll(getCurrentViewer().getSelectedEditParts());
		}

		getCurrentViewer().setSelection(
				new StructuredSelection(editPartsToSelect.toArray()));
	}

	private void setSelectionMode(int mode) {
		this.mode = mode;
	}

	/**
	 * @see org.eclipse.gef.Tool#setViewer(org.eclipse.gef.EditPartViewer)
	 */
	public void setViewer(EditPartViewer viewer) {
		if (viewer == getCurrentViewer())
			return;
		super.setViewer(viewer);
		if (viewer instanceof GraphicalViewer)
			setDefaultCursor(SharedCursors.CROSS);
		else
			setDefaultCursor(SharedCursors.NO);
	}

	private void showMarqueeFeedback() {
		Rectangle rect = getCurrentMarqueeSelectionRectangle().getCopy();
		IFigure marqueeFeedbackFigure = getMarqueeFeedbackFigure();
		//marqueeFeedbackFigure.translateToRelative(rect);// redundant
		marqueeFeedbackFigure.setBounds(rect);
		marqueeFeedbackFigure.validate();
	}

	private void showTargetFeedback() {
		for (Iterator itr = selectedEditParts.iterator(); itr.hasNext();) {
			EditPart editPart = (EditPart) itr.next();
			editPart.showTargetFeedback(getTargetRequest());
		}
	}

}
