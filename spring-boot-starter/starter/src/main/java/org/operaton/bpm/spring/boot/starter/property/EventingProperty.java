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
package org.operaton.bpm.spring.boot.starter.property;

import org.apache.commons.lang3.BooleanUtils;

import static org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties.joinOn;

/**
 * Properties controlling spring eventing.
 */
public class EventingProperty {

  /**
   * Controls events of execution listener.
   */
  private Boolean execution = Boolean.TRUE;
  /**
   * Controls events of task listener.
   */
  private Boolean task = Boolean.TRUE;
  /**
   * Controls events of history handler.
   */
  private Boolean history = Boolean.TRUE;

  /**
   * Controls if the event listeners for tasks and executions are can be skipped (controlled by `skipCustomListeners`
   * property in Cockpit and in various APIs). Defaults to true.
   */
  private Boolean skippable = Boolean.TRUE;

  public EventingProperty() {

  }

  public boolean isExecution() {
    return BooleanUtils.isTrue(execution);
  }

  public void setExecution(Boolean execution) {
    this.execution = execution;
  }

  public Boolean isTask() {
    return BooleanUtils.isTrue(task);
  }

  public void setTask(Boolean task) {
    this.task = task;
  }

  public Boolean isHistory() {
    return BooleanUtils.isTrue(history);
  }

  public void setHistory(Boolean history) {
    this.history = history;
  }

  public boolean isSkippable() {
    return BooleanUtils.isTrue(skippable);
  }

  public void setSkippable(Boolean skippable) {
    this.skippable = skippable;
  }

  @Override
  public String toString() {
    return joinOn(this.getClass())
      .add("execution=" + execution)
      .add("task=" + task)
      .add("history=" + history)
      .add("skippable=" + skippable)
      .toString();
  }
}
