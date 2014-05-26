/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Elias Volanakis - initial API and implementation
?*******************************************************************************/
package org.eclipse.gef.examples.shapes;

import java.awt.event.MouseAdapter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.NamedMember;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
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
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.internal.EditorSite;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
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

import org.eclipse.gef.examples.shapes.actions.ClipboardConnection;
import org.eclipse.gef.examples.shapes.actions.ClipboardDiagram;
import org.eclipse.gef.examples.shapes.actions.ClipboardShape;
import org.eclipse.gef.examples.shapes.actions.CopyAction;
import org.eclipse.gef.examples.shapes.actions.LabelStateAction;
import org.eclipse.gef.examples.shapes.actions.PasteAction;
import org.eclipse.gef.examples.shapes.actions.SyncAction;
import org.eclipse.gef.examples.shapes.model.Connection;
import org.eclipse.gef.examples.shapes.model.EllipticalShape;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.ShapesDiagram;
import org.eclipse.gef.examples.shapes.parts.ShapesEditPartFactory;
import org.eclipse.gef.examples.shapes.parts.ShapesTreeEditPartFactory;
import org.eclipse.ui.part.EditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A graphical editor with flyout palette that can edit .shapes files. The
 * binding between the .shapes file extension and this editor is done in
 * plugin.xml
 * 
 * @author Elias Volanakis
 */
public class ShapesEditor extends GraphicalEditorWithFlyoutPalette {

