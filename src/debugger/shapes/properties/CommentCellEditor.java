package debugger.shapes.properties;

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import debugger.shapes.model.Connection;
import debugger.shapes.model.ModelAndText;
import debugger.shapes.model.ModelElement;
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;

public class CommentCellEditor extends DialogCellEditor {
	ModelElement model;
	
	public CommentCellEditor(Composite parent) {
        super(parent, SWT.NONE);
    }

    public void dispose() {
        super.dispose();
    }

    protected Object openDialogBox(Control cellEditorWindow) {
    	Object value = getValue();
    	if (model!=null){
    		ShapesDiagram diagram=null;
    		if (model instanceof Shape){
    			diagram=((Shape)model).diagram;
    		}else if (model instanceof Connection){
    			diagram=((Connection)model).getDiagram();
    		}
    		if (diagram==null)
    			return value;
	    	CommentDialog dialog = diagram.getEditor().getCommentDialog();
			dialog.setInput(model);
	        dialog.open();
    	}
        return value;
    }

    protected void updateContents(Object value) {
    	if (value instanceof ModelAndText){
    		ModelAndText t = (ModelAndText)value;
	        model=t.model;
	    	super.updateContents(t.text);
    	}
    }
}
