package debugger.shapes;

import java.awt.event.MouseAdapter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.NamedMember;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceField;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.internal.EditorSite;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.TreeViewer;

import org.eclipse.ui.part.EditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import debugger.shapes.actions.ClipboardConnection;
import debugger.shapes.actions.ClipboardDiagram;
import debugger.shapes.actions.ClipboardShape;
import debugger.shapes.actions.CopyAction;
import debugger.shapes.actions.LabelStateAction;
import debugger.shapes.actions.PasteAction;
import debugger.shapes.actions.SyncAction;
import debugger.shapes.model.Connection;
import debugger.shapes.model.ModelUtils;
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;
import debugger.shapes.parts.DiagramEditPart;
import debugger.shapes.parts.ShapesEditPartFactory;
import debugger.shapes.parts.ShapesTreeEditPartFactory;
import debugger.shapes.properties.CommentDialog;
import debugger.shapes.properties.UndoablePropertySheetPage;

public class ShapesEditor extends GraphicalEditorWithFlyoutPalette {

	/** This is the root of the editor's model. */
	public ShapesDiagram diagram;
	public UIUtil uiutil;
	/** Palette component, holding the tools and shapes. */
	private static PaletteRoot PALETTE_MODEL;

	/** Create a new ShapesEditor instance. This is called by the Workspace. */
	public ShapesEditor() {
		setEditDomain(new DefaultEditDomain(this));
	}

	/**
	 * Configure the graphical viewer before it receives contents.
	 * <p>
	 * This is the place to choose an appropriate RootEditPart and
	 * EditPartFactory for your editor. The RootEditPart determines the behavior
	 * of the editor's "work-area". For example, GEF includes zoomable and
	 * scrollable root edit parts. The EditPartFactory maps model elements to
	 * edit parts (controllers).
	 * </p>
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		
		registry = new ImageRegistry();
		createImage(0,"icons/locate.gif");
		createImage(1,"icons/publicfield.gif");
		createImage(2,"icons/publicmethod.gif");
		createImage(3,"icons/protectedfield.gif");
		createImage(4,"icons/protectedmethod.gif");
		createImage(5,"icons/defaultfield.gif");
		createImage(6,"icons/defaultmethod.gif");
		createImage(7,"icons/privatefield.gif");
		createImage(8,"icons/privatemethod.gif");
		createImage(9,"icons/javafile.gif");
		createImage(10,"icons/class.gif");
		createImage(11,"icons/interface.gif");
		createImage(12,"icons/packagefolder.gif");
		createImage(13,"icons/packageroot.gif");
		
		createImage("close","icons/close.gif");
		
		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new ShapesEditPartFactory(this));
		ShapesRootEditPart rootEditPart = new ShapesRootEditPart();
		viewer.setRootEditPart(rootEditPart);

		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.TRUE);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.TRUE);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(25, 25));

		// configure the context menu provider
		ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(
				viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(cmProvider, viewer);

		/*
		 KeyHandler keyHandler = new KeyHandler(); keyHandler.put(
		 KeyStroke.getPressed(SWT.DEL, 127, 0),
		 getActionRegistry().getAction(ActionFactory.DELETE.getId()));
		 keyHandler.put( KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
		 getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		 
		 viewer.setKeyHandler(keyHandler);
		 */

