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
package org.operaton.bpm.engine.test.assertions.bpmn;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.externalTaskQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;

import org.junit.jupiter.api.Test;

class ExternalTaskAssertHasTopicNameTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-hasTopicName.bpmn"})
  void hasTopicNameSuccess() {
    // When
    runtimeService().startProcessInstanceByKey("ExternalTaskAssert-hasTopicName");
    // Then
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    // Then
    assertThat(externalTaskQuery().singleResult()).hasTopicName("External_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-hasTopicName.bpmn"})
  void hasTopicNameFailure() {
    // When
    runtimeService().startProcessInstanceByKey("ExternalTaskAssert-hasTopicName");
    // Then
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    // And
    expect(() -> assertThat(externalTaskQuery().singleResult()).hasTopicName("External_2"));
  }

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-hasTopicName.bpmn"})
  void hasTopicNameErrorNull() {
    // When
    runtimeService().startProcessInstanceByKey("ExternalTaskAssert-hasTopicName");
    // Then
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    // And
    expect(() -> assertThat(externalTaskQuery().singleResult()).hasTopicName(null));
  }

}
