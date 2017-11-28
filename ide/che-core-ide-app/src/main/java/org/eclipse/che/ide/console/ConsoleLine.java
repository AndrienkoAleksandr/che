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

/** @author Alexander Andrienko */
public class ConsoleLine {
  private String text;
  private String color;

  public ConsoleLine(String text) {
    this.text = text;
  }

  public ConsoleLine(String text, String color) {
    this.text = text;
    this.color = color;
  }

  public String getText() {
    return text;
  }

  public String getColor() {
    return color;
  }
}
