package debugger.shapes.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.RGB;

public class ClipboardShape {
	//0 : rect, 1: ellipse
	public int type;
	
	public String name;
	public Point location;
	
	public String file;
	public int line;
	
	public boolean showfilename;
	public RGB color;
	
	public int icon;
	
	public int offset=-1;
	public int length=0;
	public String signature=null;
	
	public String comment;
}
