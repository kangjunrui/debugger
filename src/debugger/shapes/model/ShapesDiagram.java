package debugger.shapes.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import debugger.shapes.ShapesEditor;
import debugger.shapes.UIUtil;
import debugger.shapes.parts.CompleteLinkedList;

//file element + prjname:eight types + id + packagename, note add member type:
//editor can have type method/field + package/private/public/protected
//file element can also be: file type or folder which have packagename,
//because filename is cached, so addfile will return id.
//use this id can get editor, or IFile, IFolder.(show in nodeeditpart)
//if no linenumber just show file. if getResource return IFolder, call getPackageName too. 
public class ShapesDiagram extends ModelElement {

	/** Property ID to use when a child is added to this diagram. */
	public static final String CHILD_ADDED_PROP = "ShapesDiagram.ChildAdded";
	/** Property ID to use when a child is removed from this diagram. */
	public static final String CHILD_REMOVED_PROP = "ShapesDiagram.ChildRemoved";
	private static final long serialVersionUID = 1;
	private List<Shape> shapes = new ArrayList();
	
	private transient ShapesEditor editor;
	public ShapesDiagram(UIUtil uiutil2) {
		uiutil=uiutil2;
	}
	public void setEditor(ShapesEditor editor){
		this.editor=editor;
	}
	public ShapesEditor getEditor(){
		return this.editor;
	}
	
	private Map<RGB, Color> colormap=new HashMap<RGB, Color>();
	private Map<Color, Integer> colorcount=new HashMap<Color, Integer>();
	
	public Color getColor(RGB rgb){
		Color color=colormap.get(rgb);
		if (color==null){
			color=new Color(null, rgb);
			colormap.put(rgb, color);
			colorcount.put(color, 0);
		}else{
			colorcount.put(color, colorcount.get(color)+1);
		}
		
		return color;
	}
	
	public void ungetColor(Color color){
		Integer count=colorcount.get(color);
		if (count!=null){
			if (count==1){
				colorcount.remove(color);
				colormap.remove(color.getRGB());
			}else{
				colorcount.put(color, count-1);
			}
		}
	}
	
	/**
	 * Add a shape to this diagram.
	 * 
	 * @param s
	 *            a non-null shape instance
	 * @return true, if the shape was added, false otherwise
	 */
	public boolean addChild(Shape s) {
		if (s != null && shapes.add(s)) {
			s.diagram=this;
			if (s.filename!=null){
				int id=addFile(s.filename);
				refEditor(id);
				s.filename=null;
				s.resource=id;
			}
			firePropertyChange(CHILD_ADDED_PROP, null, s);
			return true;
		}
		return false;
	}

	/**
	 * Return a List of Shapes in this diagram. The returned List should not be
	 * modified.
	 */
	public List<Shape> getChildren() {
		return shapes;
	}

	/**
	 * Remove a shape from this diagram.
	 * 
	 * @param s
	 *            a non-null shape instance;
	 * @return true, if the shape was removed, false otherwise
	 */
	public boolean removeChild(Shape s) {
		if (s != null && shapes.remove(s)) {
			if (s.resource!=-1){
				s.filename=getFile(s.resource);
				unrefEditor(s.resource);
				s.resource=-1;
			}
			firePropertyChange(CHILD_REMOVED_PROP, null, s);
			return true;
		}
		return false;
	}
	
	Map<String, Integer> fileToNumber=new HashMap<String, Integer>();
	Map<Integer, String> numberToFile=new HashMap<Integer, String>();
	//Map<Integer, String[]> numberToPackage=new HashMap<Integer, String[]>();
	
	Map<Integer, IEditorPart> numberToEditor=new HashMap<Integer, IEditorPart>();
	Map<IEditorPart, Integer> editorToNumber=new HashMap<IEditorPart, Integer>();
	Map<Integer, Integer> numberToCount=new HashMap<Integer, Integer>();
	
	Set<Integer> usedNumbers = new HashSet<Integer>();
	private int maxId=0;
	public UIUtil uiutil;
	
	//called when part listener detect part closed.
	public void closeEditor(IEditorPart editor){
		Integer id=editorToNumber.get(editor);
		if (id!=null){
			editorToNumber.remove(editor);
			numberToEditor.put(id, null);
		}
	}
	
	//used by save routine to create file element in xml.
	public Map<String, Integer> getFileToNumber(){
		return fileToNumber;
	}
	
