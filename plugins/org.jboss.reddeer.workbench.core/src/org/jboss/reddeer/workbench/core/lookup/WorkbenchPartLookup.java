/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.reddeer.workbench.core.lookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.hamcrest.Matcher;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.util.Display;
import org.jboss.reddeer.common.util.ResultRunnable;

/**
 * Workbench part lookup contains methods for looking up specific workbench part
 * (mostly views).
 * 
 * @author rawagner
 *
 */
@SuppressWarnings("restriction")
public class WorkbenchPartLookup {

	private static final Logger log = Logger.getLogger(WorkbenchPartLookup.class);

	private static WorkbenchPartLookup instance;

	private WorkbenchPartLookup() {

	}

	/**
	 * Gets instance of WorkbenchPartLookup.
	 * 
	 * @return WorkbenchPartLookup instance
	 */
	public static WorkbenchPartLookup getInstance() {
		if (instance == null) {
			instance = new WorkbenchPartLookup();
		}
		return instance;
	}

	/**
	 * Gets active workbench part.
	 * 
	 * @return active workbench part
	 */
	public IWorkbenchPart getActiveWorkbenchPart() {
		return Display.syncExec(new ResultRunnable<IWorkbenchPart>() {

			@Override
			public IWorkbenchPart run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getActivePart();
			}
		});
	}

	/**
	 * Gets active workbench part reference from current active workbench
	 * window.
	 * 
	 * @return active workbench part reference
	 */
	public IWorkbenchPartReference findActiveWorkbenchPartReference() {
		return Display.syncExec(new ResultRunnable<IWorkbenchPartReference>() {

			@Override
			public IWorkbenchPartReference run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getActivePartReference();
			}
		});
	}

	/**
	 * Gets all currently opened views as list of view parts. Includes also
	 * views on non-active tabs.
	 *
	 * @return list of currently opened view parts
	 * @deprecated This method does not work properly due to
	 *             https://bugs.eclipse.org/bugs/show_bug.cgi?id=468948
	 */
	public List<IViewPart> getOpenViews() {
		return Display.syncExec(new ResultRunnable<List<IViewPart>>() {

			@Override
			public List<IViewPart> run() {
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				Set<IViewPart> views = new HashSet<IViewPart>();

				log.debug("Looking up all open views");
				// this is a workaround of
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=468948,
				// activePage.getViewReferences() should be used
				for (IViewReference viewReference : activePage.getViewReferences()) {
					IViewPart view = viewReference.getView(false);
					if (view == null) {
						log.trace("\tView '" + viewReference.getTitle() + "' was not open or it cannot be restored");
						continue;
					}

					IViewPart[] stackedViews = activePage.getViewStack(view);
					if (stackedViews == null) {
						log.trace("\tView '" + view.getTitle() + "' does not belong to the currently active page");
						continue;
					}

					for (IViewPart part : viewReference.getPage().getViewStack(viewReference.getView(false))) {
						log.trace("\tFound view '" + part.getTitle() + "'");
						views.add(part);
					}
				}
				return new ArrayList<IViewPart>(views);
			}
		});
	}

	/**
	 * Gets all view references.
	 * 
	 * @return array of all view references
	 */
	public IViewReference[] findAllViewReferences() {
		return Display.syncExec(new ResultRunnable<IViewReference[]>() {

			@Override
			public IViewReference[] run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getViewReferences();
			}
		});
	}

	/**
	 * Gets active view as a view reference.
	 *
	 * @return active view
	 * @deprecated This method does not work properly due to
	 *             https://bugs.eclipse.org/bugs/show_bug.cgi?id=468948
	 */
	public IViewReference getActiveView() {
		return Display.syncExec(new ResultRunnable<IViewReference>() {
			public IViewReference run() {
				IWorkbenchPartReference activeWorkbenchPart = findActiveWorkbenchPartReference();
				if (activeWorkbenchPart instanceof IViewReference) {
					return (IViewReference) activeWorkbenchPart;
				}
				return null;
			}
		});
	}

	/**
	 * Gets view as a view part with title matching specified matcher.
	 *
	 * @param name
	 *            matcher to match title
	 * @return view part matching specified matcher
	 * @deprecated This method does not work properly due to
	 *             https://bugs.eclipse.org/bugs/show_bug.cgi?id=468948
	 */
	public IViewPart getViewByTitle(final Matcher<String> name) {
		return Display.syncExec(new ResultRunnable<IViewPart>() {

			@Override
			public IViewPart run() {
				List<IViewPart> views = getOpenViews();

				for (IViewPart view : views) {
					if (name.matches(view.getViewSite().getRegisteredName())) {
						return view;
					}
				}

				logAllViews(views);
				return null;
			}

			private void logAllViews(List<IViewPart> views) {
				log.debug("View matching '" + name + "' not found");
				log.debug("List of found views:");
				for (IViewPart view : views) {
					log.debug("\t'" + view.getViewSite().getRegisteredName() + "'");
				}
			}
		});
	}

	/**
	 * Gets control object associated to active workbench.
	 * 
	 * @param activeWorkbenchReference
	 *            active workbench reference
	 * @return workbench control associated to active workbench
	 */
	public Control getWorkbenchControl(final IWorkbenchPartReference activeWorkbenchReference) {
		return Display.syncExec(new ResultRunnable<Control>() {

			@Override
			public Control run() {
				return ((WorkbenchPartReference) activeWorkbenchReference).getPane().getControl();
			}
		});
	}

	/**
	 * Gets the shell for active workbench.
	 *
	 * @param workbenchReference
	 *            the workbench reference
	 * @return the shell for active workbench
	 */
	public Shell getShellForActiveWorkbench() {
		IWorkbenchPartReference workbenchReference = findActiveWorkbenchPartReference();
		if (workbenchReference == null) {
			return null;
		}
		IWorkbenchPart wPart = workbenchReference.getPart(true);
		if (wPart == null) {
			return null;
		}
		IWorkbenchSite wSite = wPart.getSite();
		if (wSite == null) {
			return null;
		}
		return wSite.getShell();
	}

	public String getActiveWorkbenchPartTitle() {
		final IWorkbenchPartReference part = findActiveWorkbenchPartReference();
		if (part != null) {
			return Display.syncExec(new ResultRunnable<String>() {
				@Override
				public String run() {
					return part.getTitle();
				}
			});
		}
		return null;
	}
	
	public Control getActiveWorkbenchPartControl(){
		final IWorkbenchPartReference part = findActiveWorkbenchPartReference();
		if (part != null) {
			return getWorkbenchControl(part);
		}
		return null;
		
	}
	
	/**
	 * Gets active {@link IWorkbenchPartReference}.
	 * 
	 * @return active workbench part reference
	 */
	public IWorkbenchPartReference getActiveWorkbenchPartReference() {
		return Display.syncExec(new ResultRunnable<IWorkbenchPartReference>() {

			@Override
			public IWorkbenchPartReference run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getActivePartReference();
			}
		});
	}
}
