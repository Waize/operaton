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

/**
 * Represents a diagram node.
 *
 * @author Falko Menge
 */
public abstract class DiagramElement implements Serializable {

  private static final long serialVersionUID = 1L;

  protected String id = null;

  protected DiagramElement() {
  }

  protected DiagramElement(String id) {
    this.id = id;
  }

  /**
   * Id of the diagram element.
   */
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "id=" + getId();
  }

  public abstract boolean isNode();
  public abstract boolean isEdge();

}
