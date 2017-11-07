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
package org.eclipse.che.ide.console.xterm;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.console.OutputConsoleViewImpl;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.terminal.TerminalJso;
import org.eclipse.che.ide.terminal.TerminalOptionsJso;
import org.eclipse.che.requirejs.ModuleHolder;

/**
 * @author Alexander Andrienko
 */
public class XtermOutPutConsoleViewImpl extends OutputConsoleViewImpl {

    private final TerminalJso terminalJso;

    @Inject
    public XtermOutPutConsoleViewImpl(final ModuleHolder moduleHolder,
                                      MachineResources resources,
                                      CoreLocalizationConstant localization) {
        super(resources, localization);
        JavaScriptObject terminalSource = moduleHolder.getModule("Xterm");

        //todo: don't use default options !!!
        this.terminalJso = TerminalJso.create(terminalSource, TerminalOptionsJso.createDefault());

        //todo caluculate size
        terminalJso.open(consoleLines.asWidget().getElement());
    }

//    @Override
//    public void showCommandLine(String commandLine) {
//
//    }
//
//    @Override
//    public void showPreviewUrl(String previewUrl) {
//
//    }

    @Override
    public void print(String text, boolean carriageReturn) {
        terminalJso.write(text);
    }

    @Override
    public void print(String text, boolean carriageReturn, String color) {
        terminalJso.write(text);
    }

    @Override
    public String getText() {
        return "Nope"; // todo
    }

//    @Override
//    public void hideCommand() {
//
//    }

//    @Override
//    public void hidePreview() {
//
//    }

//    @Override
//    public void wrapText(boolean wrap) {
//
//    }
//
//    @Override
//    public void enableAutoScroll(boolean enable) {
//
//    }

    @Override
    public void clearConsole() {

    }

//    @Override
//    public void toggleWrapTextButton(boolean toggle) {
//
//    }
//
//    @Override
//    public void toggleScrollToEndButton(boolean toggle) {
//
//    }
//
//    @Override
//    public void setReRunButtonVisible(boolean visible) {
//
//    }
//
//    @Override
//    public void setStopButtonVisible(boolean visible) {
//
//    }
//
//    @Override
//    public void enableStopButton(boolean enable) {
//
//    }
//
//    @Override
//    public void setDelegate(ActionDelegate delegate) {
//
//    }
}

