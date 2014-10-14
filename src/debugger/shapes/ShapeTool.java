package debugger.shapes;

import org.eclipse.swt.graphics.Cursor;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.SharedCursors;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.TargetingTool;
import org.eclipse.ui.IFileEditorInput;

import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;
import debugger.shapes.parts.DiagramEditPart;

/**
 * The CreationTool creates new {@link EditPart EditParts} via a
 * {@link CreationFactory}. If the user simply clicks on the viewer, the default
 * sized EditPart will be created at that point. If the user clicks and drags,
 * the created EditPart will be sized based on where the user clicked and
 * dragged.
 */
public class ShapeTool extends TargetingTool {

	private ShapeFactory factory;
	public abstract static class ShapeFactory implements CreationFactory{
		private ShapeTool tool;

		//call getExplorerSelection, if null, call getEditorSelection
		//the parameter is a shape.
		@Override
		public Object getNewObject() {
			DiagramEditPart diagrampart=(DiagramEditPart)tool.getCurrentViewer().getRootEditPart().getContents();
			ShapesDiagram diagram=(ShapesDiagram)diagrampart.getModel();
			boolean succeed=false;
			Shape[] shapes=null;
			
			Object[] selection = diagram.uiutil.prepareSelection();
			if (selection.length>0){
				Shape[] dummy=getObject(selection.length);
				shapes=diagram.uiutil.getExplorerSelection(dummy, selection);
				if (shapes.length>0)
					succeed=true;
			}
			
			if (!succeed){
				shapes=getObject(1);
				succeed=diagram.uiutil.getEditorSelection(shapes[0]);
			}
			if (!succeed){
				return null;
			}
			return shapes;
		}

		@Override
		public Object getObjectType() {
			return Shape[].class;
		}
		
		protected abstract Shape[] getObject(int num);

		public void setTool(ShapeTool tool) {
			this.tool=tool;
		}
	};
	private SnapToHelper helper;

	/**
	 * Default constructor. Sets the default and disabled cursors.
	 */
	public ShapeTool(ShapeFactory factory) {
		this.factory=factory;
		factory.setTool(this);
		setDefaultCursor(SharedCursors.CURSOR_TREE_MOVE);
		setDisabledCursor(SharedCursors.NO);
	}

	@Override
	public EditPartViewer getCurrentViewer() {
		return super.getCurrentViewer();
	}

	/**
	 * Creates a {@link CreateRequest} and sets this tool's factory on the
	 * request.
	 * 
	 * @see org.eclipse.gef.tools.TargetingTool#createTargetRequest()
	 */
	protected Request createTargetRequest() {
		CreateRequest request = new CreateRequest();
		request.setFactory(getFactory());
		return request;
	}

