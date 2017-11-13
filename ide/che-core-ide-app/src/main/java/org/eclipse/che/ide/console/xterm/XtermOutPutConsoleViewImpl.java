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

import static org.eclipse.che.ide.console.Constants.SCROLL_BACK;
import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.HandlerRegistration;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.console.OutputConsoleView;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.terminal.TerminalGeometryJso;
import org.eclipse.che.ide.terminal.TerminalInitializePromiseHolder;
import org.eclipse.che.ide.terminal.TerminalJso;
import org.eclipse.che.ide.terminal.TerminalOptionsJso;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.requirejs.ModuleHolder;
import org.vectomatic.dom.svg.ui.SVGImage;

/** @author Alexander Andrienko */
public class XtermOutPutConsoleViewImpl extends Composite implements OutputConsoleView {

  private TerminalJso terminalJso; // todo final

  private ActionDelegate delegate;

  @UiField protected DockLayoutPanel consolePanel;

  @UiField protected FlowPanel commandPanel;

  @UiField protected FlowPanel previewPanel;

  @UiField Label commandTitle;

  @UiField Label commandLabel;

  @UiField protected ResizeLayoutPanel consoleLines;

  @UiField Anchor previewUrlLabel;

  @UiField protected FlowPanel reRunProcessButton;

  @UiField protected FlowPanel stopProcessButton;

  @UiField protected FlowPanel clearOutputsButton;

  @UiField protected FlowPanel downloadOutputsButton;

  @UiField Button checkButton;

  @UiField FlowPanel wrapTextButton;

  @UiField FlowPanel scrollToBottomButton;

  //  /** If true - next printed line should replace the previous one. */
  //  private boolean carriageReturn;

  //  /** Follow the output. Scroll to the bottom automatically when <b>true</b>. */
  //  private boolean followOutput = true;

  //  /** Scroll to the bottom immediately when view become visible. */
  //  private boolean followScheduled = false;

  //  private final List<Pair<RegExp, String>> output2Color =
  //      newArrayList(
  //          new Pair<>(compile("\\[\\s*(DOCKER)\\s*\\]"), "#4EABFF"),
  //          new Pair<>(compile("\\[\\s*(ERROR)\\s*\\]"), "#FF2727"),
  //          new Pair<>(compile("\\[\\s*(WARN)\\s*\\]"), "#F5A623"),
  //          new Pair<>(compile("\\[\\s*(STDOUT)\\s*\\]"), "#8ED72B"),
  //          new Pair<>(compile("\\[\\s*(STDERR)\\s*\\]"), "#FF4343"));

  interface XtermOutPutConsoleViewUiBinder extends UiBinder<Widget, XtermOutPutConsoleViewImpl> {}

  private static final XtermOutPutConsoleViewUiBinder UI_BINDER =
      GWT.create(XtermOutPutConsoleViewUiBinder.class);

