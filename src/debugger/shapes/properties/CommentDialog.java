package debugger.shapes.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.*;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.*;

import debugger.shapes.ShapesEditor;
import debugger.shapes.model.Connection;
import debugger.shapes.model.ModelElement;
import debugger.shapes.model.Shape;
import debugger.shapes.model.commands.SetDialogTextCommand;
import debugger.shapes.parts.ShapeEditPart;

public class CommentDialog extends Dialog implements ISelectionChangedListener, PropertyChangeListener {
	Display display;
	int width, height;
	Text text;
	ShapesEditor editor;
	private CommandStack commandstack;
	ModelElement model;
	boolean isDirty;
	Shell shell;
	String name="";
	boolean changing=false;
	
	public CommentDialog(Shell parent, ShapesEditor editor, CommandStack commandstack) {
		super(parent, SWT.MODELESS);
		this.editor = editor;
		this.commandstack=commandstack;
	}

	private void setNameAndText(){
		if (shell==null) return;
		
		isDirty=false;
		if (model==null){
			text.setText("");
			if (text.getEnabled()==true)
				text.setEnabled(false);
			shell.setText("");
			return;
		}else{
			if (text.getEnabled()==false)
				text.setEnabled(true);
		}
		
		changing=true;
		if (model instanceof Shape){
			name=((Shape)model).getName()+"";
			shell.setText(name);
			text.setText(((Shape)model).getComment()+"");
		}else if (model instanceof Connection){
			Connection conn=(Connection)model;
			name=conn.getSource().getName() + "->" + conn.getTarget().getName();
			shell.setText(name);
			text.setText(((Connection)model).getLineNote()+"");
		}
		changing=false;
	}
	
	public void open() {
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MODELESS
				| SWT.TOP);
		shell.setSize(400,400);
		shell.setLayout(new FillLayout());
		shell.addShellListener(new ShellListener(){
			
			//focus listener
			
			@Override
			public void shellActivated(ShellEvent e) {}

			@Override
			public void shellDeactivated(ShellEvent e) {}
			
			//when not closed still receive input
			
			@Override
			public void shellDeiconified(ShellEvent e) {}

			@Override
			public void shellIconified(ShellEvent e) {}
			
			@Override
			public void shellClosed(ShellEvent e) {
				CommentDialog.this.editor.getGraphicalViewer().removeSelectionChangedListener(CommentDialog.this);
				setInput(null);
				shell=null;
			}			
		});
		
		text = new Text(shell, SWT.MULTI);
		text.addModifyListener(new ModifyListener(){

			@Override
			public void modifyText(ModifyEvent e) {
				if (changing)
					return;
				isDirty=true;
				shell.setText(name+"*");
			}
			
		});
		setNameAndText();
		
		this.editor.getGraphicalViewer().addSelectionChangedListener(this);
		shell.open();
	}

	public void setInput(ModelElement model) {
		if (isDirty){
			SetDialogTextCommand cmd = new SetDialogTextCommand(this.model, text.getText());
			commandstack.execute(cmd);
		}
		if (this.model!=null)
			this.model.removePropertyChangeListener(this);
		this.model = model;
		if (this.model!=null)
			this.model.addPropertyChangeListener(this);
		setNameAndText();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection){
			IStructuredSelection sel2 = (IStructuredSelection)sel;
			if (sel2.size()==1){
				Object e = sel2.getFirstElement();
				if (e instanceof ConnectionEditPart || e instanceof ShapeEditPart){
					setInput((ModelElement)(((org.eclipse.gef.GraphicalEditPart)e).getModel()));
					return;
				}
			}
		}
		setInput(null);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (model instanceof Shape && evt.getPropertyName().equals(Shape.COMMENT_PROP)){
			setNameAndText();
		}else if (model instanceof Connection && evt.getPropertyName().equals(Connection.LINENOTE_PROP)){
			setNameAndText();
		}
	}
}