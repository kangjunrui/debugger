package org.eclipse.gef.examples.shapes;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.views.ViewsPlugin;
import org.eclipse.ui.part.IContributedContentsView;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

public class ZoomView extends PageBookView implements ISelectionProvider,
		ISelectionChangedListener {

	public static final String ID = "shapes.zoomview";
	
	public ZoomView() {
		super();
	}

	/*
	 * (non-Javadoc) Method declared on ISelectionProvider.
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		getSelectionProvider().addSelectionChangedListener(listener);
	}

	private boolean shellMode=false;
	private HashMap<IPage, Shell> shellMap;
	
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		initPage(page);
		page.createControl(book);
		page.setMessage("No shapes editor.");
		return page;
	}
	
	
	private MenuDetectListener contextmenuListener = new MenuDetectListener() {
		
		@Override
		public void menuDetected(MenuDetectEvent e) {
			Menu menu = new Menu(getSite().getShell());
			MenuItem viewmodeItem = new MenuItem(menu, SWT.NONE);
			viewmodeItem.setText("view");
			viewmodeItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!shellMode)
						return;
					
					IWorkbenchPart part = getCurrentContributingPart();
					partHidden(part);
					shellMode=false;
					Collection<Shell> shells = shellMap.values();
					for (Shell shell: shells){
						shell.dispose();
					}
					shellMap=null;
					partVisible(part);
					((MessagePage)getDefaultPage()).setMessage("No shapes editor.");
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) { }
			});
			MenuItem shellmodeItem = new MenuItem(menu, SWT.NONE);
			shellmodeItem.setText("shell");
			shellmodeItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (shellMode)
						return;
					((MessagePage)getDefaultPage()).setMessage("Shell mode.");
					IWorkbenchPart part = getCurrentContributingPart();
					partHidden(part);
					shellMode=true;
					shellMap=new HashMap<IPage, Shell>();
					partVisible(part);
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { }
			});
			menu.setLocation(e.x, e.y);
			menu.setVisible(true);
		}
	};
	private MouseListener mouseListener=new MouseListener() {
		@Override
		public void mouseUp(MouseEvent e) { }
		
		@Override
		public void mouseDown(MouseEvent e) {
			ShapesEditor editor = (ShapesEditor)((Canvas)e.widget).getData("editor");
			getSite().getPage().bringToTop(editor);
		}
		
		@Override
		public void mouseDoubleClick(MouseEvent e) { }
	}; 
	
	@Override
	protected void showPageRec(PageRec pageRec) {
		if (shellMode){
			IPage page = pageRec.page;
			final Control pageControl = page.getControl();
			if (pageControl != null && !pageControl.isDisposed()) {
				Shell shell = shellMap.get(page);
				PageBook book;
				if (shell==null || shell.isDisposed()){
					shell = new Shell(getPageBook().getDisplay(), SWT.ON_TOP | SWT.CLOSE | SWT.TITLE | SWT.MIN | SWT.RESIZE);
					shell.setLayout(new FillLayout());
					book=new PageBook(shell, SWT.NONE);
					shellMap.put(page, shell);
					shell.addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(DisposeEvent e) {
							pageControl.setParent(getPageBook());
						}
					});
				}else{
					book=(PageBook)shell.getChildren()[0];
				}
				pageControl.setParent(book);
				book.showPage(pageControl);
				shell.setSize(300,400);
				shell.open();
			}
		}else{
			IPage page = pageRec.page;
			Control pageControl = page.getControl();
			if (pageControl != null && !pageControl.isDisposed() && pageControl.getParent() != getPageBook()){
				pageControl.setParent(getPageBook());
			}
			super.showPageRec(pageRec);
		}
	}

	protected void doDestroyPage(IWorkbenchPart part, PageRec rec) {
		ZoomViewPage page = (ZoomViewPage) rec.page;
		
		if (shellMap!=null){
			Shell shell = shellMap.get(page);
			if (shell!=null) 
				shell.dispose();
		}
		
		page.dispose();
		rec.dispose();
	}
	
	protected PageRec doCreatePage(IWorkbenchPart part) {	
		Object obj = ViewsPlugin.getAdapter(part, ZoomViewPage.class,
				false);
		if (obj instanceof ZoomViewPage) {
			ZoomViewPage page = (ZoomViewPage) obj;
			if (page instanceof IPageBookViewPage) {
				initPage((IPageBookViewPage) page);
			}
			page.createControl(getPageBook());
			page.getControl().addMenuDetectListener(contextmenuListener);
			((Canvas)page.getControl()).setData("editor", part);
			page.getControl().addMouseListener(mouseListener);
			return new PageRec(part, page);
		}else{
			return null;
		}
	}

	/*
	 * (non-Javadoc) Method declared on IAdaptable.
	 */
	public Object getAdapter(Class key) {
		if (key == IContributedContentsView.class) {
			return new IContributedContentsView() {
				public IWorkbenchPart getContributingPart() {
					return getContributingEditor();
				}
			};
		}
		return super.getAdapter(key);
	}

	/*
	 * (non-Javadoc) Method declared on PageBookView.
	 */
	protected IWorkbenchPart getBootstrapPart() {
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			return page.getActiveEditor();
		}

		return null;
	}

	/**
	 * Returns the editor which contributed the current page to this view.
	 * 
	 * @return the editor which contributed the current page or
	 *         <code>null</code> if no editor contributed the current page
	 */
	private IWorkbenchPart getContributingEditor() {
		return getCurrentContributingPart();
	}

	/*
	 * (non-Javadoc) Method declared on ISelectionProvider.
	 */
	public ISelection getSelection() {
		// get the selection from the selection provider
		return getSelectionProvider().getSelection();
	}

	/*
	 * (non-Javadoc) Method declared on PageBookView. We only want to track
	 * editors.
	 */
	protected boolean isImportant(IWorkbenchPart part) {
		// We only care about editors
		return (part instanceof ShapesEditor);
	}

	/*
	 * (non-Javadoc) Method declared on IViewPart. Treat this the same as part
	 * activation.
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
		partActivated(part);
	}
	
	/*
	 * (non-Javadoc) Method declared on ISelectionProvider.
	 */
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		getSelectionProvider().removeSelectionChangedListener(listener);
	}

	/*
	 * (non-Javadoc) Method declared on ISelectionChangedListener.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		getSelectionProvider().selectionChanged(event);
	}

	/*
	 * (non-Javadoc) Method declared on ISelectionProvider.
	 */
	public void setSelection(ISelection selection) {
		getSelectionProvider().setSelection(selection);
	}
}
