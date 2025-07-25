/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Stores a two-dimensional graph layout.
 *
 * @author Falko Menge
 */
public class DiagramLayout implements Serializable {

  private static final long serialVersionUID = 1L;

  private Map<String, DiagramElement> elements;

  public DiagramLayout(Map<String, DiagramElement> elements) {
    this.setElements(elements);
  }

  public DiagramNode getNode(String id) {
    DiagramElement element = getElements().get(id);
    if (element instanceof DiagramNode diagramNode) {
      return diagramNode;
    } else {
      return null;
    }
  }

  public DiagramEdge getEdge(String id) {
    DiagramElement element = getElements().get(id);
    if (element instanceof DiagramEdge diagramEdge) {
      return diagramEdge;
    } else {
      return null;
    }
  }

  public Map<String, DiagramElement> getElements() {
    return elements;
  }

  public void setElements(Map<String, DiagramElement> elements) {
    this.elements = elements;
  }

  public List<DiagramNode> getNodes() {
    List<DiagramNode> nodes = new ArrayList<>();
    for (Entry<String, DiagramElement> entry : getElements().entrySet()) {
      DiagramElement element = entry.getValue();
      if (element instanceof DiagramNode diagramNode) {
        nodes.add(diagramNode);
      }
    }
    return nodes;
  }

}
