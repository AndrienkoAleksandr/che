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
package org.eclipse.che.plugin.testing.ide.view;

import javax.inject.Inject;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.console.OutputConsoleViewImpl;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.terminal.TerminalInitializePromiseHolder;
import org.eclipse.che.plugin.testing.ide.model.Printable;
import org.eclipse.che.plugin.testing.ide.model.Printer;
import org.eclipse.che.plugin.testing.ide.model.TestState;
import org.eclipse.che.requirejs.ModuleHolder;

/** Represents an output console for test results. */
public class PrinterOutputConsole extends OutputConsoleViewImpl implements Printer {
  private TestState currentTest;

  @Inject
  public PrinterOutputConsole(
      MachineResources resources,
      CoreLocalizationConstant localization,
      TerminalInitializePromiseHolder promiseHolder,
      ModuleHolder moduleHolder) {
    super(moduleHolder, resources, localization, promiseHolder);

    reRunProcessButton.removeFromParent();
    stopProcessButton.removeFromParent();
    clearOutputsButton.removeFromParent();
    downloadOutputsButton.removeFromParent();

    consolePanel.remove(commandPanel);
    consolePanel.remove(previewPanel);
  }

  @Override
  public void print(String text, OutputType type) {
    if (type == OutputType.STDERR) {
      print(text, 38, 255, 67, 67);
    } else {
      print(text);
    }
  }

  @Override
  public void onNewPrintable(Printable printable) {
    printable.print(this);
  }

  public void testSelected(TestState testState) {
    if (currentTest == testState) {
      return;
    }
    if (currentTest != null) {
      currentTest.setPrinter(null);
    }
    if (testState == null) {
      clearConsole();
      return;
    }
    currentTest = testState;
    clearConsole();
    testState.setPrinter(this);
    testState.print(this);
  }
}
