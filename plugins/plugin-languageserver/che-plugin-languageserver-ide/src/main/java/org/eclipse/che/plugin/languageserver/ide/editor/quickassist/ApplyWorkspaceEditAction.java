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
package org.eclipse.che.plugin.languageserver.ide.editor.quickassist;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.eclipse.che.api.languageserver.shared.dto.DtoClientImpls.FileEditParamsDto;
import org.eclipse.che.api.languageserver.shared.model.FileEditParams;
import org.eclipse.che.api.languageserver.shared.util.RangeComparator;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.api.promises.client.js.Executor;
import org.eclipse.che.api.promises.client.js.RejectFunction;
import org.eclipse.che.api.promises.client.js.ResolveFunction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.BaseAction;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode;
import org.eclipse.che.ide.api.notification.StatusNotification.Status;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.plugin.languageserver.ide.LanguageServerLocalization;
import org.eclipse.che.plugin.languageserver.ide.service.WorkspaceServiceClient;
import org.eclipse.che.plugin.languageserver.ide.util.PromiseHelper;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;

@Singleton
public class ApplyWorkspaceEditAction extends BaseAction {
  private static final Comparator<TextEdit> COMPARATOR =
      RangeComparator.transform(new RangeComparator().reversed(), TextEdit::getRange);

  private EditorAgent editorAgent;
  private DtoFactory dtoFactory;
  private WorkspaceServiceClient workspaceService;
  private PromiseHelper promiseHelper;
  private LanguageServerLocalization localization;
  private NotificationManager notificationManager;
  private PromiseProvider promiseProvider;

  @Inject
  public ApplyWorkspaceEditAction(
      EditorAgent editorAgent,
      DtoFactory dtoFactory,
      WorkspaceServiceClient workspaceService,
      PromiseHelper promiseHelper,
      LanguageServerLocalization localization,
      NotificationManager notificationManager,
      PromiseProvider promiseProvider) {
    this.editorAgent = editorAgent;
    this.dtoFactory = dtoFactory;
    this.workspaceService = workspaceService;
    this.promiseHelper = promiseHelper;
    this.localization = localization;
    this.notificationManager = notificationManager;
    this.promiseProvider = promiseProvider;
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    QuickassistActionEvent qaEvent = (QuickassistActionEvent) evt;
    List<Object> arguments = qaEvent.getArguments();
    WorkspaceEdit edit =
        dtoFactory.createDtoFromJson(arguments.get(0).toString(), WorkspaceEdit.class);
    applyWorkspaceEdit(edit);
  }

  public void applyWorkspaceEdit(WorkspaceEdit edit) {
  }

  public static void applyTextEdits(Document document, List<TextEdit> edits) {
    edits
        .stream()
        .sorted(COMPARATOR)
        .forEach(
            e -> {
              Range r = e.getRange();
              Position start = r.getStart();
              Position end = r.getEnd();
              document.replace(
                  start.getLine(),
                  start.getCharacter(),
                  end.getLine(),
                  end.getCharacter(),
                  e.getNewText());
            });
  }
}
