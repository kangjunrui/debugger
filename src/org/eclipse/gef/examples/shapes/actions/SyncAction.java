package org.eclipse.gef.examples.shapes.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.examples.shapes.model.Connection;
import org.eclipse.gef.examples.shapes.model.EllipticalShape;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.ShapesDiagram;
import org.eclipse.gef.examples.shapes.model.commands.ShapePasteCommand;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.PaletteTemplateEntry;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.Clipboard;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.gef.examples.shapes.ShapesEditor;

public class SyncAction extends SelectionAction implements ISelectionChangedListener {
	public static final String ID = "debugger.actions.sync";
	private ShapesDiagram diagram;
	public static Point menuPosition=null;

	public SyncAction(IEditorPart editor, ShapesDiagram diagram) {
		super(editor, SWT.CHECK);
		this.diagram=diagram;
		setLazyEnablementCalculation(false);
	}

	protected boolean calculateEnabled() {
		return true;
	}

	protected void init() {
		super.init();
		setText("Sync");
		setToolTipText("Show selected label from package explorer.");
		setId(ID);
		ISharedImages sharedImages = PlatformUI.getWorkbench()
				.getSharedImages();
		setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
		setDisabledImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED_DISABLED));
	}

	boolean state=false;
	public void run() {
		if (state){
			setChecked(false);
			IViewPart explorer=((ShapesEditor)getWorkbenchPart()).getSite().getWorkbenchWindow().getActivePage().findView("org.eclipse.jdt.ui.PackageExplorer");
			if (explorer!=null){
				ISelectionProvider provider = explorer.getViewSite().getSelectionProvider();
				provider.removeSelectionChangedListener(this);
			}
		}else{
			setChecked(true);
			IViewPart explorer=((ShapesEditor)getWorkbenchPart()).getSite().getWorkbenchWindow().getActivePage().findView("org.eclipse.jdt.ui.PackageExplorer");
			if (explorer!=null){
				ISelectionProvider provider = explorer.getViewSite().getSelectionProvider();
				provider.addSelectionChangedListener(this);
			}
		}
		state=!state;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (!state) return;
		ISelectionProvider provider=event.getSelectionProvider();
		ISelection selection = provider.getSelection();
		if (selection instanceof TreeSelection){
			TreeSelection treesel=(TreeSelection)selection;
			//treesel.iterator()
			System.out.println(treesel.getFirstElement());
		}
	}
}
