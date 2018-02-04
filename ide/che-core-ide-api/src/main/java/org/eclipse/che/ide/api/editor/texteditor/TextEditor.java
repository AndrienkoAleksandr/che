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

import org.eclipse.che.ide.api.editor.EditorPartPresenter;

/** Public view on the editor component. */
public interface TextEditor extends EditorPartPresenter {

  /** @return the text editor view implementation */
  @Override
  TextEditorPartView getView();

  /**
   * Return the content type of the editor content.<br>
   * Returns null if the type is not known yet.
   *
   * @return the content type
   */
  String getContentType();
}
