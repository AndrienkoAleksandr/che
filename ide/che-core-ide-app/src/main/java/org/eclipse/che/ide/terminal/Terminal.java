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
package org.eclipse.che.ide.terminal;

import com.google.gwt.dom.client.Element;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.annotations.JsProperty;


/** Created by Oleksandr Andriienko */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Terminal {

  public Terminal() {}

  @JsFunction
  public interface TerminalEventHandler {
    void invoke(Object... args);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public static class TerminalGeometry {

    @JsProperty(name = "cols")
    public native int getCols();

    @JsProperty(name = "rows")
    public native int getRows();
  }

   @JsFunction
   public interface CustomKeyDownHandl {
     void onKeyDown(Object arg);
   }

  @JsProperty(name = "cols")
  public native int getCols();

  @JsProperty(name = "rows")
  public native int getRows();

  @JsProperty(name = "element")
  public native Element getElement();

  public native void open(Element element);

  public native void write(String data);

  public native void writeln(String data);

  public native void resize(int cols, int rows);

  public native void on(String event, TerminalEventHandler function);

  public native void focus();

  public native void blur();

  public native boolean hasSelection();

  public native TerminalGeometry proposeGeometry();

//  public native void attachCustomKeyDownHandler(CustomKeyDownHandl customKeyDownHandl);
}
