package debugger.schedules.model;

import java.util.List;

import org.eclipse.swt.graphics.Image;

//Display a list of SchedulesItem
public class SchedulesItem {

	public int ordinal;
	public String name;
	
	List<SchedulesItem> children;
	SchedulesItem parent;
	
	public int getOrdinal(){
		return ordinal;
	}
	
	public String getName() {
		return name;
	}

	public Image getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<SchedulesItem> list() {
		return children;
	}

	public Object getParentItem() {
		return parent;
	}
}
