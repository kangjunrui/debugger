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
package org.eclipse.gef.examples.shapes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import org.eclipse.gef.examples.shapes.model.ShapesDiagram;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create new new .shape-file. Those files can be used with the ShapesEditor
 * (see plugin.xml).
 * 
 * @author Elias Volanakis
 */
public class ShapesCreationWizard extends Wizard implements INewWizard {

	private static int fileCount = 1;
	private CreationPage page1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		// add pages to this wizard
		addPage(page1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// create pages for this wizard
		page1 = new CreationPage(workbench, selection);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		return page1.finish();
	}

	/**
	 * This WizardPage can create an empty .shapes file for the ShapesEditor.
	 */
	private class CreationPage extends WizardNewFileCreationPage {
		private static final String DEFAULT_EXTENSION = ".shapes";
		private final IWorkbench workbench;

		/**
		 * Create a new wizard page instance.
		 * 
		 * @param workbench
		 *            the current workbench
		 * @param selection
		 *            the current object selection
		 * @see ShapesCreationWizard#init(IWorkbench, IStructuredSelection)
		 */
		CreationPage(IWorkbench workbench, IStructuredSelection selection) {
			super("shapeCreationPage1", selection);
			this.workbench = workbench;
			setTitle("Create a new " + DEFAULT_EXTENSION + " file");
			setDescription("Create a new " + DEFAULT_EXTENSION + " file");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.dialogs.WizardNewFileCreationPage#createControl(org
		 * .eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			super.createControl(parent);
			setFileName("shapesExample" + fileCount + DEFAULT_EXTENSION);
			setPageComplete(validatePage());
		}

		/** Return a new ShapesDiagram instance. */
		private Object createDefaultContent() {
			return new ShapesDiagram();
		}

		/**
		 * This method will be invoked, when the "Finish" button is pressed.
		 * 
		 * @see ShapesCreationWizard#performFinish()
		 */
		boolean finish() {
			// create a new file, result != null if successful
			IFile newFile = createNewFile();
			fileCount++;

			// open newly created file in the editor
			IWorkbenchPage page = workbench.getActiveWorkbenchWindow()
					.getActivePage();
			if (newFile != null && page != null) {
				try {
					IDE.openEditor(page, newFile, true);
				} catch (PartInitException e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.dialogs.WizardNewFileCreationPage#getInitialContents()
		 */
		protected InputStream getInitialContents() {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Document doc = null;
				Element root = null;
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					doc = builder.newDocument();
					root = doc.createElement("diagram");
					doc.appendChild(root);
				} catch (Exception e) {
					e.printStackTrace();
					return null;// 如果出现异常，则不再往下执行
				}
				
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer;
				try {
					transformer = tf.newTransformer();
				} catch (TransformerConfigurationException e) {
					e.printStackTrace();
					return null;
				}
				DOMSource source = new DOMSource(doc);
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");// 设置文档的换行与缩进
				StreamResult result = new StreamResult(out);
				try {
					transformer.transform(source, result);
				} catch (TransformerException e) {
					e.printStackTrace();
					return null;
				}
				out.flush();
				out.close();
				return new ByteArrayInputStream(out.toByteArray());
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
			}
		}

		/**
		 * Return true, if the file name entered in this page is valid.
		 */
		private boolean validateFilename() {
			if (getFileName() != null
					&& getFileName().endsWith(DEFAULT_EXTENSION)) {
				return true;
			}
			setErrorMessage("The 'file' name must end with "
					+ DEFAULT_EXTENSION);
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
		 */
		protected boolean validatePage() {
			return super.validatePage() && validateFilename();
		}
	}
}
