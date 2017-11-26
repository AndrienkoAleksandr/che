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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.gwt.regexp.shared.RegExp.compile;
import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import elemental.util.Timer;
import java.util.List;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGImage;

/**
 * View representation of output console.
 *
 * @author Artem Zatsarynnyi
 * @author Vitaliy Guliy
 */
public class OutputConsoleViewImpl extends Composite implements OutputConsoleView, ScrollHandler {

  private final List<Pair<RegExp, String>> output2Color =
      newArrayList(
          new Pair<>(compile("\\[\\s*(DOCKER)\\s*\\]"), "#4EABFF"),
          new Pair<>(compile("\\[\\s*(ERROR)\\s*\\]"), "#FF2727"),
          new Pair<>(compile("\\[\\s*(WARN)\\s*\\]"), "#F5A623"),
          new Pair<>(compile("\\[\\s*(STDOUT)\\s*\\]"), "#8ED72B"),
          new Pair<>(compile("\\[\\s*(STDERR)\\s*\\]"), "#FF4343"));

  interface OutputConsoleViewUiBinder extends UiBinder<Widget, OutputConsoleViewImpl> {}

  private static final OutputConsoleViewUiBinder UI_BINDER =
      GWT.create(OutputConsoleViewUiBinder.class);

  private ActionDelegate delegate;

  @UiField protected DockLayoutPanel consolePanel;

  @UiField protected FlowPanel commandPanel;

  @UiField protected FlowPanel previewPanel;

  @UiField Label commandTitle;

  @UiField Label commandLabel;

  @UiField ScrollPanel scrollPanel;

  @UiField FlowPanel consoleLines;

  @UiField Anchor previewUrlLabel;

  @UiField Label paginationConsolePrevious;

  @UiField Label paginationConsoleNext;

  @UiField protected FlowPanel reRunProcessButton;

  @UiField protected FlowPanel stopProcessButton;

  @UiField protected FlowPanel clearOutputsButton;

  @UiField protected FlowPanel downloadOutputsButton;

  @UiField FlowPanel wrapTextButton;

  @UiField FlowPanel scrollToBottomButton;

  /** If true - next printed line should replace the previous one. */
  private boolean carriageReturn;

  /** Follow the output. Scroll to the bottom automatically when <b>true</b>. */
  private boolean followOutput = true;

  /** Scroll to the bottom immediately when view become visible. */
  private boolean followScheduled = false;