	//automatically create id, used by addFile
	private int getNewId(){
		if (maxId==usedNumbers.size()){
			int id=maxId++;
			usedNumbers.add(id);
			return id;
		}
		
		for (int id=0; id<Integer.MAX_VALUE;id++){
			if (usedNumbers.contains(id)){
				continue;
			}else{
				usedNumbers.add(id);
				if (maxId<id) maxId = id;
				return id;
			}
		}
		
		System.out.println("No Id");
		return -1;
	}
	
	//addFile with manually assigned id. used by genShapes
	public void addFile(String filename, int oldId) {
		usedNumbers.add(oldId);
		fileToNumber.put(filename, oldId);
		numberToFile.put(oldId, filename);
		numberToEditor.put(oldId, null);
		numberToCount.put(oldId, 0);
		if (maxId<oldId) maxId=oldId;
	}
	
	public int addFile(String prjname, String prjpath) {
		return addFile(prjname+":"+prjpath);
	}
	
	//when paste, undo delete, create new shape
	public int addFile(String filename) {
		Integer id=fileToNumber.get(filename);
		if (id!=null) return id;
		
		id=getNewId();
		fileToNumber.put(filename, id);
		numberToEditor.put(id, null);
		numberToCount.put(id, 0);
		numberToFile.put(id, filename);
		return id;
	}
	
	// reference counting 
	
	//used whenever use any version of addFile.
	public void refEditor(int id){
		int count=numberToCount.get(id);
		count++;
		numberToCount.put(id, count);
	}
	
	//used only when remove child from container.
	public void unrefEditor(int id){
		int count=numberToCount.get(id);
		count--;
		if (count!=0){
			numberToCount.put(id, count);
		}else{
			numberToCount.remove(id);
			IEditorPart editor=numberToEditor.get(id);
			String filename = numberToFile.get(id);
			editorToNumber.remove(editor);
			fileToNumber.remove(filename);
			numberToFile.remove(id);
			numberToEditor.remove(id);
			usedNumbers.remove(id);
			if (maxId==id){
				maxId=0;
				for (int n : usedNumbers){
					if (n>maxId) maxId=n;
				}
			}
		}
	}
	
	//used when create delete-undo, or copy.
	//also used when setShowfilename is true.
	public String getFile(int id){
		return numberToFile.get(id);
	}
	
	public IJavaElement getElement(int id){
		String prjname;
		String fileorfoldername;
		String filename=numberToFile.get(id);
		int index=filename.indexOf(":");
		prjname=filename.substring(0, index);
		fileorfoldername=filename.substring(index+1, filename.length());
		
		IProject projectres = ResourcesPlugin.getWorkspace().getRoot().getProject(prjname);
		IJavaElement e = null;
		File prjdir=new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString(), prjname);
		if ((new File(prjdir, fileorfoldername)).isDirectory()){
			IFolder folderres = projectres.getFolder(fileorfoldername);
			e = JavaCore.create(folderres);
		}else{
			IFile fileres = projectres.getFile(fileorfoldername);
			e = JavaCore.create(fileres);
		}
		
		if (e == null) { // try a non Java resource
			System.out.println("null element");
			return null;
		}
		return e;
		
	}
	
	//used when node is double clicked.
	public IEditorPart getEditor(int id) {
		IEditorPart t=numberToEditor.get(id);
		if(t!=null){
			return t;
		}
		
		String prjname;
		String prjpath;
		String filename=numberToFile.get(id);
		int index=filename.indexOf(":");
		prjname=filename.substring(0, index);
		prjpath=filename.substring(index+1, filename.length());
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file=root.getFile(new Path(prjpath));
		try {
			t=IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, false);
		} catch (PartInitException e) {
			e.printStackTrace();
			return null;
		}
		numberToEditor.put(id, t);
		editorToNumber.put(t,id);
		
		return t;
	}
	
	/*
	Map<Integer, String> groupnames = new HashMap<Integer, String>();
	Map<Integer, Boolean> groupdirs = new HashMap<Integer, Boolean>();
	Map<Integer, CompleteLinkedList> groups = new HashMap<Integer, CompleteLinkedList>();
	public void refreshGroup(int group){
		CompleteLinkedList list = groups.get(group);
		if (list==null)
			return;
		
		Iterator<Shape> itor = list.getIterator();
		while(itor.hasNext()){
			itor.next().group=-1;
		}
		
		//if direction is true, which means from source to target
		//compare source and target, target is later
		//then compare children, right-bottom is later.
		//if no adjacent shape has group, make new group.
		//if two diff group meet, merge into one.
		//diff group displayed must be diff, same group may contain several group
	}
	
	public void refreshAll(){
		
	}
	
	public void addConnection(Connection conn, Shape to){
		
	}
	
	public void removeConnection(Connection conn){
		
	}
	*/
}