	/**
	 * @see org.eclipse.gef.Tool#deactivate()
	 */
	public void deactivate() {
		super.deactivate();
		helper = null;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#getCommandName()
	 */
	protected String getCommandName() {
		return REQ_CREATE;
	}

	/**
	 * Cast the target request to a CreateRequest and returns it.
	 * 
	 * @return the target request as a CreateRequest
	 * @see TargetingTool#getTargetRequest()
	 */
	protected CreateRequest getCreateRequest() {
		return (CreateRequest) getTargetRequest();
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#getDebugName()
	 */
	protected String getDebugName() {
		return "Creation Tool";//$NON-NLS-1$
	}

	/**
	 * Returns the creation factory used to create the new EditParts.
	 * 
	 * @return the creation factory
	 */
	protected CreationFactory getFactory() {
		return factory;
	}

	/**
	 * The creation tool only works by clicking mouse button 1 (the left mouse
	 * button in a right-handed world). If any other button is pressed, the tool
	 * goes into an invalid state. Otherwise, it goes into the drag state,
	 * updates the request's location and calls
	 * {@link TargetingTool#lockTargetEditPart(EditPart)} with the edit part
	 * that was just clicked on.
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#handleButtonDown(int)
	 */
	protected boolean handleButtonDown(int button) {
		if (button != 1) {
			setState(STATE_INVALID);
			handleInvalidInput();
			return true;
		}
		if (stateTransition(STATE_INITIAL, STATE_DRAG)) {
			lockTargetEditPart(getTargetEditPart());
			updateTargetRequest();
		}
		return true;
	}

	@Override
	protected boolean handleEnteredEditPart() {
		// Snap only when size on drop is employed
		if (getTargetEditPart() != null)
			helper = (SnapToHelper) getTargetEditPart().getAdapter(
					SnapToHelper.class);
		return super.handleEnteredEditPart();
	}
	
	/**
	 * If the tool is currently in a drag or drag-in-progress state, it goes
	 * into the terminal state, performs some cleanup (erasing feedback,
	 * unlocking target edit part), and then calls {@link #performCreation(int)}
	 * .
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#handleButtonUp(int)
	 */
	protected boolean handleButtonUp(int button) {
		if (stateTransition(STATE_DRAG | STATE_DRAG_IN_PROGRESS, STATE_TERMINAL)) {
			eraseTargetFeedback();
			unlockTargetEditPart();
			performCreation(button);
		}

		setState(STATE_TERMINAL);
		handleFinished();

		return true;
	}

	/**
	 * Updates the request, sets the current command, and asks to show feedback.
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#handleDragInProgress()
	 */
	protected boolean handleDragInProgress() {
		if (isInState(STATE_DRAG_IN_PROGRESS)) {
			updateTargetRequest();
			setCurrentCommand(getCommand());
			showTargetFeedback();
		}
		return true;
	}

	/**
	 * @see org.eclipse.gef.tools.AbstractTool#handleDragStarted()
	 */
	protected boolean handleDragStarted() {
		return stateTransition(STATE_DRAG, STATE_DRAG_IN_PROGRESS);
	}

	/**
	 * If the user is in the middle of creating a new edit part, the tool erases
	 * feedback and goes into the invalid state when focus is lost.
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#handleFocusLost()
	 */
	protected boolean handleFocusLost() {
		if (isInState(STATE_DRAG | STATE_DRAG_IN_PROGRESS)) {
			eraseTargetFeedback();
			setState(STATE_INVALID);
			handleFinished();
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.gef.tools.TargetingTool#handleHover()
	 */
	protected boolean handleHover() {
		if (isInState(STATE_INITIAL))
			updateAutoexposeHelper();
		return true;
	}

	/**
	 * Updates the request and mouse target, gets the current command and asks
	 * to show feedback.
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#handleMove()
	 */
	protected boolean handleMove() {
		updateTargetRequest();
		updateTargetUnderMouse();
		setCurrentCommand(getCommand());
		showTargetFeedback();
		return true;
	}

	/**
	 * Executes the current command and selects the newly created object. The
	 * button that was released to cause this creation is passed in, but since
	 * {@link #handleButtonDown(int)} goes into the invalid state if the button
	 * pressed is not button 1, this will always be button 1.
	 * 
	 * @param button
	 *            the button that was pressed
	 */
	protected void performCreation(int button) {
		EditPartViewer viewer = getCurrentViewer();
		executeCurrentCommand();
		selectAddedObject(viewer);
	}

	/*
	 * Add the newly created object to the viewer's selected objects.
	 */
	private void selectAddedObject(EditPartViewer viewer) {
		final Shape[] shapes = (Shape[])getCreateRequest().getNewObject();
		if (shapes == null || viewer == null)
			return;
		viewer.deselectAll();
		DiagramEditPart diagrampart=(DiagramEditPart)getCurrentViewer().getRootEditPart().getContents();
		ShapesDiagram diagram=(ShapesDiagram)diagrampart.getModel();
		for (Shape model:shapes){
			diagram.uiutil.applySelection(model);
			Object editpart = viewer.getEditPartRegistry().get(model);
			if (editpart != null && editpart instanceof EditPart
					&& ((EditPart) editpart).isSelectable()) {
				// Force the new object to get positioned in the viewer.
				viewer.appendSelection((EditPart) editpart);
			}
		}
		viewer.flush();
	}

	/**
	 * Sets the location (and size if the user is performing size-on-drop) of
	 * the request.
	 * 
	 * @see org.eclipse.gef.tools.TargetingTool#updateTargetRequest()
	 */
	protected void updateTargetRequest() {
		CreateRequest createRequest = getCreateRequest();
			
		Point loq = getStartLocation().getTranslated(getDragMoveDelta());
		Rectangle bounds = new Rectangle(loq, loq.getTranslated(25, 25));

		if (helper != null) {
			PrecisionRectangle baseRect = new PrecisionRectangle(bounds);
			PrecisionRectangle result = baseRect.getPreciseCopy();
			helper.snapRectangle(createRequest, PositionConstants.NSEW,
					baseRect, result);
			createRequest.setLocation(result.getLocation());
			createRequest.setSize(result.getSize());
		}else{
			createRequest.setSize(bounds.getSize());
			createRequest.setLocation(bounds.getLocation());
			createRequest.getExtendedData().clear();
		}

	}
}
