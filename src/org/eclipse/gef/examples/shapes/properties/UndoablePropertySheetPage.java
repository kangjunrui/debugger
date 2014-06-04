package org.eclipse.gef.examples.shapes.properties;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.properties.PropertySheetPage;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;

/**
 * PropertySheetPage extension that allows to perform undo/redo of property
 * value changes also in case the editor is not active.
 * 
 * @author anyssen
 * @since 3.7
 */
public class UndoablePropertySheetPage extends PropertySheetPage {

	private final IAction undoHandler;
	private final IAction redoHandler;
	private final CommandStack commandStack;
	private final CommandStackEventListener commandStackEventListener;

	/**
	 * Constructs a new {@link UndoablePropertySheetPage}.
	 * 
	 * @param commandStack
	 *            The {@link CommandStack} shared with the editor.
	 * @param undoAction
	 *            The global action handler to be registered for undo
	 *            operations.
	 * @param redoAction
	 *            The global action handler to be registered for redo
	 *            operations.
	 */
	public UndoablePropertySheetPage(final CommandStack commandStack,
			IAction undoAction, IAction redoAction) {
		this.undoHandler = undoAction;
		this.redoHandler = redoAction;
		this.commandStack = commandStack;
		this.commandStackEventListener = new CommandStackEventListener() {

			public void stackChanged(CommandStackEvent event) {
				if (event.getDetail() == CommandStack.PRE_UNDO
						|| event.getDetail() == CommandStack.PRE_REDO) {
					// ensure the property sheet entry looses its current edit
					// state, otherwise it may revert the undo/redo operation
					// within valueChanged when the editor is activated again.
					refresh();
				}
			}
		};
		commandStack.addCommandStackEventListener(commandStackEventListener);
		setRootEntry(new UndoablePropertySheetEntry(commandStack));
	}

	/**
	 * Overwritten to unregister command stack listener.
	 * 
	 * @see org.eclipse.ui.views.properties.PropertySheetPage#dispose()
	 */
	public void dispose() {
		if (commandStack != null)
			commandStack
					.removeCommandStackEventListener(commandStackEventListener);
		super.dispose();
	}

	/**
	 * Overwritten to register global action handlers for undo and redo.
	 * 
	 * @see org.eclipse.ui.views.properties.PropertySheetPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		super.setActionBars(actionBars);
		// register global action handlers for undo and redo
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
				undoHandler);
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
				redoHandler);

	}
}