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
import org.eclipse.che.ide.api.editor.events.GutterClickEvent;
import org.eclipse.che.ide.api.editor.events.GutterClickHandler;
import org.eclipse.che.ide.api.editor.events.HasCursorActivityHandlers;
import org.eclipse.che.ide.api.editor.gutter.Gutter;
import org.eclipse.che.ide.api.editor.gutter.Gutters;
import org.eclipse.che.ide.api.editor.gutter.HasGutter;
import org.eclipse.che.ide.api.editor.link.LinkedMode;
import org.eclipse.che.ide.api.editor.position.PositionConverter;
import org.eclipse.che.ide.api.editor.text.Region;
import org.eclipse.che.ide.api.editor.texteditor.ContentInitializedHandler;
import org.eclipse.che.ide.api.editor.texteditor.EditorWidget;
import org.eclipse.che.ide.api.editor.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.editor.texteditor.LineStyler;
import org.eclipse.che.ide.api.selection.SelectionChangedEvent;
import org.eclipse.che.ide.api.selection.SelectionChangedHandler;
import org.eclipse.che.ide.editor.orion.client.jso.OrionAnnotationModelOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionCodeEditWidgetOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionContentAssistOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorOptionsOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorViewOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEventTargetOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionExtRulerOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionInputChangedEventOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionKeyModeOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionRulerClickEventOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionTextViewOverlay;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.requirejs.ModuleHolder;

/**
 * Orion implementation for {@link EditorWidget}.
 *
 * @author "Mickaël Leduque"
 */
