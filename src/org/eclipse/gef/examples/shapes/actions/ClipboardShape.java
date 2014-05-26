package org.eclipse.gef.examples.shapes.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.RGB;

public class ClipboardShape {
	//1 : rect, 2: ellipse
	public int type;
	
	public String name;
	public Point location;
	
	public String file;
	public int line;
	
	public boolean showfilename;
	public RGB color;
}
