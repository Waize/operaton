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
package org.operaton.bpm.model.cmmn;

import org.operaton.bpm.model.cmmn.instance.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Sebastian Menski
 */
class CreateModelTest {

  public CmmnModelInstance modelInstance;
  public Definitions definitions;

  @BeforeEach
  void createEmptyModel() {
    modelInstance = Cmmn.createEmptyModel();
    definitions = modelInstance.newInstance(Definitions.class);
    definitions.setTargetNamespace("http://operaton.org/examples");
    modelInstance.setDefinitions(definitions);
  }

  protected <T extends CmmnModelElementInstance> T createElement(CmmnModelElementInstance parentElement, String id, Class<T> elementClass) {
    T element = modelInstance.newInstance(elementClass);
    element.setAttributeValue("id", id, true);
    parentElement.addChildElement(element);
    return element;
  }

  @Test
  void createCaseWithOneHumanTask() {
    // create process
    Case caseInstance = createElement(definitions, "case-with-one-human-task", Case.class);

    // create case plan model
    CasePlanModel casePlanModel = createElement(caseInstance, "casePlanModel_1", CasePlanModel.class);

    // create elements
    HumanTask humanTask = createElement(casePlanModel, "HumanTask_1", HumanTask.class);

    // create a plan item
    PlanItem planItem = createElement(casePlanModel, "PlanItem_1", PlanItem.class);

    // set definition to human task
    planItem.setDefinition(humanTask);

    assertThatCode(() -> Cmmn.validateModel(modelInstance)).doesNotThrowAnyException();
  }

  @Test
  void createCaseWithOneStageAndNestedHumanTask() {
    // create process
    Case caseInstance = createElement(definitions, "case-with-one-human-task", Case.class);

    // create case plan model
    CasePlanModel casePlanModel = createElement(caseInstance, "casePlanModel_1", CasePlanModel.class);

    // create a stage
    Stage stage = createElement(casePlanModel, "Stage_1", Stage.class);

    // create elements
    HumanTask humanTask = createElement(stage, "HumanTask_1", HumanTask.class);

    // create a plan item
    PlanItem planItem = createElement(stage, "PlanItem_1", PlanItem.class);

    // set definition to human task
    planItem.setDefinition(humanTask);

    assertThatCode(() -> Cmmn.validateModel(modelInstance)).doesNotThrowAnyException();
  }
}
