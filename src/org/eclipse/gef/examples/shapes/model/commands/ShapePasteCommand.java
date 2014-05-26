package org.eclipse.gef.examples.shapes.model.commands;

import java.util.Map;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;

import org.eclipse.gef.examples.shapes.ShapesEditor;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.ShapesDiagram;
import org.eclipse.gef.requests.CreateRequest;

public class ShapePasteCommand extends Command {

	/** The new shape. */
	private Shape[] newShapes;
	/** ShapeDiagram to add to. */
	private final ShapesDiagram parent;

	public ShapePasteCommand(Shape[] newShapes, ShapesDiagram parent) {
		this.newShapes = newShapes;
		this.parent = parent;
		setLabel("shape paste");
	}

	public boolean canExecute() {
		return newShapes != null && newShapes.length > 0 && parent != null;
	}

	public void execute() {
		/*Point location = request.getLocation();
		figure.translateToRelative(location);
		figure.translateFromParent(location);
		//Point negatedLayoutOrigin = figure.getClientArea().getLocation().getNegated();
		//location.performTranslate(negatedLayoutOrigin.x, negatedLayoutOrigin.y);
		
		for (Shape shape:newShapes){
			shape.setLocation(location);
			location.translate(0, 25);
		}*/
		
		redo();
	}


	public void redo() {
		for (Shape shape:newShapes){
			parent.addChild(shape);
		}
		
		GraphicalViewer viewer = ((ShapesEditor)parent.getEditor()).getGraphicalViewer();
		Map registry = viewer.getEditPartRegistry();
		
		if (registry != null){
			viewer.deselectAll();
			for (Shape shape : newShapes){
				EditPart part=(EditPart)registry.get(shape);
				if (part!=null)
					viewer.appendSelection(part);
			}
		}
	}


	public void undo() {
		for (Shape shape:newShapes){
			parent.removeChild(shape);
		}
	}

}