public class OrionEditorWidget extends Composite
    implements EditorWidget, HasChangeHandlers, HasCursorActivityHandlers, HasGutter {

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
  private OrionExtRulerOverlay orionLineNumberRuler;
  /** Component that handles undo/redo. */
  private HandlesUndoRedo undoRedo;

  private OrionDocument embeddedDocument;
  private Gutter gutter;

  private boolean changeHandlerAdded = false;
  private boolean focusHandlerAdded = false;
  private boolean blurHandlerAdded = false;
  private boolean cursorHandlerAdded = false;
  private boolean gutterClickHandlerAdded = false;

  /** Component that handles line styling. */
  private LineStyler lineStyler;

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

  private Gutter initBreakpointRuler(ModuleHolder moduleHolder) {
    JavaScriptObject orionEventTargetModule = moduleHolder.getModule("OrionEventTarget");

    orionLineNumberRuler = editorOverlay.getTextView().getRulers()[1];
    orionLineNumberRuler.overrideOnClickEvent();
    OrionEventTargetOverlay.addMixin(orionEventTargetModule, orionLineNumberRuler);

    return new OrionBreakpointRuler(orionLineNumberRuler, editorOverlay);
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
  public void markClean() {
    this.editorOverlay.setDirty(false);
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
  public PositionConverter getPositionConverter() {
    return embeddedDocument.getPositionConverter();
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
  public HandlesUndoRedo getUndoRedo() {
    return this.undoRedo;
  }

  @Override
  public LineStyler getLineStyler() {
    return lineStyler;
  }

  @Override
  public HandlerRegistration addGutterClickHandler(final GutterClickHandler handler) {
    if (!gutterClickHandlerAdded) {
      gutterClickHandlerAdded = true;
      orionLineNumberRuler.addEventListener(
          OrionEventConstants.RULER_CLICK_EVENT,
          new OrionExtRulerOverlay.EventHandler<OrionRulerClickEventOverlay>() {
            @Override
            public void onEvent(OrionRulerClickEventOverlay parameter) {
              final int lineIndex = parameter.getLineIndex();
              fireGutterClickEvent(lineIndex);
            }
          },
          false);
    }
    return addHandler(handler, GutterClickEvent.TYPE);
  }

  private void fireGutterClickEvent(final int line) {
    final GutterClickEvent gutterEvent =
        new GutterClickEvent(line, Gutters.BREAKPOINTS_GUTTER, null);
    fireEvent(gutterEvent);
    this.embeddedDocument.getDocEventBus().fireEvent(gutterEvent);
  }

  @Override
  public void refresh() {
    this.editorOverlay.getTextView().redraw();
  }

  public OrionTextViewOverlay getTextView() {
    return editorOverlay.getTextView();
  }

  public LinkedMode getLinkedMode() {
    return editorOverlay.getLinkedMode(editorOverlay.getAnnotationModel());
  }

  public OrionAnnotationModelOverlay getAnnotationModel() {
    return editorOverlay.getAnnotationModel();
  }

  /** Returns {@link OrionEditorOverlay}. */
  public OrionEditorOverlay getEditor() {
    return editorOverlay;
  }

  @Override
  public Gutter getGutter() {
    return gutter;
  }

  public int getTopVisibleLine() {
    return editorOverlay.getTextView().getTopIndex();
  }

  public void setTopLine(int line) {
    editorOverlay.getTextView().setTopIndex(line);
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

      lineStyler = new OrionLineStyler(editorOverlay);

      final OrionTextViewOverlay textView = editorOverlay.getTextView();
      keyModeInstances.add(
          VI, OrionKeyModeOverlay.getViKeyMode(moduleHolder.getModule("OrionVi"), textView));
      keyModeInstances.add(
          EMACS,
          OrionKeyModeOverlay.getEmacsKeyMode(moduleHolder.getModule("OrionEmacs"), textView));

      undoRedo = new OrionUndoRedo(editorOverlay.getUndoStack());
      editorOverlay.setZoomRulerVisible(true);
      editorOverlay.getAnnotationStyler().addAnnotationType("che-marker", 100);
      //
      gutter = initBreakpointRuler(moduleHolder);

      orionSettingsController.updateSettings();
      widgetInitializedCallback.initialized(OrionEditorWidget.this);
    }
  }

  /**
   * Registers global prompt function to be accessible directly from JavaScript.
   *
   * <p>Function promptIDE(title, text, defaultValue, callback) title Dialog title text The text to
   * display in the dialog box defaultValue The default value callback function(value) clicking "OK"
   * will return input value clicking "Cancel" will return null
   */
  //  private native void registerPromptFunction() /*-{
  //        if (!$wnd["promptIDE"]) {
  //            var instance = this;
  //            $wnd["promptIDE"] = function (title, text, defaultValue, callback) {
  //
  // instance.@org.eclipse.che.ide.editor.orion.client.OrionEditorWidget::askLineNumber(*)(title,
  // text, defaultValue, callback);
  //            };
  //        }
  //    }-*/;

  /** Custom callback to pass given value to native javascript function. */
  //  private class InputCallback implements org.eclipse.che.ide.ui.dialogs.input.InputCallback {
  //
  //    private JavaScriptObject callback;
  //
  //    public InputCallback(JavaScriptObject callback) {
  //      this.callback = callback;
  //    }
  //
  //    @Override
  //    public void accepted(String value) {
  //      acceptedNative(value);
  //      editorAgent.activateEditor(editorAgent.getActiveEditor());
  //    }
  //
  //    private native void acceptedNative(String value) /*-{
  //            var callback =
  // this.@org.eclipse.che.ide.editor.orion.client.OrionEditorWidget.InputCallback::callback;
  //            callback(value);
  //        }-*/;
  //  }
  //
  //  private void askLineNumber(
  //      String title, String text, String defaultValue, final JavaScriptObject callback) {
  //    if (defaultValue == null) {
  //      defaultValue = "";
  //    } else {
  //      // It's strange situation defaultValue.length() returns 'undefined' but must return a
  // number.
  //      // Reinitialise the variable resolves the problem.
  //      defaultValue = "" + defaultValue;
  //    }
  //
  //    dialogFactory
  //        .createInputDialog(
  //            title, text, defaultValue, 0, defaultValue.length(), new InputCallback(callback),
  // null)
  //        .show();
  //  }
}
