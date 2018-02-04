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

import static java.lang.Boolean.parseBoolean;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.actions.LinkWithEditorAction;
import org.eclipse.che.ide.api.editor.AbstractEditorPresenter;
import org.eclipse.che.ide.api.editor.EditorAgent.OpenEditorCallback;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorLocalizationConstants;
import org.eclipse.che.ide.api.editor.document.DocumentStorage;
import org.eclipse.che.ide.api.editor.editorconfig.TextEditorConfiguration;
import org.eclipse.che.ide.api.editor.filetype.FileTypeIdentifier;
import org.eclipse.che.ide.api.editor.texteditor.EditorWidget;
import org.eclipse.che.ide.api.editor.texteditor.EditorWidgetFactory;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.editor.texteditor.TextEditorPartView;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.editor.EditorFileStatusNotificationOperation;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * {@link TextEditor} using orion. This class is only defined to allow the Gin binding to be
 * performed.
 */
public class OrionEditorPresenter extends AbstractEditorPresenter
    implements TextEditor, TextEditorPartView.Delegate {
  /** File type used when we have no idea of the actual content type. */
  public static final String DEFAULT_CONTENT_TYPE = "text/plain";

  private final DocumentStorage documentStorage;
  private final EditorLocalizationConstants constant;
  private final EditorWidgetFactory<OrionEditorWidget> editorWidgetFactory;
  private final EditorInitializePromiseHolder editorModule;
  private final TextEditorPartView editorView;
  private final FileTypeIdentifier fileTypeIdentifier;
  private final EditorFileStatusNotificationOperation editorFileStatusNotificationOperation;

  private TextEditorConfiguration configuration;
  private OrionEditorWidget editorWidget;
//  private Document document;

  private boolean delayedFocus;
  private List<String> fileTypes;

  @Inject
  public OrionEditorPresenter(
      final DocumentStorage documentStorage,
      final EditorLocalizationConstants constant,
      final EditorWidgetFactory<OrionEditorWidget> editorWigetFactory,
      final EditorInitializePromiseHolder editorModule,
      final TextEditorPartView editorView,
      final FileTypeIdentifier fileTypeIdentifier,
      final EditorFileStatusNotificationOperation editorFileStatusNotificationOperation) {
    this.documentStorage = documentStorage;
    this.constant = constant;
    this.editorWidgetFactory = editorWigetFactory;
    this.editorModule = editorModule;
    this.editorView = editorView;
    this.fileTypeIdentifier = fileTypeIdentifier;
    this.editorFileStatusNotificationOperation = editorFileStatusNotificationOperation;

    this.editorView.setDelegate(this);
  }

  @Override
  protected void initializeEditor(final OpenEditorCallback callback) {

    Promise<Void> initializerPromise = editorModule.getInitializerPromise();
    initializerPromise
        .catchError(
            arg -> {
              displayErrorPanel(constant.editorInitErrorMessage());
              callback.onInitializationFailed();
            })
        .thenPromise(arg -> documentStorage.getDocument(input.getFile()))
        .then(
            content -> {
              createEditor(content, callback);
            })
        .catchError(
            arg -> {
              displayErrorPanel(constant.editorFileErrorMessage());
              callback.onInitializationFailed();
            });
  }

  private void createEditor(final String content, OpenEditorCallback openEditorCallback) {
    this.fileTypes = detectFileType(getEditorInput().getFile());
    editorWidgetFactory.createEditorWidget(
        fileTypes, new EditorWidgetInitializedCallback(content, openEditorCallback));
  }

  @Override
  public void updateDirtyState(boolean dirty) {}

  private void displayErrorPanel(final String message) {
    this.editorView.showPlaceHolder(new Label(message));
  }

  @Override
  public void storeState() {
  }

  @Override
  public void restoreState() {
  }

  @Override
  public void close(boolean save) {
  }

  protected Widget getWidget() {
    return this.editorView.asWidget();
  }

  @Override
  public void go(AcceptsOneWidget container) {
    container.setWidget(getWidget());
  }

  @Override
  public String getTitleToolTip() {
    return null;
  }

  @Override
  public void onClosing(@NotNull final AsyncCallback<Void> callback) {}

  @Override
  public TextEditorPartView getView() {
    return this.editorView;
  }

  @Override
  public void activate() {
    if (editorWidget != null) {
      Scheduler.get()
          .scheduleDeferred(
              () -> {
                editorWidget.refresh();
                editorWidget.setFocus();
              });
    } else {
      this.delayedFocus = true;
    }
  }

  @Override
  public void initialize(@NotNull TextEditorConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public TextEditorConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public SVGResource getTitleImage() {
    return input.getSVGResource();
  }

  @Override
  public String getTitle() {
    return input.getFile().getDisplayName();
  }

  @Override
  public void doSave() {
    editorFileStatusNotificationOperation.suspend();
    doSave(
        new AsyncCallback<EditorInput>() {
          @Override
          public void onSuccess(final EditorInput result) {
            editorFileStatusNotificationOperation.resume();
          }

          @Override
          public void onFailure(final Throwable caught) {
            editorFileStatusNotificationOperation.resume();
          }
        });
  }

  @Override
  public void doSave(final AsyncCallback<EditorInput> callback) {}

  @Override
  public void doSaveAs() {}

  @Override
  public String getContentType() {
    // Before the editor content is ready, the content type is not defined
    if (this.fileTypes == null || this.fileTypes.isEmpty()) {
      return null;
    } else {
      return this.fileTypes.get(0);
    }
  }

  private List<String> detectFileType(final VirtualFile file) {
    final List<String> result = new ArrayList<>();
    if (file != null) {
      // use the identification patterns
      final List<String> types = this.fileTypeIdentifier.identifyType(file);
      if (types != null && !types.isEmpty()) {
        result.addAll(types);
      }
    }

    // ultimate fallback - can't make more generic for text
    result.add(DEFAULT_CONTENT_TYPE);

    return result;
  }

  @Override
  public void onResize() {
    if (this.editorWidget != null) {
      this.editorWidget.onResize();
    }
  }

  @Override
  public EditorWidget getEditorWidget() {
    return this.editorWidget;
  }

  /** {@inheritDoc} */
  @Override
  public void setFocus() {
    EditorWidget editorWidget = getEditorWidget();
    if (editorWidget != null) {
      OrionEditorWidget orion = ((OrionEditorWidget) editorWidget);
      orion.setFocus();
    }
  }

  private class EditorWidgetInitializedCallback implements EditorWidget.WidgetInitializedCallback {
    private String content;
    private boolean isInitialized;
    private OpenEditorCallback openEditorCallback;

    private EditorWidgetInitializedCallback(String content, OpenEditorCallback openEditorCallback) {
      this.content = content;
      this.openEditorCallback = openEditorCallback;
    }

    @Override
    public void initialized(EditorWidget widget) {
      Log.info(getClass(), "content ^)---------" + content + "");
      editorWidget = (OrionEditorWidget) widget;
      editorView.setEditorWidget(editorWidget);

      // TODO: delayed activation
      // handle delayed focus (initialization editor widget)
      // should also check if I am visible, but how ?
      if (delayedFocus) {
        editorWidget.refresh();
        editorWidget.setFocus();
        delayedFocus = false;
      }

      editorWidget.setValue(
          content,
          () -> {
            if (isInitialized) {
              return;
            }

            firePropertyChange(PROP_INPUT);

            isInitialized = true;
            openEditorCallback.onEditorOpened(OrionEditorPresenter.this);
          });
    }
  }
}
