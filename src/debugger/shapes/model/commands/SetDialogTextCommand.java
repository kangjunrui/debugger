package debugger.shapes.model.commands;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.commands.Command;

import debugger.shapes.model.Connection;
import debugger.shapes.model.ModelElement;
import debugger.shapes.model.Shape;

public class SetDialogTextCommand extends Command {

	private ModelElement model;
	private String newtext;
	private String oldtext;
	
	public SetDialogTextCommand(ModelElement model, String text) {
		this.model = model;
		this.newtext = text;
		if (model instanceof Connection)
			this.oldtext = ((Connection)model).getLineNote();
		else if (model instanceof Shape)
			this.oldtext = ((Shape)model).getComment();
		setLabel("shape rename");
	}

	/**
	 * Can execute if all the necessary information has been provided.
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		return model != null && newtext != null;
	}

	public void execute() {
		redo();
	}

	public void redo() {
		if (model instanceof Connection)
			((Connection)model).setLineNote(newtext);
		else if (model instanceof Shape)
			((Shape)model).setComment(newtext);
	}

	public void undo() {
		if (model instanceof Connection)
			((Connection)model).setLineNote(oldtext);
		else if (model instanceof Shape)
			((Shape)model).setComment(oldtext);
	}
}