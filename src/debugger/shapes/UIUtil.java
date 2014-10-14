package debugger.shapes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.NamedMember;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceField;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.internal.core.AnnotatableInfo;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

import debugger.shapes.actions.SyncAction;
import debugger.shapes.model.Shape;
import debugger.shapes.model.ShapesDiagram;

public class UIUtil {

	IWorkbenchPart part = null;
	PackageExplorerPart explorer;
	IPartListener listener = new IPartListener() {

		@Override
		public void partOpened(IWorkbenchPart part) {
			if (part instanceof PackageExplorerPart){
				explorer = (PackageExplorerPart)part;
				if (shapeseditor.syncaction!=null){
					explorer.getSite().getSelectionProvider().
					addSelectionChangedListener(shapeseditor.syncaction);
				}
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {

		}

		@Override
		public void partClosed(IWorkbenchPart part) {
			if (part instanceof PackageExplorerPart){
				explorer = null;
			}else if (UIUtil.this.part == part) {
				UIUtil.this.part = null;
			}
			
			if (part instanceof IEditorPart) {
				shapeseditor.diagram.closeEditor((IEditorPart) part);
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			partActivated(part);

		}

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (!(part instanceof ShapesEditor) 
					&& (part instanceof IEditorPart || part instanceof PackageExplorerPart)) {
				UIUtil.this.part = part;
			}

		}
	};
	
	public void setExplorerSelection(Shape shape){
		if (shape.getIcon()<9){
			IEditorPart editor = shape.diagram.getEditor(shape.resource);
			if (editor!=null && editor instanceof ITextEditor){
				try{
					Method showSelection = PackageExplorerPart.class.getDeclaredMethod("editorActivated", new Class[] {IEditorPart.class}); //$NON-NLS-1$
					showSelection.setAccessible(true);
					showSelection.invoke(explorer, new Object[] {part});
				}catch(Exception e){
					e.printStackTrace();
					return;
				}
			}
		}else{
			IJavaElement resource=shape.diagram.getElement(shape.resource);
			if (shape.getIcon()==10 || shape.getIcon()==11){
				resource=((CompilationUnit)resource).getType(shape.signature);
			}/*else if (shape.getIcon()==12){
				if (resource instanceof PackageFragmentRoot && shape.signature!=null) {
					resource = ((PackageFragmentRoot) resource)
							.getPackageFragment(shape.signature.split("."));
				}
			}*/
			
			if (explorer != null) {
				org.eclipse.jface.viewers.TreeViewer fViewer = ((PackageExplorerPart) explorer)
						.getTreeViewer();
				ISelection newSelection = new StructuredSelection(resource);
				if (fViewer.getSelection().equals(newSelection)) {
					fViewer.reveal(resource);
				} else {
					fViewer.setSelection(newSelection, true);
				}
				
			}
		}
	}
	
	public Shape[] getExplorerSelection(Shape[] dummy, Object[] selection){
		ArrayList<Shape> shapes = new ArrayList<Shape>();
		for (int i=0;i<dummy.length;i++) {
			Object o = selection[i];
			Shape shape=dummy[i];

			JavaElement e=(JavaElement)o;
			
			ISourceRange range = null;
			try {
				if (e instanceof SourceRefElement)
					range = ((SourceRefElement)e).getNameRange();
			} catch (JavaModelException ex) {
				ex.printStackTrace();
				continue;
			}
			
			//need convert to line number
			String name=e.getElementName();
			int offset=-1, length=0;
			if (range!=null){
				offset = range.getOffset();
				length = range.getLength();
			}
			
			IResource resource=e.resource();
			String prjname = resource.getProject().getProject().getName();
			String filename = resource.getFullPath().toString();
			int index = filename.indexOf("/", 1);
			filename = filename.substring(index);
			shape.setName(name);
			shape.offset=offset; // start from 0
			shape.length=length;
			shape.filename=prjname+":"+filename;
				
			if (o instanceof NamedMember) {
				//==only for icon purpose==
				
				int memberdec=0;
				if (o instanceof SourceField){
					memberdec=1;
				}
				
				if (o instanceof SourceMethod // 2, 4, 6, 8 = public protected default private			
						|| o instanceof SourceField){ 	// 1, 3, 5, 7 = public protected default private
					Object tinfo=null;
					try {
						tinfo = ((NamedMember)o).getElementInfo();
					} catch (JavaModelException ex) {
						ex.printStackTrace();
						continue;
					}
					if (!(tinfo instanceof AnnotatableInfo)){
						continue;
					}
					
					AnnotatableInfo info = (AnnotatableInfo)tinfo;
					if ((info.getModifiers() & ClassFileConstants.AccPublic) != 0){
						shape.setIcon(2-memberdec);
					}else if ((info.getModifiers() & ClassFileConstants.AccPrivate) != 0){
						shape.setIcon(8-memberdec);
					}else if ((info.getModifiers() & ClassFileConstants.AccProtected) != 0){
						shape.setIcon(4-memberdec);
					}else{
						shape.setIcon(6-memberdec);
					}
					shapes.add(shape);
				}else if (o instanceof SourceType){ //10 class 11 interface
					//class or interface
					SourceType type = (SourceType)o;
					shape.signature=type.getTypeQualifiedName();
					try {
						if (type.isClass() || type.isEnum()){
							shape.setIcon(10);
						}else if (type.isInterface()){
							shape.setIcon(11);
						}else{
							continue;
						}
					} catch (JavaModelException ex) {
						ex.printStackTrace();
						continue;
					}
					shapes.add(shape);
				}
				
			} else if (o instanceof CompilationUnit) { //9 javafile
				shape.setIcon(9);
				shapes.add(shape);
			} else if (o instanceof PackageFragment) { // 12 packagefolder
				/*PackageFragment pk = (PackageFragment) o;
				
				String[] packagename = pk.names;
				StringBuilder signature = new StringBuilder();
				for (String name:packagename){
					signature.append(name);
					signature.append(".");
				}
				shape.signature=signature.toString();*/
				shape.setIcon(12);
				shapes.add(shape);
			} else if (o instanceof PackageFragmentRoot) { // 13 packageroot
				shape.setIcon(13);
				shapes.add(shape);
			}
		}
		return shapes.toArray(new Shape[0]);
	}
	
	public Object[] prepareSelection(){
		ArrayList<Object> objs = new ArrayList<Object>();
		
		if (part==null || !getSite().getPage().isPartVisible(part) || part!=explorer){
			return objs.toArray(new Object[0]);
		}
		
		org.eclipse.jface.viewers.TreeViewer fViewer = explorer.getTreeViewer();
		ISelection selection = explorer.getViewSite()
				.getSelectionProvider().getSelection();
		if (selection instanceof TreeSelection) {
			TreeSelection treesel = (TreeSelection) selection;
			Iterator itor = treesel.iterator();
			while (itor.hasNext()) {
				Object o = itor.next();
				if (o instanceof JavaElement){
					objs.add(o);
				}
			}
		} else {
			System.out.println(selection.getClass());
		}
		return objs.toArray(new Object[0]);
	}
	
	@SuppressWarnings("deprecation")
	public boolean setEditorSelection(Shape shape){
		if (shape.getIcon()>=9)
			return false;
		
		IEditorPart editor;
		int offset=-1;
		if (shape.resource!=-1){
			editor=shape.diagram.getEditor(shape.resource);
						
			if (editor!=null && editor instanceof ITextEditor){
				//bring to top
				editor.getEditorSite().getPage().bringToTop(editor);
				ITextEditor text = (ITextEditor)editor;
				IDocumentProvider provider= text.getDocumentProvider();
				IDocument document= provider.getDocument(text.getEditorInput());
				
				//select and reveal
				try {
					if (shape.line!=-1){
						shape.offset=document.getLineOffset(shape.line);
						shape.line=-1;
						shape.length=0;
						shapeseditor.setDirty(true);
					}
					
					if (shape.signature!=null){
						offset=((shape.offset==-1)?0:shape.offset);
						int newoffset=offset;
						String txt = document.get(offset,shape.signature.length());
						if (!txt.equals(shape.signature)){
							//forward
							newoffset=document.search(offset, shape.signature, true, true, false);
							if (newoffset==-1){
								//backward
								newoffset=document.search(offset, shape.signature, false, true, false);
							}
						}
						if (offset!=newoffset){
							offset = newoffset;
							shape.offset=offset;
							shapeseditor.setDirty(true);
						}
					}else{
						offset = shape.offset;
					}
					
					if (offset!=-1)
						text.selectAndReveal(offset, shape.length);
				} catch (BadLocationException x) {
					x.printStackTrace();
				}
			}
		}
		return true;
	}
	
	//if length==0, insert marker.
	public boolean getEditorSelection(Shape shape) {		
		if (part==null || !getSite().getPage().isPartVisible(part) || part==explorer){
			part=null;
			IWorkbenchPage page = getSite().getPage();
			IEditorReference[] editors = page.getEditorReferences();
			
			for (IEditorReference editor: editors){
				IWorkbenchPart tpart = editor.getPart(false);
				if (tpart!=null && !(tpart instanceof ShapesEditor) && page.isPartVisible(tpart)){
					part=tpart;
					break;
				}
			}
		}
		if (part==null) return false;
		
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
					ITextEditor editor = (ITextEditor)part;
					IEditorInput input = editor.getEditorInput();
					
					// show file and line
					String signature = null;
					if (textsel.getLength()==0){
						signature = "#"+Long.toHexString(new Date().getTime())+"#";
					}
					
					if (input instanceof IFileEditorInput) {
						IFile file = ((IFileEditorInput) input).getFile();
						IPath prjpath = file.getFullPath();
						IProject prj = file.getProject();

						if (signature!=null){
							shape.setName(signature);
							shape.signature = signature;
							shape.offset=textsel.getOffset();
							shape.length=signature.length();
						}else{
							shape.setName(textsel.getText());
							shape.offset=textsel.getOffset();
							shape.length=textsel.getLength();
						}
						shape.filename=prj.getName()+":"+prjpath.toString();
						shape.setIcon(0);
						return true;
					}
				}

				return true;
			}
		}

