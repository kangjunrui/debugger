/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
?* All rights reserved. This program and the accompanying materials
?* are made available under the terms of the Eclipse Public License v1.0
?* which accompanies this distribution, and is available at
?* http://www.eclipse.org/legal/epl-v10.html
?*
?* Contributors:
?*????Elias Volanakis - initial API and implementation
?*******************************************************************************/
package org.eclipse.gef.examples.shapes.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.gef.examples.shapes.ShapesEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * A container for multiple shapes. This is the "root" of the model data
 * structure.
 * 
 * @author Elias Volanakis
 */
public class ShapesDiagram extends ModelElement {

	/** Property ID to use when a child is added to this diagram. */
	public static final String CHILD_ADDED_PROP = "ShapesDiagram.ChildAdded";
	/** Property ID to use when a child is removed from this diagram. */
	public static final String CHILD_REMOVED_PROP = "ShapesDiagram.ChildRemoved";
	private static final long serialVersionUID = 1;
	private List<Shape> shapes = new ArrayList();

	private transient ShapesEditor editor;
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
				s.editor=id;
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
			if (s.editor!=-1){
				s.filename=getFile(s.editor);
				unrefEditor(s.editor);
				s.editor=-1;
			}
			firePropertyChange(CHILD_REMOVED_PROP, null, s);
			return true;
		}
		return false;
	}
	
	Map<String, Integer> fileToNumber=new HashMap<String, Integer>();
	Map<Integer, String> numberToFile=new HashMap<Integer, String>();
	Map<Integer, IEditorPart> numberToEditor=new HashMap<Integer, IEditorPart>();
	Map<Integer, Integer> numberToCount=new HashMap<Integer, Integer>();
	
	Map<IEditorPart, Integer> editorToNumber=new HashMap<IEditorPart, Integer>();
	Set<Integer> usedNumbers = new HashSet<Integer>();
	private int maxId=0;
	
	public void closeEditor(IEditorPart editor){
		Integer id=editorToNumber.get(editor);
		if (id!=null){
			editorToNumber.remove(editor);
			numberToEditor.put(id, null);
		}
	}
	
	public Map<String, Integer> getFileToNumber(){
		return fileToNumber;
	}
	
	public void addFile(String filename, int oldId) {
		usedNumbers.add(oldId);
		fileToNumber.put(filename, oldId);
		numberToFile.put(oldId, filename);
		numberToEditor.put(oldId, null);
		numberToCount.put(oldId, 0);
		if (maxId<oldId) maxId=oldId;
	}
	
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
	
	public int addFile(String prjname, String prjpath) {
		String filename=prjname+":"+prjpath;
		Integer id=fileToNumber.get(filename);
		if (id!=null) return id;
		
		id=getNewId();
		fileToNumber.put(filename, id);
		numberToEditor.put(id, null);
		numberToCount.put(id, 0);
		numberToFile.put(id, filename);
		return id;
	}
	
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
	
	public void refEditor(int id){
		int count=numberToCount.get(id);
		count++;
		numberToCount.put(id, count);
	}
	
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
	
	public String getFile(int id){
		return numberToFile.get(id);
	}
	
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
}