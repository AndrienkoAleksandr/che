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
package org.eclipse.che.ide.editor.orion.client.command.output;

import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.console.OutputConsoleView;
import org.eclipse.che.ide.editor.orion.client.inject.OrionCodeEditWidgetProvider;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorViewOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionTextModelOverlay;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGImage;

/** @author Alexander Andrienko */
public class OrionOutPutConsoleViewImpl extends Composite implements OutputConsoleView {

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

  private OrionEditorViewOverlay orionView;

  private final PromiseProvider promiseProvider;

  interface OrionOutPutConsoleViewUiBinder extends UiBinder<Widget, OrionOutPutConsoleViewImpl> {}

  private static final OrionOutPutConsoleViewUiBinder UI_BINDER =
      GWT.create(OrionOutPutConsoleViewUiBinder.class);

  @Inject
  public OrionOutPutConsoleViewImpl(
      MachineResources resources,
      CoreLocalizationConstant localization,
      OrionCodeEditWidgetProvider widgetProvider,
      PromiseProvider promiseProvider) {
    this.promiseProvider = promiseProvider;
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

    widgetProvider
        .get()
        .createEditorView(consoleLines.getElement(), JavaScriptObject.createObject())
        .then(
            editor -> {
              orionView = editor;
            })
        .catchError(
            err -> {
              Log.error(getClass(), err.getMessage());
            });
  }

  @Override
  public void print(String text, boolean carriageReturn) {
    OrionTextModelOverlay modelOverlay = orionView.getEditor().getModel();
    int startOffSet = modelOverlay.getCharCount() - 1;
    modelOverlay.setText(text + "\n\r", startOffSet);
  }

  // todo implement coloring
  @Override
  public void print(String text, boolean carriageReturn, String color) {
    OrionTextModelOverlay modelOverlay = orionView.getEditor().getModel();
    int startOffSet = modelOverlay.getCharCount() - 1;
    modelOverlay.setText(text + "\n\r", startOffSet);
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
    return orionView.getEditor().getText();
  }
}
