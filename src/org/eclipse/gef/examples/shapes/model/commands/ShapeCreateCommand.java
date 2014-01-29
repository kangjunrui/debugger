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
package org.eclipse.gef.examples.shapes.model.commands;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.commands.Command;

import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.ShapesDiagram;
import org.eclipse.gef.requests.CreateRequest;

/**
 * A command to add a Shape to a ShapeDiagram. The command can be undone or
 * redone.
 * 
 * @author Elias Volanakis
 */
public class ShapeCreateCommand extends Command {

	/** The new shape. */
	private Shape[] newShapes;
	/** ShapeDiagram to add to. */
	private final ShapesDiagram parent;
	private CreateRequest request;
	private IFigure figure;

	/**
	 * Create a command that will add a new Shape to a ShapesDiagram.
	 * 
	 * @param newShape
	 *            the new Shape that is to be added
	 * @param parent
	 *            the ShapesDiagram that will hold the new element
	 * @param bounds
	 *            the bounds of the new shape; the size can be (-1, -1) if not
	 *            known
	 * @throws IllegalArgumentException
	 *             if any parameter is null, or the request does not provide a
	 *             new Shape instance
	 */
	public ShapeCreateCommand(Shape[] newShapes, ShapesDiagram parent,
			CreateRequest request, IFigure figure) {
		this.newShapes = newShapes;
		this.parent = parent;
		this.request = request;
		this.figure = figure;
		setLabel("shape creation");
	}

	/**
	 * Can execute if all the necessary information has been provided.
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		return newShapes != null && newShapes.length > 0 && parent != null && request != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		Point location = request.getLocation();
		figure.translateToRelative(location);
		figure.translateFromParent(location);
		//Point negatedLayoutOrigin = figure.getClientArea().getLocation().getNegated();
		//location.performTranslate(negatedLayoutOrigin.x, negatedLayoutOrigin.y);
		
		for (Shape shape:newShapes){
			shape.setLocation(location);
			location.translate(0, 25);
		}
		
		redo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		for (Shape shape:newShapes){
			parent.addChild(shape);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		for (Shape shape:newShapes){
			parent.removeChild(shape);
		}
	}

}