/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
?* All rights reserved. This program and the accompanying materials
?* are made available under the terms of the Eclipse Public License v1.0
?* which accompanies this distribution, and is available at
?* http://www.eclipse.org/legal/epl-v10.html
?*
?* Contributors:
?*????Elias Volanakis - initial API and implementation
?*******************************************************************************/
package debugger.shapes.parts;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import debugger.shapes.ShapesEditor;
import debugger.shapes.model.Connection;
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;


/**
 * Factory that maps model elements to edit parts.
 * 
 * @author Elias Volanakis
 */
public class ShapesEditPartFactory implements EditPartFactory {

	private ShapesEditor editor;

	public ShapesEditPartFactory(ShapesEditor editor){
		this.editor=editor;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.EditPartFactory#createEditPart(org.eclipse.gef.EditPart,
	 * java.lang.Object)
	 */
	public EditPart createEditPart(EditPart context, Object modelElement) {
		// get EditPart for model element
		EditPart part = getPartForElement(modelElement);
		// store model element in EditPart
		part.setModel(modelElement);
		return part;
	}

	/**
	 * Maps an object to an EditPart.
	 * 
	 * @throws RuntimeException
	 *             if no match was found (programming error)
	 */
	private EditPart getPartForElement(Object modelElement) {
		if (modelElement instanceof ShapesDiagram) {
			return new DiagramEditPart();
		}
		if (modelElement instanceof Shape) {
			return new ShapeEditPart(editor);
		}
		if (modelElement instanceof Connection) {
			return new ConnectionEditPart();
		}
		throw new RuntimeException("Can't create part for model element: "
				+ ((modelElement != null) ? modelElement.getClass().getName()
						: "null"));
	}

}