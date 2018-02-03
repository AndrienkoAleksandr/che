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

import static org.eclipse.che.ide.editor.preferences.editorproperties.EditorProperties.*;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.che.ide.editor.orion.client.jso.OrionEditorViewOverlay;
import org.eclipse.che.ide.editor.preferences.editorproperties.EditorProperties;
import org.eclipse.che.ide.util.loging.Log;

/**
 * The class contains methods to simplify the work with orion settings.
 *
 * @author Roman Nikitenko
 */
public class OrionSettingsController {

  private OrionEditorViewOverlay editorViewOverlay;
  private final EnumSet<EditorProperties> orionPropertiesSet =
      EnumSet.noneOf(EditorProperties.class);

  @Inject
  public OrionSettingsController() {
    fillUpEditorPropertiesSet();
  }

  public void setEditorViewOverlay(OrionEditorViewOverlay editorViewOverlay) {
    this.editorViewOverlay = editorViewOverlay;
  }

  public void updateSettings() {
    if (editorViewOverlay != null) {
      JSONObject properties = getJsonEditorPreferencesFor(orionPropertiesSet);
      editorViewOverlay.updateSettings(properties.getJavaScriptObject());

      Log.info(getClass(), "settings was updated!!!!");
    }
  }

  private Map<String, JSONValue> getEditorPreferences() {
    Map<String, JSONValue> editorPreferences = new HashMap<>();

    JSONBoolean expandTapValue = JSONBoolean.getInstance(true);
    editorPreferences.put(EXPAND_TAB.toString(), expandTapValue);

    JSONBoolean autoSaveValue = JSONBoolean.getInstance(true);
    editorPreferences.put(ENABLE_AUTO_SAVE.toString(), autoSaveValue);

    // WORD WRAP VALUE!!!!
    JSONBoolean softWrapValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SOFT_WRAP.toString(), softWrapValue);

    JSONBoolean autoPairParenthesesValue = JSONBoolean.getInstance(true);
    editorPreferences.put(AUTO_PAIR_PARENTHESES.toString(), autoPairParenthesesValue);

    JSONBoolean autoPairBracesValue = JSONBoolean.getInstance(true);
    editorPreferences.put(AUTO_PAIR_BRACES.toString(), autoPairBracesValue);

    JSONBoolean autoPairAngleBracesValue = JSONBoolean.getInstance(true);
    editorPreferences.put(AUTO_PAIR_ANGLE_BRACKETS.toString(), autoPairAngleBracesValue);

    JSONBoolean autoPairQuotationsValue = JSONBoolean.getInstance(true);
    editorPreferences.put(AUTO_PAIR_QUOTATIONS.toString(), autoPairQuotationsValue);

    JSONBoolean autoCompleteCommentsValue = JSONBoolean.getInstance(true);
    editorPreferences.put(AUTO_COMPLETE_COMMENTS.toString(), autoCompleteCommentsValue);

    JSONBoolean smartIndentionValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SMART_INDENTATION.toString(), smartIndentionValue);

    JSONBoolean showWriteSpacesValue = JSONBoolean.getInstance(false);
    editorPreferences.put(SHOW_WHITESPACES.toString(), showWriteSpacesValue);

    JSONBoolean annotationRulerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SHOW_ANNOTATION_RULER.toString(), annotationRulerValue);

    JSONBoolean lineNumberRulerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SHOW_LINE_NUMBER_RULER.toString(), lineNumberRulerValue);

    JSONBoolean foldingRulerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SHOW_FOLDING_RULER.toString(), foldingRulerValue);

    JSONBoolean overviewRulerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SHOW_OVERVIEW_RULER.toString(), overviewRulerValue);

    JSONBoolean zoomRulerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SHOW_ZOOM_RULER.toString(), zoomRulerValue);

    JSONBoolean occurrencesRulerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(SHOW_OCCURRENCES.toString(), occurrencesRulerValue);

    JSONBoolean contentAssistantAutoTriggerValue = JSONBoolean.getInstance(true);
    editorPreferences.put(
        SHOW_CONTENT_ASSIST_AUTOMATICALLY.toString(), contentAssistantAutoTriggerValue);

    Log.info(getClass(), "pref size " + editorPreferences.size());
    return editorPreferences;
  }

  /**
   * Returns saved editor preferences in json format if they exist or default preferences otherwise
   * for given set properties.
   */
  public JSONObject getJsonEditorPreferencesFor(EnumSet<EditorProperties> filter) {
    JSONObject jsonPreferences = new JSONObject();
    Map<String, JSONValue> editorPreferences = getEditorPreferences();

    for (EditorProperties property : filter) {
      String key = property.toString();
      if (editorPreferences.containsKey(key)) {
        jsonPreferences.put(key, editorPreferences.get(key));
        Log.info(getClass(), "key " + key);
      }
    }
    return jsonPreferences;
  }

  private void fillUpEditorPropertiesSet() {
    orionPropertiesSet.add(TAB_SIZE);
    orionPropertiesSet.add(EXPAND_TAB);
    orionPropertiesSet.add(AUTO_PAIR_PARENTHESES);
    orionPropertiesSet.add(AUTO_PAIR_BRACES);
    orionPropertiesSet.add(AUTO_PAIR_SQUARE_BRACKETS);
    orionPropertiesSet.add(AUTO_PAIR_ANGLE_BRACKETS);
    orionPropertiesSet.add(AUTO_PAIR_QUOTATIONS);
    orionPropertiesSet.add(AUTO_COMPLETE_COMMENTS);
    orionPropertiesSet.add(SMART_INDENTATION);
    orionPropertiesSet.add(SHOW_WHITESPACES);
    orionPropertiesSet.add(SOFT_WRAP);
    orionPropertiesSet.add(SHOW_ANNOTATION_RULER);
    orionPropertiesSet.add(SHOW_LINE_NUMBER_RULER);
    orionPropertiesSet.add(SHOW_FOLDING_RULER);
    orionPropertiesSet.add(SHOW_OVERVIEW_RULER);
    orionPropertiesSet.add(SHOW_ZOOM_RULER);
    orionPropertiesSet.add(SHOW_OCCURRENCES);
    orionPropertiesSet.add(SHOW_CONTENT_ASSIST_AUTOMATICALLY);
  }
}
