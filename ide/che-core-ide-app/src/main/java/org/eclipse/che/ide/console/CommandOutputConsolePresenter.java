/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.console;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.che.api.workspace.shared.Constants.COMMAND_PREVIEW_URL_ATTRIBUTE_NAME;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.command.CommandExecutor;
import org.eclipse.che.ide.api.command.CommandImpl;
import org.eclipse.che.ide.api.command.exec.ExecAgentCommandManager;
import org.eclipse.che.ide.api.command.exec.ProcessFinishedEvent;
import org.eclipse.che.ide.api.command.exec.ProcessStartedEvent;
import org.eclipse.che.ide.api.command.exec.dto.ProcessSubscribeResponseDto;
import org.eclipse.che.ide.api.command.exec.dto.event.ProcessDiedEventDto;
import org.eclipse.che.ide.api.command.exec.dto.event.ProcessStartedEventDto;
import org.eclipse.che.ide.api.command.exec.dto.event.ProcessStdErrEventDto;
import org.eclipse.che.ide.api.command.exec.dto.event.ProcessStdOutEventDto;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.macro.MacroProcessor;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Console for command output.
 *
 * @author Artem Zatsarynnyi
 */
public class CommandOutputConsolePresenter
    implements CommandOutputConsole, OutputConsoleView.ActionDelegate {

  private final OutputConsoleView view;
  private final MachineResources resources;
  private final CommandImpl command;
  private final EventBus eventBus;
  private final String machineName;
  private final CommandExecutor commandExecutor;
  private final ExecAgentCommandManager execAgentCommandManager;

  private int pid;
  private boolean finished;

  /** Wrap text or not */
  private boolean wrapText = false;

  /** Follow output when printing text */
  private boolean followOutput = true;

  private final List<ActionDelegate> actionDelegates = new ArrayList<>();

  private OutputCustomizer outputCustomizer = null;

  @Inject
  public CommandOutputConsolePresenter(
      final OutputConsoleView view,
      MachineResources resources,
      CommandExecutor commandExecutor,
      MacroProcessor macroProcessor,
      EventBus eventBus,
      ExecAgentCommandManager execAgentCommandManager,
      @Assisted CommandImpl command,
      @Assisted String machineName,
      AppContext appContext,
      EditorAgent editorAgent) {
    this.view = view;
    this.resources = resources;
    this.execAgentCommandManager = execAgentCommandManager;
    this.command = command;
    this.machineName = machineName;
    this.eventBus = eventBus;
    this.commandExecutor = commandExecutor;

    setCustomizer(
        new CompoundOutputCustomizer(
            new JavaOutputCustomizer(appContext, editorAgent),
            new CSharpOutputCustomizer(appContext, editorAgent),
            new CPPOutputCustomizer(appContext, editorAgent)));

    view.setDelegate(this);

    final String previewUrl = command.getAttributes().get(COMMAND_PREVIEW_URL_ATTRIBUTE_NAME);
    if (!isNullOrEmpty(previewUrl)) {
      macroProcessor
          .expandMacros(previewUrl)
          .then(
              new Operation<String>() {
                @Override
                public void apply(String arg) throws OperationException {
                  view.showPreviewUrl(arg);
                }
              });
    } else {
      view.hidePreview();
    }

    view.showCommandLine(command.getCommandLine());
  }

  @Override
  public void go(AcceptsOneWidget container) {
    container.setWidget(view);
  }

  @Override
  public CommandImpl getCommand() {
    return command;
  }

  @Nullable
  @Override
  public int getPid() {
    return pid;
  }

  @Override
  public String getTitle() {
    return command.getName();
  }

  @Override
  public SVGResource getTitleIcon() {
    return resources.output();
  }

  @Override
  public void listenToOutput(String wsChannel) {}

  @Override
  public Consumer<ProcessStdErrEventDto> getStdErrConsumer() {
    return event -> {
      String text = event.getText();
      // todo handle error consumer too.
      boolean carriageReturn = text.endsWith("\r");
      String color = "red";
      view.print(text, carriageReturn, color, false);

      for (ActionDelegate actionDelegate : actionDelegates) {
        actionDelegate.onConsoleOutput(CommandOutputConsolePresenter.this);
      }
    };
  }

  public static final long LINES_TO_LOAD = 20;
  private static final char AMOUNT_SCROLL_LINES = 3;
  private long AMOUNT_SAVED_LINES = 50;

  private long currentOffset;
  private long totalLineNum;
  private long savedLineNum;

  @Override
  public void onLoadNextLogsPortion() {
    int skip =
        Math.max((int) (totalLineNum - currentOffset - AMOUNT_SAVED_LINES - LINES_TO_LOAD), 0);

    Log.info(
        getClass(), "Total line number: " + totalLineNum + " current offset: " + currentOffset);
    if (currentOffset + AMOUNT_SAVED_LINES > totalLineNum) {
      return;
    }

    long diff = totalLineNum - currentOffset - AMOUNT_SAVED_LINES;
    long limit = diff < LINES_TO_LOAD ? diff : LINES_TO_LOAD;
    // Log.info(getClass(), "Limit to load = " + limit + " skip " + skip);
    if (limit == 0) {
      return;
    }

    execAgentCommandManager
        .getProcessLogs(machineName, pid, null, null, (int) limit, skip)
        .onSuccess(
            consumer -> {
              currentOffset += consumer.size();
              //              Log.info(getClass(), "offset " + currentOffset);
              view.clearLines(consumer.size(), 0);

              //              if (currentOffset + AMOUNT_SAVED_LINES >= totalLineNum) {
              //                view.hideNextOutPutPartLink();
              //              }
              //              if (currentOffset > 0) {
              //                view.displayPreviousOutPutLink();
              //              }

              //              consumer.forEach(responseDto -> Log.info(getClass(),
              // responseDto.getText()));
              consumer.forEach(responseDto -> print(responseDto.getText()));
              Log.info(getClass(), "savedLineNumber: " + savedLineNum);
              view.setScrollPosition((int) LINES_TO_LOAD - 1);
            })
        .onFailure(
            err -> Log.error(getClass(), "Log pagination next failed. Cause" + err.getMessage()));
  }

  @Override
  public void onLoadPreviousPortion() {
    if (currentOffset == 0) {
      return;
    }

    int skip = (int) (totalLineNum - currentOffset);
    // Log.info(getClass(), "Total line number: " + totalLineNum + " current offset: " +
    // currentOffset);
    // Log.info(getClass(), " Offset = " + currentOffset);
    // Log.info(getClass(), "skip = totalLineNum - currentOffset = " + skip);

    execAgentCommandManager
        .getProcessLogs(machineName, pid, null, null, (int) LINES_TO_LOAD, skip)
        .onSuccess(
            consumer -> {
              view.clearLines(consumer.size(), (int) (savedLineNum - consumer.size()));
              //              consumer.forEach(responseDto -> Log.info(getClass(),
              // responseDto.getText()));
              currentOffset -= consumer.size();
              Lists.reverse(consumer).forEach(responseDto -> printOnTop(responseDto.getText()));

              //              if (currentOffset == 0) {
              //                view.hidePreviousOutPutLink();
              //              }
              // view.scrollToBottom();
              //              if (currentOffset + AMOUNT_SAVED_LINES != totalLineNum) {
              //                view.displayNextOutPutPartLink();
              //              }

              view.setScrollPosition((int) (LINES_TO_LOAD - AMOUNT_SCROLL_LINES));
            })
        .onFailure(
            err ->
                Log.error(getClass(), "Log pagination previous failed. Cause" + err.getMessage()));
  }

  @Override
  public Consumer<ProcessStdOutEventDto> getStdOutConsumer() {
    return event -> {
      if (savedLineNum > AMOUNT_SAVED_LINES - 1) {
        view.clearConsole();
        //        view.displayPreviousOutPutLink();
        savedLineNum = 0;
        currentOffset += AMOUNT_SAVED_LINES;
      }

      totalLineNum++;
      savedLineNum++;
      print(event.getText());

      for (ActionDelegate actionDelegate : actionDelegates) {
        actionDelegate.onConsoleOutput(CommandOutputConsolePresenter.this);
      }
    };
  }

  private void print(String line) {
    boolean carriageReturn = line.endsWith("\r");
    view.print(line, carriageReturn);
  }

  private void printOnTop(String line) {
    boolean carriageReturn = line.endsWith("\r");
    view.print(line, carriageReturn, null, true);
  }

  @Override
  public Consumer<ProcessStartedEventDto> getProcessStartedConsumer() {
    return event -> {
      finished = false;
      view.enableStopButton(true);
      view.toggleScrollToEndButton(true);

      pid = event.getPid();

      eventBus.fireEvent(new ProcessStartedEvent(pid, machineName));
    };
  }

  @Override
  public Consumer<ProcessDiedEventDto> getProcessDiedConsumer() {
    return event -> {
      finished = true;
      view.enableStopButton(false);
      view.toggleScrollToEndButton(false);

      eventBus.fireEvent(new ProcessFinishedEvent(pid, machineName));
    };
  }

  @Override
  public Consumer<ProcessSubscribeResponseDto> getProcessSubscribeConsumer() {
    return process -> pid = process.getPid();
  }

  @Override
  public void printOutput(String output) {
    view.print(output.replaceAll("\\[STDOUT\\] ", ""), false);
  }

  @Override
  public boolean isFinished() {
    return finished;
  }

  @Override
  public void stop() {
    execAgentCommandManager.killProcess(machineName, pid);
  }

  @Override
  public void close() {
    actionDelegates.clear();
  }

  @Override
  public void addActionDelegate(ActionDelegate actionDelegate) {
    actionDelegates.add(actionDelegate);
  }

  @Override
  public void reRunProcessButtonClicked() {
    if (isFinished()) {
      commandExecutor.executeCommand(command, machineName);
    } else {
      execAgentCommandManager
          .killProcess(machineName, pid)
          .onSuccess(() -> commandExecutor.executeCommand(command, machineName));
    }
  }

  @Override
  public void stopProcessButtonClicked() {
    stop();
  }

  @Override
  public void clearOutputsButtonClicked() {
    // todo console and buffer.
    view.clearConsole();
  }

  @Override
  public void downloadOutputsButtonClicked() {
    for (ActionDelegate actionDelegate : actionDelegates) {
      actionDelegate.onDownloadOutput(this);
    }
  }

  @Override
  public void wrapTextButtonClicked() {
    wrapText = !wrapText;
    view.wrapText(wrapText);
    view.toggleWrapTextButton(wrapText);
  }

  @Override
  public void scrollToBottomButtonClicked() {
    followOutput = !followOutput;

    view.toggleScrollToEndButton(followOutput);
    view.enableAutoScroll(followOutput);
  }

  @Override
  public void onOutputScrolled(boolean bottomReached) {
    followOutput = bottomReached;
    view.toggleScrollToEndButton(bottomReached);
  }

  /**
   * Returns the console text.
   *
   * @return console text
   */
  public String getText() {
    return view.getText();
  }

  @Override
  public OutputCustomizer getCustomizer() {
    return outputCustomizer;
  }

  /** Sets up the text output customizer */
  public void setCustomizer(OutputCustomizer customizer) {
    this.outputCustomizer = customizer;
  }
}
