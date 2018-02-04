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

import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.events.DocumentChangedEvent;

/** An interface for editor widget implementations. */
public interface EditorWidget
    extends IsWidget,
        RequiresResize,
        HasBlurHandlers,
        HasChangeHandlers,
        HasFocusHandlers
{

  /**
   * Sets the content of the editor.<br>
   * The operation <em>must</em> send a {@link DocumentChangedEvent} on the document private event
   * bus.
   *
   * @param newValue the new contents
   * @param initializationHandler must be called when content injected in the Editor Widget
   */
  void setValue(String newValue, ContentInitializedHandler initializationHandler);

  /** Give the focus to the editor. */
  void setFocus();

  /** Refresh the editor widget. */
  void refresh();

  /** Callback that should be called when editor widget implementation is fully initialized. */
  interface WidgetInitializedCallback {
    void initialized(EditorWidget editorWidget);
  }
}
