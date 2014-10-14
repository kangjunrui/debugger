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
package debugger.shapes.model;

import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import org.eclipse.draw2d.Graphics;

import debugger.shapes.properties.CommentPropertyDescriptor;

/**
 * A connection between two distinct shapes.
 * 
 * @author Elias Volanakis
 */
public class Connection extends ModelElement {
	/**
	 * Used for indicating that a Connection with solid line style should be
	 * created.
	 * 
	 * @see debugger.shapes.parts.ShapeEditPart#createEditPolicies()
	 */
	public static final Integer SOLID_CONNECTION = new Integer(
			Graphics.LINE_SOLID);
	/**
	 * Used for indicating that a Connection with dashed line style should be
	 * created.
	 * 
	 * @see debugger.shapes.parts.ShapeEditPart#createEditPolicies()
	 */
	public static final Integer DOT_CONNECTION = new Integer(
			Graphics.LINE_DOT);
	/** Property ID to use when the line style of this connection is modified. */
	public static final String LINESTYLE_PROP = "LineStyle";
	public static final String LINENOTE_PROP = "LineNote";
	private static final IPropertyDescriptor[] descriptors = new IPropertyDescriptor[2];
	private static final String SOLID_STR = "Solid";
	private static final String DOT_STR = "Dot";
	private static final long serialVersionUID = 1;

	/** True, if the connection is attached to its endpoints. */
	private boolean isConnected;
	/** Connection's source endpoint. */
	private Shape source;
	/** Connection's target endpoint. */
	private Shape target;

	static {
		descriptors[0] = new ComboBoxPropertyDescriptor(LINESTYLE_PROP,
				LINESTYLE_PROP, new String[] { SOLID_STR, DOT_STR });
		descriptors[1] = new CommentPropertyDescriptor(LINENOTE_PROP, LINENOTE_PROP);
	}

	/**
	 * Create a (solid) connection between two distinct shapes.
	 * 
	 * @param source
	 *            a source endpoint for this connection (non null)
	 * @param target
	 *            a target endpoint for this connection (non null)
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null or source == target
	 * @see #setLineStyle(int)
	 */
	public Connection(Shape source, Shape target) {
		reconnect(source, target);
	}

	/**
	 * Disconnect this connection from the shapes it is attached to.
	 */
	public void disconnect() {
		if (isConnected) {
			source.removeConnection(this);
			target.removeConnection(this);
			isConnected = false;
		}
	}



	/**
	 * Returns the descriptor for the lineStyle property
	 * 
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return descriptors;
	}

	/**
	 * Returns the lineStyle as String for the Property Sheet
	 * 
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
	 */
	public Object getPropertyValue(Object id) {
		if (id.equals(LINESTYLE_PROP)) {
			if (getLineStyle() == Graphics.LINE_DOT)
				// Dashed is the second value in the combo dropdown
				return new Integer(1);
			// Solid is the first value in the combo dropdown
			return new Integer(0);
		}else if (id.equals(LINENOTE_PROP)) {
			return new ModelAndText(this, this.lineNote);
		}
		return super.getPropertyValue(id);
	}

	/**
	 * Sets the lineStyle based on the String provided by the PropertySheet
	 * 
	 * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object,
	 *      java.lang.Object)
	 */
	public void setPropertyValue(Object id, Object value) {
		if (id.equals(LINESTYLE_PROP)){
			setLineStyle(new Integer(1).equals(value) ? Graphics.LINE_DOT
					: Graphics.LINE_SOLID);
		}else if (id.equals(LINENOTE_PROP)){
			if (value instanceof String){
				String comment = (String) value;
				setLineNote(comment);
			}else if (value instanceof ModelAndText){
				ModelAndText t = (ModelAndText) value;
				setLineNote(t.text);
			}
		} else {
			super.setPropertyValue(id, value);
		}
	}
	
	/**
	 * Returns the source endpoint of this connection.
	 * 
	 * @return a non-null Shape instance
	 */
	public Shape getSource() {
		return source;
	}

	/**
	 * Returns the target endpoint of this connection.
	 * 
	 * @return a non-null Shape instance
	 */
	public Shape getTarget() {
		return target;
	}

	/**
	 * Reconnect this connection. The connection will reconnect with the shapes
	 * it was previously attached to.
	 */
	public void reconnect() {
		if (!isConnected) {
			source.addConnection(this);
			target.addConnection(this);
			isConnected = true;
		}
	}

	/**
	 * Reconnect to a different source and/or target shape. The connection will
	 * disconnect from its current attachments and reconnect to the new source
	 * and target.
	 * 
	 * @param newSource
	 *            a new source endpoint for this connection (non null)
	 * @param newTarget
	 *            a new target endpoint for this connection (non null)
	 * @throws IllegalArgumentException
	 *             if any of the paramers are null or newSource == newTarget
	 */
	public void reconnect(Shape newSource, Shape newTarget) {
		if (newSource == null || newTarget == null || newSource == newTarget) {
			throw new IllegalArgumentException();
		}
		disconnect();
		this.source = newSource;
		this.target = newTarget;
		reconnect();
	}

	/** Line drawing style for this connection. */
	private int lineStyle = Graphics.LINE_SOLID;
	private String lineNote = "";
	
	/**
	 * Returns the line drawing style of this connection.
	 * 
	 * @return an int value (Graphics.LINE_DASH or Graphics.LINE_SOLID)
	 */
	public int getLineStyle() {
		return lineStyle;
	}
	
	/**
	 * Set the line drawing style of this connection.
	 * 
	 * @param lineStyle
	 *            one of following values: Graphics.LINE_DOT or
	 *            Graphics.LINE_SOLID
	 * @see Graphics#LINE_DOT
	 * @see Graphics#LINE_SOLID
	 * @throws IllegalArgumentException
	 *             if lineStyle does not have one of the above values
	 */
	public void setLineStyle(int lineStyle) {
		if (lineStyle != Graphics.LINE_DOT && lineStyle != Graphics.LINE_SOLID) {
			throw new IllegalArgumentException();
		}
		this.lineStyle = lineStyle;
		firePropertyChange(LINESTYLE_PROP, null, new Integer(this.lineStyle));
	}

	public String getLineNote(){
		return this.lineNote;
	}
	
	public void setLineNote(String note){
		if (note!=null && this.lineNote!=null && !this.lineNote.equals(note)){
			this.lineNote=note;
			firePropertyChange(LINENOTE_PROP, null, this.lineNote);
		}
	}

	public ShapesDiagram getDiagram() {
		if (getSource()!=null)
			return getSource().diagram;
		if (getTarget()!=null)
			return getTarget().diagram;
		return null;
	}
}