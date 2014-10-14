package debugger.shapes.actions;

import org.eclipse.swt.SWT;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;

import org.eclipse.gef.internal.GEFMessages;

/**
 * @author Eric Bordeau
 */
public class SyncRetargetAction extends RetargetAction {

	/**
	 * Constructs a new DeleteRetargetAction with the default ID, label and
	 * image.
	 */
	public SyncRetargetAction() {
		super(SyncAction.ID, "Sync", SWT.CHECK);
		setToolTipText("Show selected label from package explorer.");
		ISharedImages sharedImages = PlatformUI.getWorkbench()
				.getSharedImages();
		setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
		setDisabledImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED_DISABLED));
		setChecked(false);
	}

}
