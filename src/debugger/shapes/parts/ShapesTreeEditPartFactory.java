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
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;

/**
 * Factory that maps model elements to TreeEditParts. TreeEditParts are used in
 * the outline view of the ShapesEditor.
 * 
 * @author Elias Volanakis
 */
public class ShapesTreeEditPartFactory implements EditPartFactory {

	private ShapesEditor editor;

	public ShapesTreeEditPartFactory(ShapesEditor editor){
		this.editor=editor;
	}

	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof Shape) {
			return new ShapeTreeEditPart((Shape) model, editor);
		}
		if (model instanceof ShapesDiagram) {
			return new DiagramTreeEditPart((ShapesDiagram) model);
		}
		return null; // will not show an entry for the corresponding model
						// instance
	}

}
