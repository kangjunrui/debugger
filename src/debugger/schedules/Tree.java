package debugger.schedules;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import debugger.schedules.model.SchedulesRoot;

public class Tree extends EditorPart {
	TreeViewer treeViewer;
	Button addButton;
	Button renameButton;
	
	//specified in EditorPart, but not yet implemented.
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof IFileEditorInput))
			throw new PartInitException(
					"Invalid Input: Must be IFileEditorInput");
		setSite(site);
		setInput(input);
	}
	
	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		SchedulesRoot root=null;
		treeViewer.setContentProvider(root.getContentProvider());
		treeViewer.setLabelProvider(root.getLabelProvider());
		treeViewer.setInput(root);
	}
	
	//open
	
	@Override
	public void createPartControl(Composite parent) {
		Button addButton=new Button(parent, SWT.PUSH);
		Button renameButton=new Button(parent, SWT.PUSH);
		treeViewer = new TreeViewer(parent);
		
		addButton.setText("add");
		addButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeElement el = new TreeElement("node3", ((TreeElement)treeViewer.getInput()));
				treeViewer.refresh(treeViewer.getInput());
				ISelection selection = new StructuredSelection(el);
				//treeViewer.setSelection(selection, false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		renameButton.setText("rename");
		renameButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeElement el = (TreeElement)((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
				el.name="new name";
				treeViewer.update(el, null);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		GridData layoutData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		renameButton.setLayoutData(layoutData);
		
		layoutData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		addButton.setLayoutData(layoutData);
		
		layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		layoutData.horizontalSpan = 3;
		treeViewer.getTree().setLayoutData(layoutData);
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	//Save 
	
	@Override
	public boolean isDirty() {
		//if model changed
		return false;
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		//save model into the same xml file
	}

	@Override
	public void doSaveAs() {
		//save model into a different xml file, first show a filename dialog.
	}

}
