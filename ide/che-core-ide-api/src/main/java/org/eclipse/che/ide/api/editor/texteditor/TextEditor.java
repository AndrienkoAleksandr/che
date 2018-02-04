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
package org.eclipse.che.ide.api.editor.texteditor;

import javax.validation.constraints.NotNull;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.editorconfig.TextEditorConfiguration;

/** Public view on the editor component. */
public interface TextEditor extends EditorPartPresenter {

  /**
   * Initializes this editor with the configuration and document provider.
   *
   * @param configuration the configuration of this editor.
   */
  void initialize(@NotNull TextEditorConfiguration configuration);

  /** @return the text editor view implementation */
  @Override
  TextEditorPartView getView();

  /**
   * Closes this text editor after optionally saving changes.
   *
   * @param save <code>true</code> if unsaved changed should be saved, and <code>false</code> if
   *     unsaved changed should be discarded
   */
  @Override
  void close(boolean save);

  /**
   * Return the content type of the editor content.<br>
   * Returns null if the type is not known yet.
   *
   * @return the content type
   */
  String getContentType();
}
