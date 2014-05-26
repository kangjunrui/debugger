/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *????Elias Volanakis - initial API and implementation
 *******************************************************************************/
package org.eclipse.gef.examples.shapes.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.EllipseAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.ViewportAutoexposeHelper;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;

import org.eclipse.gef.examples.shapes.ShapesEditor;
import org.eclipse.gef.examples.shapes.model.Connection;
import org.eclipse.gef.examples.shapes.model.EllipticalShape;
import org.eclipse.gef.examples.shapes.model.ModelElement;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.commands.ConnectionCreateCommand;
import org.eclipse.gef.examples.shapes.model.commands.ConnectionReconnectCommand;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * EditPart used for Shape instances (more specific for EllipticalShape and
 * RectangularShape instances).
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 * 
 * @author Elias Volanakis
 */
public class ShapeEditPart extends AbstractGraphicalEditPart implements
		PropertyChangeListener, NodeEditPart{

	private ConnectionAnchor anchor;
	
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

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
	 */
	protected void createEditPolicies() {
		// allow removal of the associated model element
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new ShapeComponentEditPolicy());
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new DiagramDirectEditPolicy());
		// allow the creation of connections and
		// and the reconnection of connections between Shape instances
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
				new GraphicalNodeEditPolicy() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see
					 * org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
					 * getConnectionCompleteCommand
					 * (org.eclipse.gef.requests.CreateConnectionRequest)
					 */
					protected Command getConnectionCompleteCommand(
							CreateConnectionRequest request) {
						ConnectionCreateCommand cmd = (ConnectionCreateCommand) request
								.getStartCommand();
						cmd.setTarget((Shape) getHost().getModel());
						return cmd;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see
					 * org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
					 * getConnectionCreateCommand
					 * (org.eclipse.gef.requests.CreateConnectionRequest)
					 */
					protected Command getConnectionCreateCommand(
							CreateConnectionRequest request) {
						Shape source = (Shape) getHost().getModel();
						int style = ((Integer) request.getNewObjectType())
								.intValue();
						ConnectionCreateCommand cmd = new ConnectionCreateCommand(
								source, style);
						request.setStartCommand(cmd);
						return cmd;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see
					 * org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
					 * getReconnectSourceCommand
					 * (org.eclipse.gef.requests.ReconnectRequest)
					 */
					protected Command getReconnectSourceCommand(
							ReconnectRequest request) {
						Connection conn = (Connection) request
								.getConnectionEditPart().getModel();
						Shape newSource = (Shape) getHost().getModel();
						ConnectionReconnectCommand cmd = new ConnectionReconnectCommand(
								conn);
						cmd.setNewSource(newSource);
						return cmd;
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see
					 * org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
					 * getReconnectTargetCommand
					 * (org.eclipse.gef.requests.ReconnectRequest)
					 */
					protected Command getReconnectTargetCommand(
							ReconnectRequest request) {
						Connection conn = (Connection) request
								.getConnectionEditPart().getModel();
						Shape newTarget = (Shape) getHost().getModel();
						ConnectionReconnectCommand cmd = new ConnectionReconnectCommand(
								conn);
						cmd.setNewTarget(newTarget);
						return cmd;
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
	 */
	protected IFigure createFigure() {
		IFigure f = createFigureForModel();
		f.setOpaque(true); // non-transparent figure
		f.setBackgroundColor(getCastedModel().diagram.getColor(getCastedModel().getColor()));
		return f;
	}

	private static Font FONT_USER_NAME;
	static {
		Display current = Display.getCurrent();
		Font systemFont = current.getSystemFont();
		FontData[] systemFontData = systemFont.getFontData();
		String name = "Arial";
		int height = 9;
		if (systemFontData.length >= 1) {
			name = systemFontData[0].getName();
			height = systemFontData[0].getHeight();
		}

		FONT_USER_NAME = new Font(current, name, height, SWT.NORMAL);
	}
	
	Label m_name;
	org.eclipse.draw2d.Shape actualFigure=null;
	/**
	 * Return a IFigure depending on the instance of the current model element.
	 * This allows this EditPart to be used for both sublasses of Shape.
	 */
	private IFigure createFigureForModel() {
		if (getModel() instanceof EllipticalShape) {
			actualFigure = new Ellipse();
		} else if (getModel() instanceof RectangularShape) {
			actualFigure = new RectangleFigure();
		} else {
			// if Shapes gets extended the conditions above must be updated
			throw new IllegalArgumentException();
		}
		actualFigure.setOutline(false);
		
		m_name = new Label(){
			@Override
			public Dimension getPreferredSize(int wHint, int hHint) {
				if (prefSize == null) {
					prefSize = calculateLabelSize(getTextSize());
				}
				return prefSize;
			}
		};

		//m_name.setText(getCastedModel().getName());
		m_name.setFont(FONT_USER_NAME);
		setShowfilename(getShowfilename());
		m_name.setForegroundColor(ColorConstants.black);
		m_name.setOpaque(false);
		m_name.setVisible(true);
		
		actualFigure.setLayoutManager(new FlowLayout());
		actualFigure.add(m_name);
		actualFigure.setOpaque(true); 
		
		Rectangle bounds = new Rectangle(getCastedModel().getLocation(),
				getLabelSize());
		actualFigure.setBounds(bounds);
		
		return actualFigure;
	}

	public boolean getShowfilename(){
		return getCastedModel().showfilename;
	}
	
	public boolean setShowfilename(boolean state){
		boolean oldstate=getCastedModel().showfilename;
		if (state && getCastedModel().editor==-1)
			return oldstate;
		if (state){
			String name=getCastedModel().diagram.getFile(getCastedModel().editor);
			int index1=name.lastIndexOf(".");
			int index2=name.lastIndexOf("/", index1-1);
			m_name.setText(name.substring(index2+1, index1)+"."+getCastedModel().getName());
		}else{
			m_name.setText(getCastedModel().getName());
		}
		getCastedModel().showfilename=state;
		Rectangle bounds = new Rectangle(getCastedModel().getLocation(),
				getLabelSize());
		actualFigure.setBounds(bounds);
		return state;
	}
	
	public Dimension getLabelSize(){
		if (m_name!=null)
			return m_name.getPreferredSize(-1, -1).expand(1, 1);
		else
			return new Dimension(40,20);
	}
	
	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			((ModelElement) getModel()).removePropertyChangeListener(this);
			m_name=null;
		}
	}

	public Shape getCastedModel() {
		return (Shape) getModel();
	}

	protected ConnectionAnchor getConnectionAnchor() {
		if (anchor == null) {
			if (getModel() instanceof EllipticalShape)
				anchor = new EllipseAnchor(getFigure());
			else if (getModel() instanceof RectangularShape)
				anchor = new ChopboxAnchor(getFigure());
			else
				// if Shapes gets extended the conditions above must be updated
				throw new IllegalArgumentException("unexpected model");
		}
		return anchor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelSourceConnections
	 * ()
	 */
	protected List getModelSourceConnections() {
		return getCastedModel().getSourceConnections();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelTargetConnections
	 * ()
	 */
	protected List getModelTargetConnections() {
		return getCastedModel().getTargetConnections();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef
	 * .ConnectionEditPart)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef
	 * .Request)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef
	 * .ConnectionEditPart)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef
	 * .Request)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (Shape.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		} else if (Shape.SOURCE_CONNECTIONS_PROP.equals(prop)) {
			refreshSourceConnections();
		} else if (Shape.TARGET_CONNECTIONS_PROP.equals(prop)) {
			refreshTargetConnections();
		} else if (Shape.NAME_PROP.equals(prop)) {
			if (m_name!=null){
				//m_name.setText((String)evt.getNewValue());
				setShowfilename(getShowfilename());
				getFigure().setSize(m_name.getSize());
				refreshVisuals();
			}
		} else if (Shape.COLOR_PROP.equals(prop)) {
			//actualFigure
			Color oldcolor=actualFigure.getBackgroundColor();
			actualFigure.setBackgroundColor(getCastedModel().diagram.getColor(getCastedModel().getColor()));
			getCastedModel().diagram.ungetColor(oldcolor);
		}
	}

	protected void refreshVisuals() {
		// notify parent container of changed position & location
		// if this line is removed, the XYLayoutManager used by the parent
		// container
		// (the Figure of the ShapesDiagramEditPart), will not know the bounds
		// of this figure
		// and will not draw it correctly.
		Rectangle bounds = new Rectangle(getCastedModel().getLocation(),getLabelSize());
		//((GraphicalEditPart) getParent()).setLayoutConstraint(this,	getFigure(), bounds);
		getFigure().setBounds(bounds);
	}
	
	@Override
	public DragTracker getDragTracker(Request request) {
		return new ShapeEditPartsDragTracker(this);
	}

	private DirectEditManager manager;
	public void performRequest(Request request) {
		if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
			if (manager == null) {
				manager = new DirectEditManager(this, TextCellEditor.class, new CellEditorLocator(){
					@Override
					public void relocate(CellEditor celleditor) {
						Text text = (Text) celleditor.getControl();
						int scrollWidth = 0;//东西偏移量
						int scrollHeight = 0;//南北偏移量

						FigureCanvas canvas = (FigureCanvas) text.getParent(); // 得到滚动区域的画布
						scrollWidth = canvas.getViewport().getHorizontalRangeModel().getValue();
						scrollHeight = canvas.getViewport().getVerticalRangeModel().getValue();

						org.eclipse.swt.graphics.Point pref = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
						Rectangle rect = ShapeEditPart.this.m_name.getTextBounds();	//得到覆盖的文本label
						text.setBounds(rect.x - 1 - scrollWidth, rect.y - 1 - scrollHeight,
								pref.x + 1, pref.y + 1);
					}
				}){
					protected CellEditor createCellEditorOn(Composite composite) {
				           return new TextCellEditor (composite, SWT.MULTI);
				    }
					@Override
					protected void initCellEditor() { 
						getCellEditor().setValue(getCastedModel().getName());
					}
				};
			}
			manager.show();
		}else if (request.getType() == RequestConstants.REQ_OPEN) {
			Shape model = getCastedModel();
			if (model.editor!=-1){
				IEditorPart t=model.diagram.getEditor(model.editor);
				if (t!=null && t instanceof ITextEditor){
					t.getEditorSite().getPage().bringToTop(t);
					ITextEditor editor = (ITextEditor)t;
					IDocumentProvider provider= editor.getDocumentProvider();
					IDocument document= provider.getDocument(editor.getEditorInput());
					try {

						int start= document.getLineOffset(getCastedModel().line);
						editor.selectAndReveal(start, 0);
					} catch (BadLocationException x) {
						x.printStackTrace();
					}
				}
			}
		}
	}
}