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
import java.util.List;
import javax.validation.constraints.NotNull;
import org.eclipse.che.ide.api.editor.codeassist.AdditionalInfoCallback;
import org.eclipse.che.ide.api.editor.codeassist.CompletionProposal;
import org.eclipse.che.ide.api.editor.codeassist.CompletionsSource;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.events.DocumentChangedEvent;
import org.eclipse.che.ide.api.editor.events.HasCursorActivityHandlers;
import org.eclipse.che.ide.api.editor.events.HasGutterClickHandlers;
import org.eclipse.che.ide.api.editor.hotkeys.HotKeyItem;
import org.eclipse.che.ide.api.editor.keymap.Keymap;
import org.eclipse.che.ide.api.editor.position.PositionConverter;
import org.eclipse.che.ide.api.editor.text.Region;

/** An interface for editor widget implementations. */
public interface EditorWidget
    extends IsWidget,
        RequiresResize,
        HasBlurHandlers,
        HasChangeHandlers,
        HasFocusHandlers,
        HasGutterClickHandlers,
        LineStyler.HasLineStyler,
        UndoableEditor {

  /**
   * Returns the contents of the editor.
   *
   * @return
   */
  String getValue();

  /**
   * Sets the content of the editor.<br>
   * The operation <em>must</em> send a {@link DocumentChangedEvent} on the document private event
   * bus.
   *
   * @param newValue the new contents
   * @param initializationHandler must be called when content injected in the Editor Widget
   */
  void setValue(String newValue, ContentInitializedHandler initializationHandler);

  /** Sets whether the annotation ruler is visible. */
  void setAnnotationRulerVisible(boolean show);

  /** Sets whether the folding ruler is visible. */
  void setFoldingRulerVisible(boolean show);

  /** Sets whether the zoom ruler is visible. */
  void setZoomRulerVisible(boolean show);

  /** Sets whether the overview ruler is visible. */
  void setOverviewRulerVisible(boolean show);

  /**
   * Returns the dirty state of the editor.
   *
   * @return true iff the editor is dirty (i.e. unsaved change were made)
   */
  boolean isDirty();

  /** Marks the editor as clean i.e change the dirty state to false. */
  void markClean();

  /**
   * The instance of {@link org.eclipse.che.ide.api.editor.document.Document}.
   *
   * @return the embedded document
   */
  Document getDocument();

  /** Give the focus to the editor. */
  void setFocus();

  /**
   * Selects the given range in the editor.
   *
   * @param selection the new selection
   * @param show whether the editor should be scrolled to show the range
   */
  void setSelectedRange(Region selection, boolean show);

  /**
   * Returns a position converter relative to this editor (pixel coordinates <-> line char
   * positions).
   *
   * @return a position converter
   */
  PositionConverter getPositionConverter();

  /** Refresh the editor widget. */
  void refresh();


  /** Callback that should be called when editor widget implementation is fully initialized. */
  interface WidgetInitializedCallback {
    void initialized(EditorWidget editorWidget);
  }
}
