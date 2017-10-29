/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.quick.start.ide;

import javax.inject.Inject;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.plugin.quick.start.ide.action.OpenGuidePanelAction;
import org.eclipse.che.plugin.quick.start.ide.panel.DocsPartPresenter;

/**
 * Quick start panel entry point.
 *
 * @author Oleksander Andriienko
 */
@Extension(title = "QuickStart")
public class QuickStartDocsExtension {

  @Inject
  public QuickStartDocsExtension(
      final DocsPartPresenter docsPartPresenter,
      GuideResources guideResources,
      OpenGuidePanelAction openGuidePanelAction,
      ActionManager actionManager) {

    guideResources.getGuideStyle().ensureInjected();

    actionManager.registerAction("open-guide-panel", openGuidePanelAction);
  }
}
