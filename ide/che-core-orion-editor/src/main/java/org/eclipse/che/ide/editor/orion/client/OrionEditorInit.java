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

import elemental.events.KeyboardEvent.KeyCode;
import java.util.List;
import java.util.Map;
import org.eclipse.che.ide.api.editor.annotation.AnnotationModel;
import org.eclipse.che.ide.api.editor.annotation.HasAnnotationRendering;
import org.eclipse.che.ide.api.editor.annotation.QueryAnnotationsEvent;
import org.eclipse.che.ide.api.editor.autosave.AutoSaveMode;
import org.eclipse.che.ide.api.editor.changeintercept.ChangeInterceptorProvider;
import org.eclipse.che.ide.api.editor.changeintercept.TextChange;
import org.eclipse.che.ide.api.editor.changeintercept.TextChangeInterceptor;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistProcessor;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistant;
import org.eclipse.che.ide.api.editor.codeassist.CodeAssistantFactory;
import org.eclipse.che.ide.api.editor.codeassist.CompletionProposal;
import org.eclipse.che.ide.api.editor.codeassist.CompletionReadyCallback;
import org.eclipse.che.ide.api.editor.codeassist.CompletionsSource;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.document.DocumentHandle;
import org.eclipse.che.ide.api.editor.editorconfig.TextEditorConfiguration;
import org.eclipse.che.ide.api.editor.events.CompletionRequestEvent;
import org.eclipse.che.ide.api.editor.events.CompletionRequestHandler;
import org.eclipse.che.ide.api.editor.events.DocumentChangedEvent;
import org.eclipse.che.ide.api.editor.events.TextChangeEvent;
import org.eclipse.che.ide.api.editor.events.TextChangeHandler;
import org.eclipse.che.ide.api.editor.formatter.ContentFormatter;
import org.eclipse.che.ide.api.editor.keymap.KeyBinding;
import org.eclipse.che.ide.api.editor.keymap.KeyBindingAction;
import org.eclipse.che.ide.api.editor.partition.DocumentPartitioner;
import org.eclipse.che.ide.api.editor.position.PositionConverter;
import org.eclipse.che.ide.api.editor.quickfix.QuickAssistAssistant;
import org.eclipse.che.ide.api.editor.reconciler.Reconciler;
import org.eclipse.che.ide.api.editor.signature.SignatureHelpProvider;
import org.eclipse.che.ide.api.editor.text.TextPosition;
import org.eclipse.che.ide.api.editor.text.TypedRegion;
import org.eclipse.che.ide.api.editor.texteditor.HasKeyBindings;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.util.browser.UserAgent;

/**
 * Initialization controller for the text editor. Sets-up (when available) the different components
 * that depend on the document being ready.
 */
public class OrionEditorInit {


  private final AutoSaveMode autoSaveMode;
  private final TextEditorConfiguration configuration;

  /** The quick assist assistant. */
  public OrionEditorInit(
      final AutoSaveMode autoSaveMode,
      final TextEditorConfiguration configuration) {
    this.autoSaveMode = autoSaveMode;
    this.configuration = configuration;
  }

  /**
   * Initialize the text editor.
   *
   * @param document to initialise with
   */
  public void init(Document document) {

  }

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
