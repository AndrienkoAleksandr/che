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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.editor.AbstractEditorPresenter;
import org.eclipse.che.ide.api.editor.EditorAgent.OpenEditorCallback;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.document.DocumentStorage;
import org.eclipse.che.ide.api.editor.editorconfig.TextEditorConfiguration;
import org.eclipse.che.ide.api.editor.texteditor.EditorWidget;
import org.eclipse.che.ide.api.editor.texteditor.EditorWidgetFactory;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.editor.texteditor.TextEditorPartView;
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
  private final EditorWidgetFactory<OrionEditorWidget> editorWidgetFactory;
  private final EditorInitializePromiseHolder editorModule;
  private final TextEditorPartView editorView;

  private OrionEditorWidget editorWidget;

  private boolean delayedFocus;

  @Inject
  public OrionEditorPresenter(
      final DocumentStorage documentStorage,
      final EditorWidgetFactory<OrionEditorWidget> editorWigetFactory,
      final EditorInitializePromiseHolder editorModule,
      final TextEditorPartView editorView) {
    this.documentStorage = documentStorage;
    this.editorWidgetFactory = editorWigetFactory;
    this.editorModule = editorModule;
    this.editorView = editorView;

    this.editorView.setDelegate(this);
  }

  @Override
  protected void initializeEditor(final OpenEditorCallback callback) {

    Promise<Void> initializerPromise = editorModule.getInitializerPromise();
    initializerPromise
        .catchError(
            arg -> {
              GWT.log("initialization failed 0");
              callback.onInitializationFailed();
            })
        .thenPromise(arg -> documentStorage.getDocument(input.getFile()))
        .then(
            content -> {
              createEditor(content, callback);
            })
        .catchError(
            arg -> {
              GWT.log("initialization failed 1");
              callback.onInitializationFailed();
            });
  }

  private void createEditor(final String content, OpenEditorCallback openEditorCallback) {
    editorWidgetFactory.createEditorWidget(new EditorWidgetInitializedCallback(content, openEditorCallback));
  }

  @Override
  public void updateDirtyState(boolean dirty) {}

  @Override
  public void storeState() {
  }

  @Override
  public void restoreState() {
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
  public SVGResource getTitleImage() {
    return input.getSVGResource();
  }

  @Override
  public String getTitle() {
    return input.getFile().getDisplayName();
  }

  @Override
  public void doSave() {
    doSave(
        new AsyncCallback<EditorInput>() {
          @Override
          public void onSuccess(final EditorInput result) {
          }

          @Override
          public void onFailure(final Throwable caught) {
          }
        });
  }

  @Override
  public void doSave(final AsyncCallback<EditorInput> callback) {}

  @Override
  public String getContentType() {
    return DEFAULT_CONTENT_TYPE;
  }

  @Override
  public void onResize() {
    if (this.editorWidget != null) {
      this.editorWidget.onResize();
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