  @Inject
  public XtermOutPutConsoleViewImpl(
      final ModuleHolder moduleHolder,
      MachineResources resources,
      CoreLocalizationConstant localization,
      TerminalInitializePromiseHolder promiseHolder) {
    initWidget(UI_BINDER.createAndBindUi(this));

    reRunProcessButton.add(new SVGImage(resources.reRunIcon()));
    stopProcessButton.add(new SVGImage(resources.stopIcon()));
    clearOutputsButton.add(new SVGImage(resources.clearOutputsIcon()));
    downloadOutputsButton.getElement().setInnerHTML(FontAwesome.DOWNLOAD);

    wrapTextButton.add(new SVGImage(resources.lineWrapIcon()));
    scrollToBottomButton.add(new SVGImage(resources.scrollToBottomIcon()));

    reRunProcessButton.addDomHandler(
        event -> {
          if (!reRunProcessButton.getElement().hasAttribute("disabled") && delegate != null) {
            delegate.reRunProcessButtonClicked();
          }
        },
        ClickEvent.getType());

    stopProcessButton.addDomHandler(
        event -> {
          if (!stopProcessButton.getElement().hasAttribute("disabled") && delegate != null) {
            delegate.stopProcessButtonClicked();
          }
        },
        ClickEvent.getType());

    clearOutputsButton.addDomHandler(
        event -> {
          if (!clearOutputsButton.getElement().hasAttribute("disabled") && delegate != null) {
            delegate.clearOutputsButtonClicked();
          }
        },
        ClickEvent.getType());

    downloadOutputsButton.addDomHandler(
        event -> {
          if (delegate != null) {
            delegate.downloadOutputsButtonClicked();
          }
        },
        ClickEvent.getType());

    wrapTextButton.addDomHandler(
        clickEvent -> {
          if (!wrapTextButton.getElement().hasAttribute("disabled") && delegate != null) {
            delegate.wrapTextButtonClicked();
          }
        },
        ClickEvent.getType());

    scrollToBottomButton.addDomHandler(
        event -> {
          if (!scrollToBottomButton.getElement().hasAttribute("disabled") && delegate != null) {
            delegate.scrollToBottomButtonClicked();
          }
        },
        ClickEvent.getType());

    Tooltip.create(
        (elemental.dom.Element) reRunProcessButton.getElement(),
        BOTTOM,
        MIDDLE,
        localization.consolesReRunButtonTooltip());

    Tooltip.create(
        (elemental.dom.Element) stopProcessButton.getElement(),
        BOTTOM,
        MIDDLE,
        localization.consolesStopButtonTooltip());

    Tooltip.create(
        (elemental.dom.Element) clearOutputsButton.getElement(),
        BOTTOM,
        MIDDLE,
        localization.consolesClearOutputsButtonTooltip());

    Tooltip.create(
        (elemental.dom.Element) wrapTextButton.getElement(),
        BOTTOM,
        MIDDLE,
        localization.consolesWrapTextButtonTooltip());

    Tooltip.create(
        (elemental.dom.Element) scrollToBottomButton.getElement(),
        BOTTOM,
        MIDDLE,
        localization.consolesAutoScrollButtonTooltip());

    promiseHolder
        .getInitializerPromise()
        .then(
            arg -> {
              JavaScriptObject terminalSource = moduleHolder.getModule("Xterm");

              // todo: don't use default options !!!
              this.terminalJso =
                  TerminalJso.create(
                      terminalSource,
                      TerminalOptionsJso.createDefault()
                          .withFocusOnOpen(false)
                          .withScrollBack(SCROLL_BACK));

              // todo calculate size;
              terminalJso.open(consoleLines.asWidget().getElement());
              TerminalGeometryJso geometryJso = terminalJso.proposeGeometry();
              terminalJso.resize(geometryJso.getCols(), geometryJso.getRows());
              Log.info(getClass(), "Script loaded" + consoleLines.asWidget().getOffsetHeight());
            });

    consoleLines.addAttachHandler(event -> Log.info(getClass(), "attach!!!"));
    HandlerRegistration reg =
        consoleLines.addResizeHandler(
            event -> {
              Log.info(getClass(), "resize!!!!");
              resizeTimer.schedule(500);
            });

    checkButton.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Log.info(getClass(), "check" + resizeTimer.isRunning());
          }
        });
  }

  private Timer resizeTimer =
      new Timer() {
        @Override
        public void run() {
          resizeTerminal();
        }
      };

  private void resizeTerminal() {
    //    Log.info(getClass(), "calculate geometry");
    TerminalGeometryJso geometryJso = terminalJso.proposeGeometry();
    //    Log.info(getClass(), "calculate geometry completed" + geometryJso);
    //
    terminalJso.resize(geometryJso.getCols(), geometryJso.getRows());
    //    Log.info(getClass(), "resize!!!!");
  }

  @Override
  public void print(String text, boolean carriageReturn) {
//    Log.info(getClass(), "print1");
    terminalJso.writeln(text);
  }

  @Override
  public void print(String text, boolean carriageReturn, String color) {
//    Log.info(getClass(), "print2");
    terminalJso.writeln(text);
  }

  @Override
  public void setDelegate(ActionDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  public void hideCommand() {
    consolePanel.setWidgetHidden(commandPanel, true);
  }

  @Override
  public void hidePreview() {
    consolePanel.setWidgetHidden(previewPanel, true);
  }

  @Override
  public void wrapText(boolean wrap) {
    if (wrap) {
      consoleLines.getElement().setAttribute("wrap", "");
    } else {
      consoleLines.getElement().removeAttribute("wrap");
    }
  }

  @Override
  public void enableAutoScroll(boolean enable) {
    // todo
  }

  @Override
  public void clearConsole() {
    consoleLines.getElement().setInnerHTML("");
  }

  @Override
  public void toggleWrapTextButton(boolean toggle) {
    if (toggle) {
      wrapTextButton.getElement().setAttribute("toggled", "");
    } else {
      wrapTextButton.getElement().removeAttribute("toggled");
    }
  }

  @Override
  public void toggleScrollToEndButton(boolean toggle) {
    if (toggle) {
      scrollToBottomButton.getElement().setAttribute("toggled", "");
    } else {
      scrollToBottomButton.getElement().removeAttribute("toggled");
    }
  }

  @Override
  public void setReRunButtonVisible(boolean visible) {
    reRunProcessButton.setVisible(visible);
  }

  @Override
  public void setStopButtonVisible(boolean visible) {
    stopProcessButton.setVisible(visible);
  }

  @Override
  public void enableStopButton(boolean enable) {
    if (enable) {
      stopProcessButton.getElement().removeAttribute("disabled");
    } else {
      stopProcessButton.getElement().setAttribute("disabled", "");
    }
  }

  @Override
  public void showCommandLine(String commandLine) {
    commandLabel.setText(commandLine);
    Tooltip.create((elemental.dom.Element) commandLabel.getElement(), BOTTOM, MIDDLE, commandLine);
  }

  @Override
  public void showPreviewUrl(String previewUrl) {
    if (Strings.isNullOrEmpty(previewUrl)) {
      hidePreview();
    } else {
      previewUrlLabel.setText(previewUrl);
      previewUrlLabel.setHref(previewUrl);
      Tooltip.create(
          (elemental.dom.Element) previewUrlLabel.getElement(), BOTTOM, MIDDLE, previewUrl);
    }
  }

  @Override
  public String getText() {
    // todo complete this method.
    return "";
  }
}
