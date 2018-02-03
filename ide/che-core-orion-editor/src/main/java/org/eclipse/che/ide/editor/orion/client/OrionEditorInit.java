/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.editor.orion.client;

import org.eclipse.che.ide.api.editor.autosave.AutoSaveMode;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.editorconfig.TextEditorConfiguration;
import org.eclipse.che.ide.api.editor.reconciler.Reconciler;
import org.eclipse.che.ide.api.editor.signature.SignatureHelpProvider;

/**
 * Initialization controller for the text editor. Sets-up (when available) the different components
 * that depend on the document being ready.
 */
public class OrionEditorInit {

  private final AutoSaveMode autoSaveMode;
  private final TextEditorConfiguration configuration;

  /** The quick assist assistant. */
  public OrionEditorInit(
      final AutoSaveMode autoSaveMode, final TextEditorConfiguration configuration) {
    this.autoSaveMode = autoSaveMode;
    this.configuration = configuration;
  }

  /**
   * Initialize the text editor.
   *
   * @param document to initialise with
   */
  public void init(Document document) {}

  public void uninstall() {
    Reconciler reconciler = configuration.getReconciler();
    if (reconciler != null) {
      reconciler.uninstall();
    }
    SignatureHelpProvider signatureHelpProvider = configuration.getSignatureHelpProvider();
    if (signatureHelpProvider != null) {
      signatureHelpProvider.uninstall();
    }
    autoSaveMode.uninstall();
  }
}
