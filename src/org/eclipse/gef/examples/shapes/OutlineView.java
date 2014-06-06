package org.eclipse.gef.examples.shapes;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.contentoutline.ContentOutline;

public class OutlineView extends ContentOutline {
	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part instanceof ShapesEditor)
			return;
		super.partActivated(part);
	}
}
