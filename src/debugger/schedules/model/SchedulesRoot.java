package debugger.schedules.model;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

//The list can contain special items
public class SchedulesRoot extends SchedulesItem {
	public ITreeContentProvider getContentProvider() {
		return new ITreeContentProvider() {
			//ITreeContentProvider
			
			//used by tree.
			public Object[] getChildren(Object parentElement) {
				SchedulesItem[] list = ((SchedulesItem) parentElement).list().toArray(new SchedulesItem[0]);
				if (list == null)
					return new Object[0];
				return list;
			}

			public Object getParent(Object element) {
				return ((SchedulesItem) element).getParentItem();
			}

			public boolean hasChildren(Object element) {
				List<SchedulesItem> list = ((SchedulesItem) element).list();
				if (list == null || list.size() == 0)
					return false;
				return true;
			}

			//should return tree roots from input element.
			//input element should not be a single root.
			
			public Object[] getElements(Object inputElement) {
				SchedulesItem[] list = ((SchedulesRoot) inputElement).list().toArray(new SchedulesItem[0]);
				if (list == null)
					return new Object[0];
				return list;
			}

			//IContentProvider
			
			public void dispose() {
			}

			//when happend, it is the contentprovider to update or refresh, ...
			
			Object input;
			
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				input = newInput;
			}
		};
	}

	public LabelProvider getLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((SchedulesItem) element).getIcon();
			}

			public String getText(Object element) {
				return ((SchedulesItem) element).getName();
			}
		};
	}
}
