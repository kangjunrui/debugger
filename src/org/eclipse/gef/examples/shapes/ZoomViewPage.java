package org.eclipse.gef.examples.shapes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;


public class ZoomViewPage extends org.eclipse.ui.part.Page implements
		org.eclipse.ui.views.contentoutline.IContentOutlinePage {

	private EditPartViewer viewer;
	private Canvas canvas;

	/**
	 * Constructs a ContentOutlinePage for the given viewer.
	 * 
	 * @param viewer
	 *            the viewer
	 */
	public ZoomViewPage(EditPartViewer viewer) {
		this.viewer = viewer;
	}

	/**
	 * @see ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		getViewer().addSelectionChangedListener(listener);
	}

	ScrollableThumbnail thumbnail;
	DisposeListener disposeListener;
	/**
	 * Forwards the createControl request to the editpartviewer.
	 * 
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		canvas = new Canvas(parent, SWT.BORDER);
		LightweightSystem lws = new LightweightSystem(canvas);
		thumbnail = new ScrollableThumbnail(
				(Viewport) ((ScalableFreeformRootEditPart) getViewer()
						.getRootEditPart()).getFigure());
		thumbnail.setSource(((ScalableFreeformRootEditPart) getViewer()
				.getRootEditPart()).getLayer(LayerConstants.PRINTABLE_LAYERS));
		lws.setContents(thumbnail);
		disposeListener = new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (thumbnail != null) {
					thumbnail.deactivate();
					thumbnail = null;
				}
			}
		};
		getViewer().getControl().addDisposeListener(disposeListener);
	}

	public void dispose() { 
		//getSelectionSynchronizer().removeViewer(getViewer()); 
		if (getViewer().getControl() != null && !getViewer().getControl().isDisposed()) 
			getViewer().getControl().removeDisposeListener(disposeListener); 
		super.dispose(); 
	}
	
	/**
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return canvas;
	}

	/**
	 * Forwards selection request to the viewer.
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		// $TODO when could this even happen?
		if (getViewer() == null)
			return StructuredSelection.EMPTY;
		return getViewer().getSelection();
	}

	/**
	 * Returns the EditPartViewer
	 * 
	 * @return the viewer
	 */
	protected EditPartViewer getViewer() {
		return viewer;
	}

	/**
	 * @see ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		getViewer().removeSelectionChangedListener(listener);
	}

	/**
	 * Sets focus to a part in the page.
	 */
	public void setFocus() {
		if (getControl() != null)
			getControl().setFocus();
	}

	/**
	 * @see ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	public void setSelection(ISelection selection) {
		if (getViewer() != null)
			getViewer().setSelection(selection);
	}

}
