package debugger.schedules;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import debugger.schedules.model.SchedulesItem;
import debugger.schedules.model.SchedulesRoot;
import debugger.shapes.model.ShapesDiagram;

public class SchedulesEditor extends EditorPart implements IDoubleClickListener {

	private SchedulesRoot root;

	public SchedulesEditor() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		try {
			IFile file = ((IFileEditorInput) input).getFile();
			ObjectInputStream in = new ObjectInputStream(file.getContents());
			root = (SchedulesRoot) in.readObject();
			in.close();
			setPartName(file.getName());
		} catch (IOException e) {
			handleLoadException(e);
		} catch (CoreException e) {
			handleLoadException(e);
		} catch (ClassNotFoundException e) {
			handleLoadException(e);
		}
	}


	private void handleLoadException(Exception e) {
		System.err.println("** Load failed. Using default model. **");
		e.printStackTrace();
		root = new SchedulesRoot();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}
	
	TreeViewer treeViewer;
	ToolBar toolbar;
	
	@Override
	public void doubleClick(DoubleClickEvent event) {
		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, true));

		toolbar = new ToolBar(parent, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		ToolItem item_back=new ToolItem(toolbar, SWT.PUSH);
		item_back.setText("Back");
		item_back.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("Back");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		ToolItem item_forward=new ToolItem(toolbar, SWT.PUSH);
		item_forward.setText("Back");
		item_forward.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("Foward");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
	    treeViewer = new TreeViewer(parent, SWT.BORDER);
	    treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));

	    treeViewer.addDoubleClickListener(this);
	    
	    // Sets the content provider.
	    treeViewer.setContentProvider(root.getContentProvider());
	    
	    // Sets the label provider. 
	    treeViewer.setLabelProvider(root.getLabelProvider());
	    
	    // Sorts the tree. 
	    treeViewer.setSorter(new ViewerSorter() {
	      public int category(Object element) {
	        SchedulesItem item = (SchedulesItem)element;
	        return item.getOrdinal();
	      }
	    });
	    
	    
	    treeViewer.setInput(root);
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}
}
