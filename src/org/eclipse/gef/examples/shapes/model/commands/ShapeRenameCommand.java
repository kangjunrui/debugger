/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
锟�All rights reserved. This program and the accompanying materials
锟�are made available under the terms of the Eclipse Public License v1.0
锟�which accompanies this distribution, and is available at
锟�http://www.eclipse.org/legal/epl-v10.html
锟�锟�Contributors:
锟界墜鐗婨lias Volanakis - initial API and implementation
锟�*****************************************************************************/
package org.eclipse.gef.examples.shapes.model.commands;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.examples.shapes.model.Shape;

/**
 * A command to add a Shape to a ShapeDiagram. The command can be undone or
 * redone.
 * 
 * @author Elias Volanakis
 */
public class ShapeRenameCommand extends Command {

	private Shape model;
	private String newname;
	private String oldname;
	
	public ShapeRenameCommand(Shape model, String name) {
		this.model = model;
		this.newname = name;
		this.oldname = model.getName();
		setLabel("shape rename");
	}

	/**
	 * Can execute if all the necessary information has been provided.
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		return model != null && newname != null;
	}

	public void execute() {
		redo();
	}

	public void redo() {
		model.setName(newname);
	}

	public void undo() {
		model.setName(oldname);
	}

}