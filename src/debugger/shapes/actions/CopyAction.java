package debugger.shapes.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.PaletteTemplateEntry;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import debugger.shapes.ShapesEditor;
import debugger.shapes.model.*;
import debugger.shapes.model.commands.ShapeSetConstraintCommand;
import debugger.shapes.parts.*;


/**
 * An action to delete selected objects.
 */
public class CopyAction extends SelectionAction {

	/** @deprecated Use ActionFactory.DELETE.getId() instead. */
	public static final String ID = ActionFactory.COPY.getId();
	ShapesEditor editor;
	
	static Clipboard clipboard = new Clipboard(Display.getCurrent());
	static String lineSeperator = System.lineSeparator();
	
	public CopyAction(ShapesEditor editor) {
		super((IWorkbenchPart) editor);
		this.editor = editor;
		setLazyEnablementCalculation(false);
	}

	
	List<Shape> shapes;
	protected boolean calculateEnabled() {
		List editparts=getSelectedObjects();
		shapes=new ArrayList<Shape>();
		
		for (Object o : editparts){
			if (o instanceof ShapeEditPart){
				shapes.add((Shape)((EditPart) o).getModel());
			}
		}
		if (shapes.size()==0)
			return false;
		return true;
	}


	/**
	 * Initializes this action's text and images.
	 */
	protected void init() {
		super.init();
		setText(GEFMessages.CopyAction_Label);
		setToolTipText(GEFMessages.CopyAction_Tooltip);
		setId(ActionFactory.COPY.getId());
		setEnabled(false);
		ISharedImages sharedImages = PlatformUI.getWorkbench()
				.getSharedImages();
		setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
	}

	public static ClipboardDiagram GlobalData;
	public static Point GlobalLocation;
	
	/**
	 * Performs the delete action on the selected objects.
	 */
	public void run() {
		if (GlobalData==null) GlobalData=new ClipboardDiagram();
		synchronized(GlobalData){
			StringBuilder data = new StringBuilder();

			ClipboardDiagram diagram=new ClipboardDiagram();
			diagram.shapes=new ArrayList<ClipboardShape>();
			diagram.connections=new ArrayList<ClipboardConnection>();
			ClipboardShape shape;
			Map<Shape, ClipboardShape> shapemap = new HashMap<Shape, ClipboardShape>();
			for (Shape shapemodel: shapes){
				shape=new ClipboardShape();
				shape.location=shapemodel.getLocation();
				shape.name=shapemodel.getName();
				shape.type=shapemodel.getType();
				shape.comment=shapemodel.getComment();
				shape.icon=shapemodel.getIcon();
				if (shapemodel.resource!=-1){
					shape.line=shapemodel.line;
					shape.file=shapemodel.diagram.getFile(shapemodel.resource);
					
					shape.offset=shapemodel.offset;
					shape.length=shapemodel.length;
					shape.signature=shapemodel.signature;
				}
				shape.showfilename=shapemodel.showfilename;
				shape.color=shapemodel.getColor();
				diagram.shapes.add(shape);
				shapemap.put(shapemodel, shape);
				
				if (shapemodel.resource!=-1 && shape.showfilename){
					String name=shape.file;
					int index1=name.lastIndexOf(".");
					int index2=name.lastIndexOf("/", index1-1);
					data.append(name.substring(index2+1, index1)+"."+shape.name);
				}else{
					data.append(shape.name);
				}
				data.append(lineSeperator);
			}
			for (Shape shapemodel: shapes){
				List<Connection> conns=shapemodel.getSourceConnections();
				for (Connection conn: conns){
					if (shapemap.containsKey(conn.getTarget())){
						ClipboardConnection connection = new ClipboardConnection();
						connection.lineStyle = conn.getLineStyle();
						connection.source = shapemap.get(conn.getSource());
						connection.target = shapemap.get(conn.getTarget());
						connection.note=conn.getLineNote();
						diagram.connections.add(connection);
					}
				}
			}
			GlobalData = diagram;
			GlobalLocation = new Point(0, 0);
			editor.updatePasteAction();
			if (data.length() > 0) {
				clipboard.setContents(new Object[] { data.toString() },
						new Transfer[] { TextTransfer.getInstance() });
			}
		}
//		Clipboard clipboard = new Clipboard(display);
//		String plainText = "Hello World";
//		String rtfText = "{\\rtf1\\b Hello World}";
//		ShapeTransfer textTransfer = ShapeTransfer.getInstance();
//		RTFTransfer rftTransfer = RTFTransfer.getInstance();
//		clipboard.setContents(new String[]{plainText, rtfText}, new Transfer[]{textTransfer, rftTransfer});
//		clipboard.dispose();
//		Clipboard.getDefault().setContents(diagram);
		
		
	}
}
