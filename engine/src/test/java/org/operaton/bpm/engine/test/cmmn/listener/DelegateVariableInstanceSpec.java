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
package org.operaton.bpm.engine.test.cmmn.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.delegate.DelegateCaseVariableInstance;
import org.operaton.bpm.engine.runtime.CaseExecution;

/**
 * @author Thorben Lindhauer
 *
 */
public class DelegateVariableInstanceSpec {

  protected String expectedEventName;
  protected String expectedVariableName;
  protected String expectedVariableValue;

  protected String expectedProcessInstanceId;
  protected String expectedExecutionId;
  protected String expectedCaseInstanceId;
  protected String expectedCaseExecutionId;
  protected String expectedTaskId;
  protected String expectedActivityInstanceId;

  protected CaseExecution expectedSourceExecution;

  public void matches(DelegateCaseVariableInstance instance) {
    assertThat(instance.getEventName()).isEqualTo(expectedEventName);
    assertThat(instance.getName()).isEqualTo(expectedVariableName);
    assertThat(instance.getValue()).isEqualTo(expectedVariableValue);
    assertThat(instance.getProcessInstanceId()).isEqualTo(expectedProcessInstanceId);
    assertThat(instance.getExecutionId()).isEqualTo(expectedExecutionId);
    assertThat(instance.getCaseInstanceId()).isEqualTo(expectedCaseInstanceId);
    assertThat(instance.getCaseExecutionId()).isEqualTo(expectedCaseExecutionId);
    assertThat(instance.getTaskId()).isEqualTo(expectedTaskId);
    assertThat(instance.getActivityInstanceId()).isEqualTo(expectedActivityInstanceId);

    assertThat(instance.getSourceExecution().getId()).isEqualTo(expectedSourceExecution.getId());
    assertThat(instance.getSourceExecution().getActivityId()).isEqualTo(expectedSourceExecution.getActivityId());
    assertThat(instance.getSourceExecution().getActivityName()).isEqualTo(expectedSourceExecution.getActivityName());
    assertThat(instance.getSourceExecution().getCaseDefinitionId()).isEqualTo(expectedSourceExecution.getCaseDefinitionId());
    assertThat(instance.getSourceExecution().getCaseInstanceId()).isEqualTo(expectedSourceExecution.getCaseInstanceId());
    assertThat(instance.getSourceExecution().getParentId()).isEqualTo(expectedSourceExecution.getParentId());
  }

  public static DelegateVariableInstanceSpec fromCaseExecution(CaseExecution caseExecution) {
    DelegateVariableInstanceSpec spec = new DelegateVariableInstanceSpec();
    spec.expectedCaseExecutionId = caseExecution.getId();
    spec.expectedCaseInstanceId = caseExecution.getCaseInstanceId();
    spec.expectedSourceExecution = caseExecution;
    return spec;
  }

  public DelegateVariableInstanceSpec sourceExecution(CaseExecution sourceExecution) {
    this.expectedSourceExecution = sourceExecution;
    return this;
  }

  public DelegateVariableInstanceSpec event(String eventName) {
    this.expectedEventName = eventName;
    return this;
  }

  public DelegateVariableInstanceSpec name(String variableName) {
    this.expectedVariableName = variableName;
    return this;
  }

  public DelegateVariableInstanceSpec value(String variableValue) {
    this.expectedVariableValue = variableValue;
    return this;
  }

  public DelegateVariableInstanceSpec activityInstanceId(String activityInstanceId) {
    this.expectedActivityInstanceId = activityInstanceId;
    return this;
  }
}
