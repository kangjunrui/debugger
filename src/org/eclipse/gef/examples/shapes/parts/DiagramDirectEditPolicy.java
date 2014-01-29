package org.eclipse.gef.examples.shapes.parts;

import org.eclipse.draw2d.Label;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalEditPolicy;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.commands.ShapeRenameCommand;
import org.eclipse.gef.requests.DirectEditRequest;

public class DiagramDirectEditPolicy extends GraphicalEditPolicy {

	private boolean showing;

	public boolean understandsRequest(Request request) {
		if (RequestConstants.REQ_DIRECT_EDIT.equals(request.getType()))
			return true;
		return super.understandsRequest(request);
	}

	public Command getCommand(Request request) {
		if (REQ_DIRECT_EDIT == request.getType()) {
			DirectEditRequest direct_edit_request = (DirectEditRequest) request;
			String newValue = (String) direct_edit_request.getCellEditor()
					.getValue();
			if (newValue.equals(""))
				newValue = "NoName";

			ShapeRenameCommand cmd = new ShapeRenameCommand((Shape) getHost()
					.getModel(), newValue);
			return cmd;
		}
		return null;
	}

}
