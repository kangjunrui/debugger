/*******************************************************************************
 * Copyright (c) 2004, 2010 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elias Volanakis - initial API and implementation
 *******************************************************************************/
package org.eclipse.gef.examples.shapes;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.gef.Tool;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteToolbar;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.tools.CreationTool;

import org.eclipse.gef.examples.shapes.model.Connection;
import org.eclipse.gef.examples.shapes.model.EllipticalShape;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;

/**
 * Utility class that can create a GEF Palette.
 * 
 * @see #createPalette()
 * @author Elias Volanakis
 */
final class ShapesEditorPaletteFactory {

	/** Create the "Shapes" drawer. */
	private static PaletteContainer createShapesDrawer() {
		PaletteDrawer componentsDrawer = new PaletteDrawer("Shapes");

		ToolEntry shape1 = new ToolEntry(
				"Shapes", "Create shapes", 
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/rectangle16.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/rectangle24.gif")){@Override
						public Tool createTool() {
							return new ShapeTool(new ShapeTool.ShapeFactory(){
								@Override
								protected Shape[] getObject(int num) {
									RectangularShape[] shapes=new RectangularShape[num];
									for (int i=0;i<num;i++){
										shapes[i]=new RectangularShape();
									}
									return shapes;
								}
							});
						}};
		componentsDrawer.add(shape1);

		ToolEntry shape2 = new ToolEntry(
				"Shapes", "Create shapes", 
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/ellipse16.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/ellipse24.gif")){
			public Tool createTool() {
				return new ShapeTool(new ShapeTool.ShapeFactory(){
					@Override
					protected Shape[] getObject(int num) {
						EllipticalShape[] shapes=new EllipticalShape[num];
						for (int i=0;i<num;i++){
							shapes[i]=new EllipticalShape();
						}
						return shapes;
					}
				});
			}
		};
		componentsDrawer.add(shape2);
			
		/*
		ToolEntry container = new ToolEntry("Rectangle", "Create a rectangular shape", 
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/ellipse16.gif"), ImageDescriptor.createFromFile(
						ShapesPlugin.class, "icons/ellipse24.gif")
				);
		componentsDrawer.add(container);
		 */
		
		return componentsDrawer;
	}

	/**
	 * Creates the PaletteRoot and adds all palette elements. Use this factory
	 * method to create a new palette for your graphical editor.
	 * 
	 * @return a new PaletteRoot
	 */
	static PaletteRoot createPalette() {
		PaletteRoot palette = new PaletteRoot();
		palette.add(createToolsGroup(palette));
		palette.add(createShapesDrawer());
		return palette;
	}

	public static class DragTool extends CreationTool{
		{
			setUnloadWhenFinished(false);
		}
	}
	
	/** Create the "Tools" group. */
	private static PaletteContainer createToolsGroup(PaletteRoot palette) {
		PaletteToolbar toolbar = new PaletteToolbar("Tools");

		// Add a selection tool to the group
		ToolEntry tool = new PanningSelectionToolEntry();
		toolbar.add(tool);
		palette.setDefaultEntry(tool);

		tool = new ToolEntry("Rectangle",
				"Create a rectangular shape",
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/rectangle16.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/rectangle24.gif"),
						DragTool.class){
			{
				setToolProperty(DragTool.PROPERTY_CREATION_FACTORY, new SimpleFactory(RectangularShape.class));
			}
		};
		toolbar.add(tool);

		// Add (solid-line) connection tool
		tool = new ConnectionCreationToolEntry("Solid connection",
				"Create a solid-line connection", new CreationFactory() {
					public Object getNewObject() {
						return null;
					}

					// see ShapeEditPart#createEditPolicies()
					// this is abused to transmit the desired line style
					public Object getObjectType() {
						return Connection.SOLID_CONNECTION;
					}
				}, ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/connection_s16.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/connection_s24.gif"));
		toolbar.add(tool);

		// Add (dashed-line) connection tool
		tool = new ConnectionCreationToolEntry("Dot connection",
				"Create a dot-line connection", new CreationFactory() {
					public Object getNewObject() {
						return null;
					}

					// see ShapeEditPart#createEditPolicies()
					// this is abused to transmit the desired line style
					public Object getObjectType() {
						return Connection.DOT_CONNECTION;
					}
				}, ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/connection_d16.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/connection_d24.gif"));
		toolbar.add(tool);

		return toolbar;
	}

	/** Utility class. */
	private ShapesEditorPaletteFactory() {
		// Utility class
	}

}