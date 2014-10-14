package debugger.shapes.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
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

import debugger.shapes.ShapesEditor;
import debugger.shapes.model.Connection;
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;
import debugger.shapes.model.commands.ShapePasteCommand;

/**
 * An action to delete selected objects.
 */
public class PasteAction extends SelectionAction {

	/** @deprecated Use ActionFactory.DELETE.getId() instead. */
	public static final String ID = ActionFactory.PASTE.getId();
	private ShapesDiagram diagram;
	public static Point menuPosition=null;

	public PasteAction(IEditorPart editor, ShapesDiagram diagram) {
		super(editor);
		this.diagram=diagram;
		setLazyEnablementCalculation(false);
	}

	/**
	 * Returns <code>true</code> if the selected objects can be deleted. Returns
	 * <code>false</code> if there are no objects selected or the selected
	 * objects are not {@link EditPart}s.
	 * 
	 * @return <code>true</code> if the command should be enabled
	 */
	protected boolean calculateEnabled() {
//		Clipboard clipboard = new Clipboard(display);
//		TransferData[] transferDatas = clipboard.getAvailableTypes();
//		for(int i=0; i<transferDatas.length; i++) {
//		  // Checks whether RTF format is available.
//		  if(RTFTransfer.getInstance().isSupportedType(transferDatas[i])) {
//		    String rtfText = (String)clipboard.getContents(RTFTransfer.getInstance());
//			System.out.println("RTF: " + rtfText);
//			break;
//		  }
//		}
		
		return CopyAction.GlobalData!=null && CopyAction.GlobalData.shapes.size()!=0;
	}

	/**
	 * Create a command to remove the selected objects.
	 * 
	 * @param objects
	 *            The objects to be deleted.
	 * @return The command to remove the selected objects.
	 */
	public Command createPasteCommand(ClipboardDiagram diagram) {
		if (diagram == null)
			return null;
		if (diagram.shapes.size()==0)
			return null;

		//create shapes for diagram
		Shape[] newShapes;
		Map<ClipboardShape, Shape> shapemap=new HashMap<ClipboardShape, Shape>();
		newShapes=new Shape[diagram.shapes.size()];
		Point location=new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
		for (ClipboardShape shape: diagram.shapes){
			Shape shapemodel = new Shape();
			shapemodel.setType(shape.type);
			shapemodel.setComment(shape.comment);
			shapemodel.setName(shape.name);
			if (shape.location.x<location.x || shape.location.y<location.y){
				location.setLocation(shape.location);
			}
			shapemodel.resource=-1;
			if (shape.file!=null){
				shapemodel.filename=shape.file;
				shapemodel.line=shape.line;
				
				shapemodel.offset=shape.offset;
				shapemodel.length=shape.length;
				shapemodel.signature=shape.signature;
			}
			shapemodel.setIcon(shape.icon);
			shapemodel.setLocation(shape.location.getCopy());
			shapemodel.showfilename=shape.showfilename;
			shapemodel.setColor(shape.color);
			shapemap.put(shape, shapemodel);
		}
		newShapes=shapemap.values().toArray(new Shape[0]);
		if (menuPosition!=null){
			location=location.getNegated();
			location=location.getTranslated(menuPosition);
		}else{
			CopyAction.GlobalLocation = CopyAction.GlobalLocation.getTranslated(-10, -10);
			location=CopyAction.GlobalLocation;
		}
		for (Shape shapemodel : newShapes){
			shapemodel.setLocation(shapemodel.getLocation().getTranslated(location));
		}
		menuPosition=null;
		
		for (ClipboardConnection connection: diagram.connections){
			Connection connectionmodel = new Connection(shapemap.get(connection.source), shapemap.get(connection.target));
			connectionmodel.setLineStyle(connection.lineStyle);
			connectionmodel.setLineNote(connection.note);
		}
		
		ShapePasteCommand cmd = new ShapePasteCommand(newShapes, this.diagram);
		
		return cmd;
	}

	/**
	 * Initializes this action's text and images.
	 */
	protected void init() {
		super.init();
		setText(GEFMessages.PasteAction_Label);
		setToolTipText(GEFMessages.PasteAction_Tooltip);
		setId(ActionFactory.PASTE.getId());
		setEnabled(false);
		ISharedImages sharedImages = PlatformUI.getWorkbench()
				.getSharedImages();
		setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setDisabledImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
	}

	/**
	 * Performs the delete action on the selected objects.
	 */
	public void run() {
		if (CopyAction.GlobalData==null) return;
		Command cmd=null;
		synchronized(CopyAction.GlobalData){
			cmd = createPasteCommand((ClipboardDiagram)CopyAction.GlobalData);
		}
		if (cmd != null)
			execute(cmd);
	}
}
