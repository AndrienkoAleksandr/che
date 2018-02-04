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

import static org.eclipse.che.ide.editor.orion.client.KeyMode.EMACS;
import static org.eclipse.che.ide.editor.orion.client.KeyMode.VI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.web.bindery.event.shared.EventBus;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.editor.events.CursorActivityEvent;
import org.eclipse.che.ide.api.editor.events.CursorActivityHandler;
import org.eclipse.che.ide.api.editor.events.HasCursorActivityHandlers;
import org.eclipse.che.ide.api.editor.text.Region;
import org.eclipse.che.ide.api.editor.texteditor.ContentInitializedHandler;
import org.eclipse.che.ide.api.editor.texteditor.EditorWidget;
import org.eclipse.che.ide.api.selection.SelectionChangedEvent;
import org.eclipse.che.ide.api.selection.SelectionChangedHandler;
import org.eclipse.che.ide.editor.orion.client.jso.OrionAnnotationModelOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionCodeEditWidgetOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionContentAssistOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorOptionsOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorViewOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionInputChangedEventOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionKeyModeOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionTextViewOverlay;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.requirejs.ModuleHolder;

/**
 * Orion implementation for {@link EditorWidget}.
 *
 * @author "Mickaël Leduque"
 */
public class OrionEditorWidget extends Composite
    implements EditorWidget, HasChangeHandlers, HasCursorActivityHandlers {

  /** The UI binder instance. */
  private static final OrionEditorWidgetUiBinder UIBINDER =
      GWT.create(OrionEditorWidgetUiBinder.class);

  /** The logger. */
  private static final Logger LOG = Logger.getLogger(OrionEditorWidget.class.getSimpleName());

  private final ModuleHolder moduleHolder;
  private final EventBus eventBus;
  private final KeyModeInstances keyModeInstances;
  private final OrionSettingsController orionSettingsController;

  @UiField SimplePanel panel;
  /** The instance of the orion editor native element style. */
  @UiField EditorElementStyle editorElementStyle;

  private OrionEditorViewOverlay editorViewOverlay;
  private OrionEditorOverlay editorOverlay;
  private String modeName;
  /** Component that handles undo/redo. */

  private OrionDocument embeddedDocument;
//  private Gutter gutter;

  private boolean changeHandlerAdded = false;
  private boolean focusHandlerAdded = false;
  private boolean blurHandlerAdded = false;
  private boolean cursorHandlerAdded = false;

  @AssistedInject
  public OrionEditorWidget(
      final ModuleHolder moduleHolder,
      final KeyModeInstances keyModeInstances,
      final EventBus eventBus,
      final Provider<OrionCodeEditWidgetOverlay> orionCodeEditWidgetProvider,
      @Assisted final List<String> editorModes,
      @Assisted final WidgetInitializedCallback widgetInitializedCallback,
      final Provider<OrionEditorOptionsOverlay> editorOptionsProvider,
      final OrionSettingsController orionSettingsController) {

    this.moduleHolder = moduleHolder;
    this.keyModeInstances = keyModeInstances;
    this.eventBus = eventBus;

    this.orionSettingsController = orionSettingsController;
    initWidget(UIBINDER.createAndBindUi(this));

    panel.getElement().setId("orion-parent-" + Document.get().createUniqueId());
    panel.getElement().addClassName(this.editorElementStyle.editorParent());

    // todo
    orionCodeEditWidgetProvider
        .get()
        .createEditorView(panel.getElement(), editorOptionsProvider.get())
        .then(new EditorViewCreatedOperation(widgetInitializedCallback));
  }

  @Override
  public String getValue() {
    return editorOverlay.getText();
  }

  @Override
  public void setValue(String newValue, final ContentInitializedHandler initializationHandler) {
    editorOverlay.addEventListener(
        OrionInputChangedEventOverlay.TYPE,
        (OrionEditorOverlay.EventHandler<OrionInputChangedEventOverlay>)
            event -> {
              Log.info(getClass(), "INIT HANDLER!!!!!");
              if (initializationHandler != null) {
                initializationHandler.onContentInitialized();
              }
            },
        true);

    this.editorViewOverlay.setContents(newValue, modeName);
  }

  @Override
  public void setAnnotationRulerVisible(boolean show) {
    editorOverlay.setAnnotationRulerVisible(show);
  }

  @Override
  public void setFoldingRulerVisible(boolean show) {
    editorOverlay.setFoldingRulerVisible(show);
  }

  @Override
  public void setZoomRulerVisible(boolean show) {
    editorOverlay.setZoomRulerVisible(show);
  }

  @Override
  public void setOverviewRulerVisible(boolean show) {
    editorOverlay.setOverviewRulerVisible(show);
  }

  @Override
  public boolean isDirty() {
    return this.editorOverlay.isDirty();
  }

  @Override
  public org.eclipse.che.ide.api.editor.document.Document getDocument() {
    if (this.embeddedDocument == null) {
      this.embeddedDocument =
          new OrionDocument(this.editorOverlay.getTextView(), this, editorOverlay);
    }
    return this.embeddedDocument;
  }

  @Override
  public void setSelectedRange(final Region selection, final boolean show) {
    //    this.editorOverlay.setSelection(selection.getOffset(), selection.getLength(), show);
  }

  @Override
  public HandlerRegistration addChangeHandler(final ChangeHandler handler) {
    if (!changeHandlerAdded) {
      changeHandlerAdded = true;
      final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
      textView.addEventListener(
          OrionEventConstants.MODEL_CHANGED_EVENT,
          new OrionTextViewOverlay.EventHandlerNoParameter() {

            @Override
            public void onEvent() {
              fireChangeEvent();
            }
          });
    }
    return addHandler(handler, ChangeEvent.getType());
  }

  private void fireChangeEvent() {
    DomEvent.fireNativeEvent(Document.get().createChangeEvent(), this);
  }

  @Override
  public HandlerRegistration addCursorActivityHandler(CursorActivityHandler handler) {
    if (!cursorHandlerAdded) {
      cursorHandlerAdded = true;
      final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
      textView.addEventListener(
          OrionEventConstants.SELECTION_EVENT,
          new OrionTextViewOverlay.EventHandlerNoParameter() {

            @Override
            public void onEvent() {
              fireCursorActivityEvent();
            }
          });
    }
    return addHandler(handler, CursorActivityEvent.TYPE);
  }

  private void fireCursorActivityEvent() {
    fireEvent(new CursorActivityEvent());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    if (!focusHandlerAdded) {
      focusHandlerAdded = true;
      final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
      textView.addEventListener(
          OrionEventConstants.FOCUS_EVENT,
          new OrionTextViewOverlay.EventHandlerNoParameter() {

            @Override
            public void onEvent() {
              fireFocusEvent();
            }
          });
    }
    return addHandler(handler, FocusEvent.getType());
  }

  private void fireFocusEvent() {
    DomEvent.fireNativeEvent(Document.get().createFocusEvent(), this);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    if (!blurHandlerAdded) {
      blurHandlerAdded = true;
      final OrionTextViewOverlay textView = this.editorOverlay.getTextView();
      textView.addEventListener(
          OrionEventConstants.BLUR_EVENT,
          new OrionTextViewOverlay.EventHandlerNoParameter() {

            @Override
            public void onEvent() {
              fireBlurEvent();
            }
          });
    }
    return addHandler(handler, BlurEvent.getType());
  }

  private void fireBlurEvent() {
    DomEvent.fireNativeEvent(Document.get().createBlurEvent(), this);
  }

  @Override
  public void setFocus() {
    this.editorOverlay.focus();
  }

  @Override
  protected void onLoad() {
    // fix for native editor height
    if (panel.getElement().getChildCount() > 0) {
      final Element child = panel.getElement().getFirstChildElement();
      child.setId("orion-editor-" + Document.get().createUniqueId());
      child.getStyle().clearHeight();

    } else {
      LOG.severe("Orion insertion failed.");
    }
  }

  @Override
  public void onResize() {
    // redraw text and rulers
    // maybe just redrawing the text would be enough
    this.editorOverlay.getTextView().redraw();
  }


  @Override
  public void refresh() {
    this.editorOverlay.getTextView().redraw();
  }

  public OrionTextViewOverlay getTextView() {
    return editorOverlay.getTextView();
  }

  public OrionAnnotationModelOverlay getAnnotationModel() {
    return editorOverlay.getAnnotationModel();
  }

  /** Returns {@link OrionEditorOverlay}. */
  public OrionEditorOverlay getEditor() {
    return editorOverlay;
  }

  /**
   * UI binder interface for this component.
   *
   * @author "Mickaël Leduque"
   */
  interface OrionEditorWidgetUiBinder extends UiBinder<SimplePanel, OrionEditorWidget> {}

  /**
   * CSS style for the orion native editor element.
   *
   * @author "Mickaël Leduque"
   */
  public interface EditorElementStyle extends CssResource {

    @ClassName("editor-parent")
    String editorParent();
  }

  private class EditorViewCreatedOperation implements Operation<OrionEditorViewOverlay> {
    private final WidgetInitializedCallback widgetInitializedCallback;

    private EditorViewCreatedOperation(WidgetInitializedCallback widgetInitializedCallback) {
      this.widgetInitializedCallback = widgetInitializedCallback;
    }

    @Override
    public void apply(OrionEditorViewOverlay arg) throws OperationException {
      editorViewOverlay = arg;
      editorOverlay = arg.getEditor();
      orionSettingsController.setEditorViewOverlay(arg);

      final OrionContentAssistOverlay contentAssist = editorOverlay.getContentAssist();
      eventBus.addHandler(
          SelectionChangedEvent.TYPE,
          new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent event) {
              if (contentAssist.isActive()) {
                contentAssist.deactivate();
              }
            }
          });

      final OrionTextViewOverlay textView = editorOverlay.getTextView();
      keyModeInstances.add(
          VI, OrionKeyModeOverlay.getViKeyMode(moduleHolder.getModule("OrionVi"), textView));
      keyModeInstances.add(
          EMACS,
          OrionKeyModeOverlay.getEmacsKeyMode(moduleHolder.getModule("OrionEmacs"), textView));

      editorOverlay.setZoomRulerVisible(true);
      editorOverlay.getAnnotationStyler().addAnnotationType("che-marker", 100);

      orionSettingsController.updateSettings();
      widgetInitializedCallback.initialized(OrionEditorWidget.this);
    }
  }
}