		viewer.getControl().addMouseListener(
				new org.eclipse.swt.events.MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						if (e.button == 3) {
							PasteAction.menuPosition = new Point(e.x, e.y);
						}
					}
				});
	}
	
	CommentDialog commentdlg = null;
	public Object getAdapter(Class type) {
		if (type == org.eclipse.ui.views.properties.IPropertySheetPage.class) {
			return new UndoablePropertySheetPage(getCommandStack(),
					getActionRegistry().getAction(ActionFactory.UNDO.getId()),
					getActionRegistry().getAction(ActionFactory.REDO.getId()));
		}else if (type == CommentDialog.class){
			return getCommentDialog();
		}else if (type == IContentOutlinePage.class){
			return new ShapesOutlinePage(new TreeViewer());
		}else
			return super.getAdapter(type);
	}

	public CommentDialog getCommentDialog(){
		if (commentdlg == null){
			commentdlg=new CommentDialog(getGraphicalViewer().getControl().getShell(), this, getCommandStack());
		}
		return commentdlg;
	}
	
	public void updatePasteAction() {
		ActionRegistry registry = getActionRegistry();
		WorkbenchPartAction action = (WorkbenchPartAction) registry.getAction(PasteAction.ID);
		action.update();
	}

	protected void createActions() {
		super.createActions();
		ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new CopyAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new PasteAction(this, diagram);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		/*syncaction = new SyncAction(this, diagram);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		uiutil.init();*/
		
		action = new LabelStateAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
		
		/*
		IActionBars bars = ((EditorSite)getSite()).getActionBars();
		   if (bars != null) {
		      IToolBarManager tbm = bars.getToolBarManager();
		      if (tbm != null) {
		         IContributionItem[] items = tbm.getItems();
		         for (IContributionItem item : items)
		        	 if (item.getId()==SyncAction.ID){
		        		 System.out.println(item.getClass());
		        	 }
		         }
		      }*/
	};

	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}


	@Override
	public void init(IEditorSite site, IEditorInput input)	throws PartInitException {
		super.init(site, input);
	}

	@Override
	public void dispose() {
		uiutil.dispose();
		super.dispose();
	}

	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	protected PaletteViewerProvider createPaletteViewerProvider() {
		return new PaletteViewerProvider(getEditDomain()) {
			protected void configurePaletteViewer(PaletteViewer viewer) {
				super.configurePaletteViewer(viewer);
				// create a drag source listener for this palette viewer
				// together with an appropriate transfer drop target listener,
				// this will enable
				// model element creation by dragging a
				// CombinatedTemplateCreationEntries
				// from the palette into the editor
				// @see ShapesEditor#createTransferDropTargetListener()
				viewer.addDragSourceListener(new TemplateTransferDragSourceListener(
						viewer));
			}
		};
	}

	/**
	 * Create a transfer drop target listener. When using a
	 * CombinedTemplateCreationEntry tool in the palette, this will enable model
	 * element creation by dragging from the palette.
	 * 
	 * @see #createPaletteViewerProvider()
	 */
	private TransferDropTargetListener createTransferDropTargetListener() {
		return new TemplateTransferDropTargetListener(getGraphicalViewer()) {
			protected CreationFactory getFactory(Object template) {
				return new SimpleFactory((Class) template);
			}
		};
	}

	public void doSave(IProgressMonitor monitor) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ModelUtils.save(out, diagram);
			hasSetDirty = false;
			IFile file = ((IFileEditorInput) getEditorInput()).getFile();
			// true - keep saving,even if IFile is out of sync with the Workspace
			// false - dont keep history
			// monitor -  progress monitor
			file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, monitor); 
			getCommandStack().markSaveLocation();
		} catch (CoreException ce) {
			ce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void doSaveAs() {
		// Show a SaveAs dialog
		Shell shell = getSite().getWorkbenchWindow().getShell();
		SaveAsDialog dialog = new SaveAsDialog(shell);
		dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
		dialog.open();

		IPath path = dialog.getResult();
		if (path != null) {
			// try to save the editor's contents under a different file name
			final IFile file = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(path);
			try {
				new ProgressMonitorDialog(shell).run(false, // don't fork
						false, // not cancelable
						new WorkspaceModifyOperation() { // run this operation
							public void execute(final IProgressMonitor monitor) {
								try {
									ByteArrayOutputStream out = new ByteArrayOutputStream();
									ModelUtils.save(out, diagram);
									hasSetDirty = false;
									file.create(
											new ByteArrayInputStream(out
													.toByteArray()), // contents
											true, // keep saving, even if IFile
													// is out of sync with the
													// Workspace
											monitor); // progress monitor
								} catch (CoreException ce) {
									ce.printStackTrace();
								} catch (IOException ioe) {
									ioe.printStackTrace();
								}
							}
						});
				// set input to the new file
				setInput(new FileEditorInput(file));
				getCommandStack().markSaveLocation();
			} catch (InterruptedException ie) {
				// should not happen, since the monitor dialog is not cancelable
				ie.printStackTrace();
			} catch (InvocationTargetException ite) {
				ite.printStackTrace();
			}
		}
	}

	ShapesDiagram getModel() {
		return diagram;
	}

	protected PaletteRoot getPaletteRoot() {
		if (PALETTE_MODEL == null)
			PALETTE_MODEL = ShapesEditorPaletteFactory.createPalette();
		return PALETTE_MODEL;
	}

	private void handleLoadException(Exception e) {
		System.err.println("** Load failed. Using default model. **");
		e.printStackTrace();
		diagram = new ShapesDiagram(uiutil);
		diagram.setEditor(this);
	}

	/**
	 * Set up the editor's inital content (after creation).
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#initializeGraphicalViewer()
	 */
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setContents(getModel()); // set the contents of this editor

		// listen for dropped parts
		viewer.addDropTargetListener(createTransferDropTargetListener());
	}

	public boolean isSaveAsAllowed() {
		return true;
	}

	boolean hasSetDirty = false;

	public void setDirty(boolean isDirty) {
		hasSetDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isDirty() {
		if (hasSetDirty)
			return true;
		return super.isDirty();
	}

	protected void setInput(IEditorInput input) {
		super.setInput(input);
		if (uiutil==null)
			uiutil = new UIUtil(this);
		diagram = new ShapesDiagram(uiutil);
		diagram.setEditor(this);
		IFile file = ((IFileEditorInput) input).getFile();
		IPath loc = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		try{
			ModelUtils.load(new File(loc.toString()+file.getFullPath()), diagram);
		} catch (IOException e) {
			handleLoadException(e);
		} catch (CoreException e) {
			handleLoadException(e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		setPartName(file.getName());
	}

	private ImageRegistry registry;
	protected ISelectionChangedListener syncaction;
	
	private Image createImage(int icon, String name) {
		return createImage(icon+"", name);
	}
	
	private Image createImage(String icon, String name) {
		InputStream stream = ShapesPlugin.class.getResourceAsStream(name);
		if (stream==null){
			System.out.println(name+" no stream");
			return null;
		}
		Image image = new Image(Display.getCurrent(), stream);
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		registry.put(icon+"", image);
		return image;
	}
	
	public Image getImage(int icon){
		return registry.get(icon+"");
	}
	

	/**
	 * Creates an outline pagebook for this editor.
	 */
	public class ShapesOutlinePage extends ContentOutlinePage {
		/**
		 * Create a new outline page for the shapes editor.
		 * 
		 * @param viewer
		 *            a viewer (TreeViewer instance) used for this outline page
		 * @throws IllegalArgumentException
		 *             if editor is null
		 */
		public ShapesOutlinePage(EditPartViewer viewer) {
			super(viewer);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite
		 * )
		 */
		public void createControl(Composite parent) {
			// create outline viewer page
			getViewer().createControl(parent);
			// configure outline viewer
			getViewer().setEditDomain(getEditDomain());
			getViewer().setEditPartFactory(new ShapesTreeEditPartFactory(ShapesEditor.this));
			// configure & add context menu to viewer
			ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(
					getViewer(), getActionRegistry());
			getViewer().setContextMenu(cmProvider);
			getSite().registerContextMenu(
					"org.eclipse.gef.examples.shapes.outline.contextmenu",
					cmProvider, getSite().getSelectionProvider());
			// hook outline viewer
			getSelectionSynchronizer().addViewer(getViewer());
			// initialize outline viewer with model
			getViewer().setContents(getModel());
			// show outline viewer
		}

		public void dispose() {
			// unhook outline viewer
			getSelectionSynchronizer().removeViewer(getViewer());
			// dispose
			super.dispose();
		}

		public Control getControl() {
			return getViewer().getControl();
		}

		/**
		 * @see org.eclipse.ui.part.IPageBookViewPage#init(org.eclipse.ui.part.IPageSite)
		 */
		public void init(IPageSite pageSite) {
			super.init(pageSite);
			ActionRegistry registry = getActionRegistry();
			IActionBars bars = pageSite.getActionBars();
			String id = ActionFactory.UNDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.REDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.DELETE.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
		}
	}
}