	/** This is the root of the editor's model. */
	private ShapesDiagram diagram;
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

		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new ShapesEditPartFactory());
		ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
		viewer.setRootEditPart(rootEditPart);

		ZoomManager manager = rootEditPart.getZoomManager();
		getActionRegistry().registerAction(new ZoomInAction(manager));
		getActionRegistry().registerAction(new ZoomOutAction(manager));
		// La liste des zooms possible. 1 = 100%
		double[] zoomLevels = new double[] { 0.25, 0.5, 0.75, 1.0, 1.5, 2.0,
				2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
		manager.setZoomLevels(zoomLevels);
		// On ajoute certains zooms prédéfinis
		ArrayList<String> zoomContributions = new ArrayList<String>();
		zoomContributions.add(ZoomManager.FIT_ALL);
		zoomContributions.add(ZoomManager.FIT_HEIGHT);
		zoomContributions.add(ZoomManager.FIT_WIDTH);
		manager.setZoomLevelContributions(zoomContributions);

		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.TRUE);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.TRUE);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(25,
				25));

		// configure the context menu provider
		ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(
				viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(cmProvider, viewer);

		getActionRegistry().registerAction(new ToggleGridAction(viewer));
		getActionRegistry().registerAction(new ZoomInAction(manager));
		getActionRegistry().registerAction(new ZoomOutAction(manager));
		/*
		 * KeyHandler keyHandler = new KeyHandler(); keyHandler.put(
		 * KeyStroke.getPressed(SWT.DEL, 127, 0),
		 * getActionRegistry().getAction(ActionFactory.DELETE.getId()));
		 * keyHandler.put( KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
		 * getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		 * 
		 * viewer.setKeyHandler(keyHandler);
		 */

		/*
		 * 					ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart)getViewer().getRootEditPart();
					double zoom = root.getZoomManager().getZoom();*/
		viewer.getControl().addMouseListener(
				new org.eclipse.swt.events.MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						if (e.button == 3) {
							PasteAction.menuPosition = new Point(e.x, e.y);
						}
					}
				});
		explorer = getSite().getWorkbenchWindow().getActivePage().findView("org.eclipse.jdt.ui.PackageExplorer");
		if (explorer!=null){
			explorer.getSite().getSelectionProvider().
			addSelectionChangedListener((ISelectionChangedListener)getActionRegistry().getAction(SyncAction.ID));
		}
	}

	ZoomViewPage zoom;
	
	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return ((ScalableFreeformRootEditPart) getGraphicalViewer()
					.getRootEditPart()).getZoomManager();
		else if (type==ZoomViewPage.class){
			if (zoom==null)
				zoom=new ZoomViewPage(getGraphicalViewer());
			return zoom;
		}else
			return super.getAdapter(type);
	}

	public void updatePasteAction() {
		ActionRegistry registry = getActionRegistry();
		WorkbenchPartAction action = (WorkbenchPartAction) registry
				.getAction(PasteAction.ID);
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

		action = new SyncAction(this, diagram);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new LabelStateAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new GetAction();
		registry.registerAction(action);

		action = new SetAction();
		registry.registerAction(action);
		
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

	private String packagePath = null;
	private String filePath = null;
	private int[] fieldStart = null;

	class GetAction extends Action {
		public GetAction() {
			setId("getaction");
			setText("get selection");
		}

		@Override
		public void run() {
			IViewPart explorer = ShapesEditor.this.getSite()
					.getWorkbenchWindow().getActivePage()
					.findView("org.eclipse.jdt.ui.PackageExplorer");
			org.eclipse.jface.viewers.TreeViewer fViewer = ((PackageExplorerPart) explorer)
					.getTreeViewer();
			if (explorer != null) {
				ISelection selection = explorer.getViewSite()
						.getSelectionProvider().getSelection();
				if (selection instanceof TreeSelection) {
					TreeSelection treesel = (TreeSelection) selection;
					Iterator itor = treesel.iterator();
					for (; itor.hasNext();) {
						Object o = itor.next();
						if (o instanceof NamedMember) {
							NamedMember field = (NamedMember) o;
							ISourceRange range = null;
							try {
								range = field.getNameRange();
							} catch (JavaModelException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out
									.println(field.getElementName()
											+ ":"
											+ (range != null ? (range
													.getOffset() + "," + range
													.getLength()) : ""));
						} else if (o instanceof CompilationUnit) {
							CompilationUnit file = (CompilationUnit) o;
							IJavaElement e = JavaCore.create((IFile) file
									.getResource());
							// file(LabelStateAction.java):a,/a/src/LabelStateAction.java,L/a/src/LabelStateAction.java
							// System.out.println("file("+file.getElementName()+"):"+file.getJavaProject().getProject().getName()+","+file.getPath()+","+file.getResource());
						} else if (o instanceof PackageFragment) {
							PackageFragment pk = (PackageFragment) o;
							String[] name = ((PackageFragment) o).names;
							IJavaElement e = JavaCore.create((IFolder) pk
									.getResource());
							if (e == null) { // try a non Java resource
								System.out.println("null element");
								continue;
							} else {
								if (e instanceof PackageFragmentRoot) {
									e = ((PackageFragmentRoot) e)
											.getPackageFragment(name);
								}
								ISelection newSelection = new StructuredSelection(
										e);
								if (fViewer.getSelection().equals(newSelection)) {
									fViewer.reveal(e);
								} else {
									fViewer.setSelection(newSelection, true);
								}
							}
							// package():a,/a/src,F/a/src
							// System.out.println("package("+pk.getElementName()+"):"+pk.getJavaProject().getProject().getName()+","+pk.getPath()+","+pk.getResource());
						}
					}
				} else {
					System.out.println(selection.getClass());
				}
			}
			super.run();
		}
	}

	class SetAction extends Action {
		public SetAction() {
			setId("setaction");
			setText("set selection");
		}

		@Override
		public void run() {
			IViewPart explorer = ShapesEditor.this.getSite()
					.getWorkbenchWindow().getActivePage()
					.findView("org.eclipse.jdt.ui.PackageExplorer");
			org.eclipse.jface.viewers.TreeViewer fViewer = ((PackageExplorerPart) explorer)
					.getTreeViewer();
			if (explorer != null) {
				ISelection selection = explorer.getViewSite()
						.getSelectionProvider().getSelection();
				if (selection instanceof TreeSelection) {
					TreeSelection treesel = (TreeSelection) selection;
					Iterator itor = treesel.iterator();
					for (; itor.hasNext();) {
						Object o = itor.next();
						if (o instanceof NamedMember) {
							NamedMember field = (NamedMember) o;
							ISourceRange range = null;
							try {
								range = field.getNameRange();
							} catch (JavaModelException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out
									.println(field.getElementName()
											+ ":"
											+ (range != null ? (range
													.getOffset() + "," + range
													.getLength()) : ""));
						} else if (o instanceof CompilationUnit) {
							CompilationUnit file = (CompilationUnit) o;
							IJavaElement e = JavaCore.create((IFile) file
									.getResource());
							// file(LabelStateAction.java):a,/a/src/LabelStateAction.java,L/a/src/LabelStateAction.java
							// System.out.println("file("+file.getElementName()+"):"+file.getJavaProject().getProject().getName()+","+file.getPath()+","+file.getResource());
						} else if (o instanceof PackageFragment) {
							PackageFragment pk = (PackageFragment) o;
							String[] name = ((PackageFragment) o).names;
							IJavaElement e = JavaCore.create((IFolder) pk
									.getResource());
							if (e == null) { // try a non Java resource
								System.out.println("null element");
								continue;
							} else {
								if (e instanceof PackageFragmentRoot) {
									e = ((PackageFragmentRoot) e)
											.getPackageFragment(name);
								}
								ISelection newSelection = new StructuredSelection(
										e);
								if (fViewer.getSelection().equals(newSelection)) {
									fViewer.reveal(e);
								} else {
									fViewer.setSelection(newSelection, true);
								}
							}
							// package():a,/a/src,F/a/src
							// System.out.println("package("+pk.getElementName()+"):"+pk.getJavaProject().getProject().getName()+","+pk.getPath()+","+pk.getResource());
						}
					}
				} else {
					System.out.println(selection.getClass());
				}
			}
			super.run();
		}
	}

	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	IWorkbenchPart part = null;
	IViewPart explorer;
	IPartListener listener = new IPartListener() {

		@Override
		public void partOpened(IWorkbenchPart part) {
			if (part instanceof PackageExplorerPart){
				explorer = (IViewPart)part;
				explorer.getSite().getSelectionProvider().
				addSelectionChangedListener((ISelectionChangedListener)getActionRegistry().getAction(SyncAction.ID));
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {

		}

		@Override
		public void partClosed(IWorkbenchPart part) {
			if (ShapesEditor.this.part == part) {
				ShapesEditor.this.part = null;
			}
			if (part instanceof IEditorPart) {
				diagram.closeEditor((IEditorPart) part);
			}
			if (part instanceof PackageExplorerPart){
				explorer = null;
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			partActivated(part);
		}

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (!(part instanceof ShapesEditor) && part instanceof IEditorPart) {
				ShapesEditor.this.part = part;
			}
		}
	};

	public Object getSelection() {		
		if (part==null || !getEditorSite().getPage().isPartVisible(part)){
			part=null;
			IWorkbenchPage page = getEditorSite().getPage();
			IEditorReference[] editors = page.getEditorReferences();
			
			for (IEditorReference editor: editors){
				IWorkbenchPart tpart = editor.getPart(false);
				if (tpart!=null && !(tpart instanceof ShapesEditor) && page.isPartVisible(tpart)){
					part=tpart;
					break;
				}
			}
		}
		 if (part==null) return new String[0];
		Method method = null;
		try {
			method = part.getClass().getMethod("getSelectionProvider");
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}
		org.eclipse.jface.viewers.ISelectionProvider provider = null;
		if (method != null) {
			try {
				provider = (org.eclipse.jface.viewers.ISelectionProvider) method
						.invoke(part);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (provider != null) {
			ISelection sel = provider.getSelection();
			if (sel != null
					&& sel instanceof org.eclipse.jface.text.TextSelection) {
				org.eclipse.jface.text.TextSelection textsel = ((org.eclipse.jface.text.TextSelection) sel);

				if (part instanceof ITextEditor) {
					// show file and line
					int currentLine = textsel.getStartLine(); // start from 0
					ITextEditor editor = (ITextEditor) part;
					IEditorInput input = editor.getEditorInput();
					if (input instanceof IFileEditorInput) {
						IFile file = ((IFileEditorInput) input).getFile();
						IPath prjpath = file.getFullPath();
						IProject prj = file.getProject();
						return new Object[] { textsel.getText(), prj.getName(),
								prjpath.toString(), currentLine };
					}
				}

				return new Object[] { textsel.getText(), null, 0 };
			}
		}

		return new String[0];
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		site.getPage().addPartListener(listener);
	}

	@Override
	public void dispose() {
		getSite().getPage().removePartListener(listener);
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.ui.parts.GraphicalEditor#commandStackChanged(java.util
	 * .EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	private void createOutputStream(OutputStream os) throws IOException {
		Document doc = null;
		Element root = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
			root = doc.createElement("diagram");
			doc.appendChild(root);
		} catch (Exception e) {
			e.printStackTrace();
			return;// 如果出现异常，则不再往下执行
		}

		hasSetDirty = false;

		Element element;

		Map<String, Integer> fileToNumber = diagram.getFileToNumber();
		for (String filename : fileToNumber.keySet()) {
			element = doc.createElement("file");
			element.setAttribute("id", "" + fileToNumber.get(filename));
			element.setAttribute("name", "" + filename);
			root.appendChild(element);
		}

		Shape[] shapes = diagram.getChildren().toArray(new Shape[0]);
		int id = 0;
		Map<Shape, Integer> shapemap = new HashMap<Shape, Integer>();
		for (Shape shapemodel : shapes) {
			if (shapemodel instanceof RectangularShape) {
				element = doc.createElement("rectshape");
			} else {
				element = doc.createElement("shape");
			}
			element.setAttribute("id", "" + id);
			element.setAttribute("name", "" + shapemodel.getName());
			Point location = shapemodel.getLocation();
			element.setAttribute("x", "" + location.x);
			element.setAttribute("y", "" + location.y);

			if (shapemodel.editor != -1) {
				element.setAttribute("editor", shapemodel.editor + "");
				element.setAttribute("line", shapemodel.line + "");
			}
			element.setAttribute("state", ""
					+ (shapemodel.showfilename ? 1 : 0));
			element.setAttribute("color", shapemodel.getColor().red + ","
					+ shapemodel.getColor().green + ","
					+ shapemodel.getColor().blue);

			root.appendChild(element);
			shapemap.put(shapemodel, id);
			id++;
		}
		for (Shape shapemodel : shapes) {
			List<Connection> conns = shapemodel.getSourceConnections();
			for (Connection conn : conns) {
				element = doc.createElement("connection");
				element.setAttribute("lineStyle", conn.getLineStyle() + "");
				element.setAttribute("source", shapemap.get(conn.getSource())
						+ "");
				element.setAttribute("target", shapemap.get(conn.getTarget())
						+ "");
				root.appendChild(element);
			}
		}

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			return;
		}
		DOMSource source = new DOMSource(doc);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");// 设置文档的换行与缩进
		StreamResult result = new StreamResult(os);
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			e.printStackTrace();
			return;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#
	 * createPaletteViewerProvider()
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor
	 * )
	 */
	public void doSave(IProgressMonitor monitor) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			createOutputStream(out);
			IFile file = ((IFileEditorInput) getEditorInput()).getFile();
			file.setContents(new ByteArrayInputStream(out.toByteArray()), true, // keep
																				// saving,
																				// even
																				// if
																				// IFile
																				// is
																				// out
																				// of
																				// sync
																				// with
																				// the
																				// Workspace
					false, // dont keep history
					monitor); // progress monitor
			getCommandStack().markSaveLocation();
		} catch (CoreException ce) {
			ce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
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
									createOutputStream(out);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getPaletteRoot
	 * ()
	 */
	protected PaletteRoot getPaletteRoot() {
		if (PALETTE_MODEL == null)
			PALETTE_MODEL = ShapesEditorPaletteFactory.createPalette();
		return PALETTE_MODEL;
	}

	private void handleLoadException(Exception e) {
		System.err.println("** Load failed. Using default model. **");
		e.printStackTrace();
		diagram = new ShapesDiagram();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		diagram = new ShapesDiagram();
		diagram.setEditor(this);

		try {
			IFile file = ((IFileEditorInput) input).getFile();
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(file.getContents());
			Element element = document.getDocumentElement();
			Map<String, Shape> shapemap = new HashMap<String, Shape>();
			NodeList nodes = element.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i).getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element node = (Element) nodes.item(i);
				if (node.getTagName() == "file") {
					diagram.addFile(node.getAttribute("name"),
							Integer.parseInt(node.getAttribute("id")));
					continue;
				} else if (node.getTagName() == "connection") {
					Connection connection = new Connection(shapemap.get(node
							.getAttribute("source")), shapemap.get(node
							.getAttribute("target")));
					connection.setLineStyle(Integer.parseInt(node
							.getAttribute("lineStyle")));
					continue;
				} else {
					Shape shape = null;
					if (node.getTagName() == "rectshape") {
						shape = new RectangularShape();
					} else if (node.getTagName() == "shape") {
						shape = new EllipticalShape();
					}
					shape.setName(node.getAttribute("name"));
					shape.setLocation(new Point(Integer.parseInt(node
							.getAttribute("x")), Integer.parseInt(node
							.getAttribute("y"))));
					String editor = node.getAttribute("editor");
					if (editor.length() != 0) {
						shape.editor = Integer.parseInt(editor);
						diagram.refEditor(shape.editor);
						shape.line = Integer
								.parseInt(node.getAttribute("line"));
					} else {
						shape.editor = -1;
					}
					String state = node.getAttribute("state");
					shape.showfilename = (state != null && state.equals("1")) ? true
							: false;

					String colorstr = node.getAttribute("color");
					if (!colorstr.isEmpty()) {
						String[] values = colorstr.split(",");
						int red = Integer.parseInt(values[0]);
						int green = Integer.parseInt(values[1]);
						int blue = Integer.parseInt(values[2]);
						shape.setColor(new RGB(red, green, blue));
					}
					diagram.addChild(shape);
					shapemap.put(node.getAttribute("id"), shape);
				}
			}// end for i

			setPartName(file.getName());
		} catch (IOException e) {
			handleLoadException(e);
		} catch (CoreException e) {
			handleLoadException(e);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}