  @Inject
  public OutputConsoleViewImpl(MachineResources resources, CoreLocalizationConstant localization) {
    initWidget(UI_BINDER.createAndBindUi(this));

    reRunProcessButton.add(new SVGImage(resources.reRunIcon()));
    stopProcessButton.add(new SVGImage(resources.stopIcon()));
    clearOutputsButton.add(new SVGImage(resources.clearOutputsIcon()));
    downloadOutputsButton.getElement().setInnerHTML(FontAwesome.DOWNLOAD);

    wrapTextButton.add(new SVGImage(resources.lineWrapIcon()));
    scrollToBottomButton.add(new SVGImage(resources.scrollToBottomIcon()));

    scrollPanel.addDomHandler(this, ScrollEvent.getType());

    reRunProcessButton.addDomHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (!reRunProcessButton.getElement().hasAttribute("disabled") && delegate != null) {
              delegate.reRunProcessButtonClicked();
            }
          }
        },
        ClickEvent.getType());

    stopProcessButton.addDomHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (!stopProcessButton.getElement().hasAttribute("disabled") && delegate != null) {
              delegate.stopProcessButtonClicked();
            }
          }
        },
        ClickEvent.getType());

    clearOutputsButton.addDomHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (!clearOutputsButton.getElement().hasAttribute("disabled") && delegate != null) {
              delegate.clearOutputsButtonClicked();
            }
          }
        },
        ClickEvent.getType());

    downloadOutputsButton.addDomHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (delegate != null) {
              delegate.downloadOutputsButtonClicked();
            }
          }
        },
        ClickEvent.getType());

    wrapTextButton.addDomHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent clickEvent) {
            if (!wrapTextButton.getElement().hasAttribute("disabled") && delegate != null) {
              delegate.wrapTextButtonClicked();
            }
          }
        },
        ClickEvent.getType());

    scrollToBottomButton.addDomHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (!scrollToBottomButton.getElement().hasAttribute("disabled") && delegate != null) {
              delegate.scrollToBottomButtonClicked();
            }
          }
        },
        ClickEvent.getType());

    paginationConsolePrevious.addClickHandler(event -> delegate.onPaginationPreviousClicked());
    paginationConsoleNext.addClickHandler(event -> delegate.onPaginationNextClicked());

    scrollPanel.addScrollHandler(
        new ScrollHandler() {

          @Override
          public void onScroll(ScrollEvent event) {
            //            Log.info(getClass(),
            // "--------------------------------------------------------------------------");
            //            Log.info(getClass(), " scrollPanel.getOffsetHeight(): " +
            // scrollPanel.getOffsetHeight());
            //            Log.info(getClass(), " scrollPanel.getVerticalScrollPosition(): " +
            // scrollPanel.getVerticalScrollPosition());
            //            Log.info(getClass(), " scrollPanel.getMaximumVerticalScrollPosition(): " +
            // scrollPanel.getMaximumVerticalScrollPosition());
            //            Log.info(getClass(), " consoleLines.getOffsetHeight(): " +
            // consoleLines.getOffsetHeight());
            //            Log.info(getClass(), " top " +
            // scrollPanel.getElement().getStyle().getPaddingTop());
            //            Log.info(getClass(), " bottom  " +
            // scrollPanel.getElement().getStyle().getPaddingBottom());
            //            Log.info(getClass(), " " + scrollPanel.getElement().getClientHeight());
            //            Log.info(getClass(), scrollPanel.getElement().getStyle().getPadding());

            if (scrollPanel.getVerticalScrollPosition() == 0) {
              Log.info(getClass(), "previous");
              delegate.onPaginationPreviousClicked();
            }
            //
            int bottomPosition = scrollPanel.getVerticalScrollPosition() +
            scrollPanel.getElement().getClientHeight() - 2; // 2px it's a padding.
            //           Log.info(getClass(), bottomPosition + " " +consoleLines.getOffsetHeight());
//            if (bottomPosition == consoleLines.getOffsetHeight()) {
//              Log.info(getClass(), "next");
//              delegate.onPaginationNextClicked();
//            }
            //            Log.info(getClass(),
            // "--------------------------------------------------------------------------");
          }
        });

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
    followOutput = enable;
    followOutput();
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
  public void displayPreviousOutPutLink() {
    // todo localization constant
    paginationConsolePrevious.setText("Click to display previous output.");
  }

  @Override
  public void hidePreviousOutPutLink() {
    paginationConsolePrevious.setText("");
  }

  @Override
  public void displayNextOutPutPartLink() {
    // todo localization constant
    paginationConsoleNext.setText("Click to display next output.");
  }

  @Override
  public void hideNextOutPutPartLink() {
    paginationConsoleNext.setText("");
  }

  @Override
  public void scrollToBottom() {
    scrollPanel.scrollToBottom();
  }

  @Override
  public void print(String text, boolean carriageReturn) {
    print(text, carriageReturn, null);
  }

  @Override
  public void print(final String text, boolean carriageReturn, String color) {
    if (this.carriageReturn) {
      Node lastChild = consoleLines.getElement().getLastChild();
      if (lastChild != null) {
        lastChild.removeFromParent();
      }
    }

    this.carriageReturn = carriageReturn;

    PreElement pre = DOM.createElement("pre").cast();
    pre.setInnerSafeHtml(generateContent(text, color));
    if (color != null) {
      pre.getStyle().setColor(color);
    }

    consoleLines.getElement().appendChild(pre);

    // followOutput();
  }

  @Override
  public void printOnTop(final String text, boolean carriageReturn) {
    if (this.carriageReturn) {
      Node lastChild = consoleLines.getElement().getLastChild();
      if (lastChild != null) {
        lastChild.removeFromParent();
      }
    }

    this.carriageReturn = carriageReturn;

    PreElement pre = DOM.createElement("pre").cast();
    pre.setInnerSafeHtml(generateContent(text, null));

    Node firstChild = consoleLines.getElement().getFirstChild();
    //    Log.info(getClass(), lastChild.getNodeName());
    if (firstChild != null) {
      consoleLines.getElement().insertBefore(pre, firstChild);
    }

    //    followOutput();
  }

  private SafeHtml generateContent(String text, String color) {
    return (SafeHtml)
        () -> {
          if (Strings.isNullOrEmpty(text)) {
            return " ";
          }

          String encoded = SafeHtmlUtils.htmlEscape(text);
          if (delegate != null) {
            if (delegate.getCustomizer() != null) {
              if (delegate.getCustomizer().canCustomize(encoded)) {
                encoded = delegate.getCustomizer().customize(encoded);
              }
            }
          }

          for (final Pair<RegExp, String> pair : output2Color) {
            final MatchResult matcher = pair.first.exec(encoded);

            if (matcher != null) {
              return encoded.replaceAll(
                  matcher.getGroup(1),
                  "<span style=\"color: " + pair.second + "\">" + matcher.getGroup(1) + "</span>");
            }
          }

          return encoded;
        };
  }

  @Override
  public void clearText() {
    consoleLines.getElement().removeAllChildren();
  }

  @Override
  public void clearLines(int amountLines, int offset) {
    NodeList<Node> children = consoleLines.getElement().getChildNodes();

    Node node = consoleLines.getElement().getFirstChild();
    int amountChildren = children.getLength();
    for (int i = 0; amountLines != 0 && i < amountChildren; i++) {
      if (node == null) {
        break;
      }

      Node nextNode = node.getNextSibling();
      if (i < offset) {
        node = nextNode;
        continue;
      }

      consoleLines.getElement().removeChild(node);
      amountLines--;
      node = nextNode;
    }
  }

  @Override
  public String getText() {
    String text = "";
    NodeList<Node> nodes = consoleLines.getElement().getChildNodes();

    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.getItem(i);
      Element element = node.cast();
      text += element.getInnerText() + "\r\n";
    }

    return text;
  }

  @Override
  public void onScroll(ScrollEvent event) {
    // Do nothing if content height less scroll area height
    if (scrollPanel.getElement().getScrollHeight() < scrollPanel.getElement().getOffsetHeight()) {
      followOutput = true;
      if (delegate != null) {
        delegate.onOutputScrolled(followOutput);
      }
      return;
    }

    // Follow output if scroll area is scrolled to the end
    if (scrollPanel.getElement().getScrollTop() + scrollPanel.getElement().getOffsetHeight()
        >= scrollPanel.getElement().getScrollHeight()) {
      followOutput = true;
    } else {
      followOutput = false;
    }

    if (delegate != null) {
      delegate.onOutputScrolled(followOutput);
    }
  }

  private Timer visibilityTimer =
      new Timer() {
        @Override
        public void run() {
          if (isVisible() && followOutput) {
            scrollPanel.scrollToBottom();
            scrollPanel.scrollToLeft();
          }
        }
      };

  /** Scrolls to the bottom if following the output is enabled. */
  private void followOutput() {
    if (!followOutput) {
      return;
    }

    /** Scroll bottom immediately if view is visible */
    if (isVisible() && followOutput) {
      scrollPanel.scrollToBottom();
      scrollPanel.scrollToLeft();
      return;
    } else {
      /** Otherwise, check the visibility periodically and scroll the view when it's visible */
      visibilityTimer.schedule(500);
    }
  }
}
