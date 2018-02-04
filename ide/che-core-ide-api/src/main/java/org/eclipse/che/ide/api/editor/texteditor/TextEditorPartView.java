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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.editor.codeassist.CompletionsSource;
import org.eclipse.che.ide.api.editor.text.TextPosition;

/**
 * View interface for the text editors components.
 *
 * @author "Mickaël Leduque"
 */
public interface TextEditorPartView extends RequiresResize, IsWidget, HasNotificationPanel {

  /**
   * Sets the view delegate.
   *
   * @param delegate the delegate
   */
  void setDelegate(Delegate delegate);

  /**
   * Sets the editor widget.
   *
   * @param editorWidget the widget
   */
  void setEditorWidget(EditorWidget editorWidget);

  /**
   * Display a placeholder in place of the editor widget.
   *
   * @param placeHolder the widget to display
   */
  void showPlaceHolder(Widget placeHolder);

  /**
   * Sets whether the info panel is visible.
   *
   * @param visible {@code true} to show the info panel, {@code false} to hide it
   */
  void setInfoPanelVisible(boolean visible);


  /** Delegate interface for this view. */
  interface Delegate extends RequiresResize {}
}
