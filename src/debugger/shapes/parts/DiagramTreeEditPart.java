package debugger.shapes.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;

import debugger.shapes.model.ModelElement;
import debugger.shapes.model.ShapesDiagram;

/**
 * TreeEditPart for a ShapesDiagram instance. This is used in the Outline View
 * of the ShapesEditor.
 */
class DiagramTreeEditPart extends AbstractTreeEditPart implements
		PropertyChangeListener {

	/**
	 * Create a new instance of this edit part using the given model element.
	 * 
	 * @param model
	 *            a non-null ShapesDiagram instance
	 */
	DiagramTreeEditPart(ShapesDiagram model) {
		super(model);
	}

	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	public void activate() {
		if (!isActive()) {
			super.activate();
			((ModelElement) getModel()).addPropertyChangeListener(this);
		}
	}


	protected void createEditPolicies() {
		// If this editpart is the root content of the viewer, then disallow
		// removal
		if (getParent() instanceof RootEditPart) {
			installEditPolicy(EditPolicy.COMPONENT_ROLE,
					new RootComponentEditPolicy());
		}
	}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			((ModelElement) getModel()).removePropertyChangeListener(this);
		}
	}

	private ShapesDiagram getCastedModel() {
		return (ShapesDiagram) getModel();
	}

	/**
	 * Convenience method that returns the EditPart corresponding to a given
	 * child.
	 * 
	 * @param child
	 *            a model element instance
	 * @return the corresponding EditPart or null
	 */
	private EditPart getEditPartForChild(Object child) {
		return (EditPart) getViewer().getEditPartRegistry().get(child);
	}

	
	//return an array of temp objects for group integer
	protected List getModelChildren() {
		return getCastedModel().getChildren(); // a list of shapes
	}


	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (ShapesDiagram.CHILD_ADDED_PROP.equals(prop)) {
			// add a child to this edit part
			// causes an additional entry to appear in the tree of the outline
			// view
			addChild(createChild(evt.getNewValue()), -1);
		} else if (ShapesDiagram.CHILD_REMOVED_PROP.equals(prop)) {
			// remove a child from this edit part
			// causes the corresponding edit part to disappear from the tree in
			// the outline view
			removeChild(getEditPartForChild(evt.getNewValue()));
		} else {
			refreshVisuals();
		}
	}
}