		return false;
	}

	public void applySelection(Shape shape) {
		if (shape.signature!=null && shape.getIcon()==0){
			IWorkbenchPart tpart = shapeseditor.diagram.getEditor(shape.resource);
			if (tpart!=null && tpart instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor)tpart;
				IEditorInput input = editor.getEditorInput();
				int lineStart = 0;
				IDocument document= editor.getDocumentProvider().getDocument(input);
				try {
					document.replace(shape.offset, 0, "/*"+shape.signature+"*/");
				} catch (BadLocationException x) {
					x.printStackTrace();
				}
			}
		}
	}
	
	ShapesEditor shapeseditor;
	public IWorkbenchPartSite getSite(){
		return shapeseditor.getSite();
	}
	public UIUtil(ShapesEditor editor) {
		this.shapeseditor=editor;
		
		getSite().getPage().addPartListener(listener);
		
		explorer = (PackageExplorerPart)getSite().getWorkbenchWindow().getActivePage().findView("org.eclipse.jdt.ui.PackageExplorer");
		if (explorer!=null && shapeseditor.syncaction!=null){
			explorer.getSite().getSelectionProvider().
			addSelectionChangedListener(shapeseditor.syncaction);
		}
	}

	public void init(){
		if (explorer!=null && shapeseditor.syncaction!=null){
			explorer.getSite().getSelectionProvider().
			addSelectionChangedListener(shapeseditor.syncaction);
		}
	}
	public void dispose() {
		getSite().getPage().removePartListener(listener);
	}
}
