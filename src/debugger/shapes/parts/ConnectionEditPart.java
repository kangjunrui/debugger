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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartListener;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import org.eclipse.swt.graphics.Color;

import debugger.shapes.model.Connection;
import debugger.shapes.model.ModelElement;
import debugger.shapes.model.commands.ConnectionDeleteCommand;

/**
 * Edit part for Connection model elements.
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 * 
 * @author Elias Volanakis
 */
class ConnectionEditPart extends AbstractConnectionEditPart implements
		PropertyChangeListener {
	static EditPartListener selectionListener = new EditPartListener() {
		
		@Override
		public void selectedStateChanged(EditPart editpart) {
			if (editpart.getSelected() != SELECTED_NONE){
				((PolylineConnection)((GraphicalEditPart)editpart).getFigure()).setForegroundColor(ColorConstants.blue);
			}else{
				((PolylineConnection)((GraphicalEditPart)editpart).getFigure()).setForegroundColor(ColorConstants.black);
			}
		}
		
		@Override
		public void removingChild(EditPart child, int index) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void partDeactivated(EditPart editpart) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void partActivated(EditPart editpart) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void childAdded(EditPart child, int index) {
			// TODO Auto-generated method stub
			
		}
	};
	
	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	public void activate() {
		if (!isActive()) {
			super.activate();
			((ModelElement) getModel()).addPropertyChangeListener(this);
			addEditPartListener(selectionListener);
		}		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
	 */
	protected void createEditPolicies() {
		// Selection handle edit policy.
		// Makes the connection show a feedback, when selected by the user.
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		// Allows the removal of the connection model element
		installEditPolicy(EditPolicy.CONNECTION_ROLE,
				new ConnectionEditPolicy() {
					protected Command getDeleteCommand(GroupRequest request) {
						return new ConnectionDeleteCommand(getCastedModel());
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
	 */
	protected IFigure createFigure() {
		PolylineConnection connection = (PolylineConnection) super
				.createFigure();
		connection.setTargetDecoration(new PolygonDecoration()); // arrow at
																	// target
																	// endpoint
		connection.setLineStyle(getCastedModel().getLineStyle()); // line
																	// drawing
																	// style
		return connection;
	}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			((ModelElement) getModel()).removePropertyChangeListener(this);
			removeEditPartListener(selectionListener);
		}
	}

	private Connection getCastedModel() {
		return (Connection) getModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getPropertyName();
		if (Connection.LINESTYLE_PROP.equals(property)) {
			((PolylineConnection) getFigure()).setLineStyle(getCastedModel()
					.getLineStyle());
		}
	}

}