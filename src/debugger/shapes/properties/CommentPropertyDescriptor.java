package debugger.shapes.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColorCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import debugger.shapes.ShapesEditor;

public class CommentPropertyDescriptor extends PropertyDescriptor {	
    public CommentPropertyDescriptor(Object id, String displayName) {
        super(id, displayName);
    }

    public CellEditor createPropertyEditor(Composite parent) {
        CellEditor editor = new CommentCellEditor(parent);
        if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
        return editor;
    }
}
