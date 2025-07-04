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
package org.operaton.bpm.model.bpmn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.instance.ConditionExpression;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class ConditionalSequenceFlowTest {

  protected BpmnModelInstance modelInstance;
  protected SequenceFlow flow1;
  protected SequenceFlow flow2;
  protected SequenceFlow flow3;
  protected ConditionExpression conditionExpression1;
  protected ConditionExpression conditionExpression2;
  protected ConditionExpression conditionExpression3;

  @BeforeEach
  void parseModel() {
    modelInstance = Bpmn.readModelFromStream(getClass().getResourceAsStream(getClass().getSimpleName() + ".xml"));
    flow1 = modelInstance.getModelElementById("flow1");
    flow2 = modelInstance.getModelElementById("flow2");
    flow3 = modelInstance.getModelElementById("flow3");
    conditionExpression1 = flow1.getConditionExpression();
    conditionExpression2 = flow2.getConditionExpression();
    conditionExpression3 = flow3.getConditionExpression();
  }

  @Test
  void shouldHaveTypeTFormalExpression() {
    assertThat(conditionExpression1.getType()).isEqualTo("tFormalExpression");
    assertThat(conditionExpression2.getType()).isEqualTo("tFormalExpression");
    assertThat(conditionExpression3.getType()).isEqualTo("tFormalExpression");
  }

  @Test
  void shouldHaveLanguage() {
    assertThat(conditionExpression1.getLanguage()).isNull();
    assertThat(conditionExpression2.getLanguage()).isNull();
    assertThat(conditionExpression3.getLanguage()).isEqualTo("groovy");
  }

  @Test
  void shouldHaveSourceCode() {
    assertThat(conditionExpression1.getTextContent()).isEqualTo("test");
    assertThat(conditionExpression2.getTextContent()).isEqualTo("${test}");
    assertThat(conditionExpression3.getTextContent()).isEmpty();
  }

  @Test
  void shouldHaveResource() {
    assertThat(conditionExpression1.getOperatonResource()).isNull();
    assertThat(conditionExpression2.getOperatonResource()).isNull();
    assertThat(conditionExpression3.getOperatonResource()).isEqualTo("test.groovy");
  }

}
