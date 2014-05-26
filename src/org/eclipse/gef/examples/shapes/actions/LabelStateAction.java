package org.eclipse.gef.examples.shapes.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.examples.shapes.ShapesEditor;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.PaletteTemplateEntry;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.Clipboard;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.gef.examples.shapes.model.*;
import org.eclipse.gef.examples.shapes.model.commands.ShapeSetConstraintCommand;
import org.eclipse.gef.examples.shapes.parts.*;

/**
 * An action to delete selected objects.
 */
public class LabelStateAction extends SelectionAction {
	public static final String ID = "debugger.actions.labelstate";
	ShapesEditor editor;
	
	public LabelStateAction(ShapesEditor editor) {
		super((IWorkbenchPart) editor, SelectionAction.AS_CHECK_BOX);
		this.editor = editor;
		setLazyEnablementCalculation(false);
	}

	
	List<ShapeEditPart> shapes;
	boolean state;
	protected boolean calculateEnabled() {
		List editparts=getSelectedObjects();
		shapes=new ArrayList<ShapeEditPart>();
		
		state=true;
		boolean state_needset=true;
		for (Object o : editparts){
			if (!(o instanceof ShapeEditPart)) continue;
			ShapeEditPart part=(ShapeEditPart)o;
			if (o instanceof ShapeEditPart){
				shapes.add(part);
			}
			if (state_needset){
				if (!part.getCastedModel().showfilename){
					state_needset=false;
					state=false;
				}
			}
		}
		
		//enabled value:
		if (shapes.size()==0){
			return false;
		}else{
			setChecked(state);
			return true;
		}
	}


	/**
	 * Initializes this action's text and images.
	 */
	protected void init() {
		super.init();
		setText("Show Filename");
		setId(ID);
		setEnabled(false); //after selected, setEnabled(true)
		ISharedImages sharedImages = PlatformUI.getWorkbench()
				.getSharedImages();
		setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
	}

	/**
	 * Performs the delete action on the selected objects.
	 */
	public void run() {
		boolean newstate=!state;
		int numchanged=0;
		for (ShapeEditPart shape:shapes){
			if (newstate==shape.setShowfilename(newstate))
				numchanged++;
		}
		if (numchanged == shapes.size()){
			editor.setDirty(true);
			state=!state;
		}else{
			if (shapes.size()==1 || numchanged==0){
				;
			}else{
				editor.setDirty(true);
			}
		}
	}
}
