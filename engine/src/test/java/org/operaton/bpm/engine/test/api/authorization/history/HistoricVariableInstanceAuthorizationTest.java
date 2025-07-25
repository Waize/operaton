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
package org.operaton.bpm.engine.test.api.authorization.history;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE_HISTORY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions.READ_HISTORY_VARIABLE;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.HISTORIC_TASK;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.HistoricTaskPermissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricVariableInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected boolean ensureSpecificVariablePermission;
  protected String deploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/messageStartEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn")
            .getId();

    ensureSpecificVariablePermission = processEngineConfiguration.isEnforceSpecificVariablePermission();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    processEngineConfiguration.setEnableHistoricInstancePermissions(false);
    processEngineConfiguration.setEnforceSpecificVariablePermission(ensureSpecificVariablePermission);
  }

  // historic variable instance query (standalone task) /////////////////////////////////////////////

  @Test
  void testQueryAfterStandaloneTaskVariables() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    taskService.setVariables(taskId, getVariables());
    enableAuthorization();

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  // historic variable instance query (process variables) ///////////////////////////////////////////

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithReadHistoryVariablePermissionOnProcessDefinition() {
    // given
    setReadHistoryVariableAsDefaultReadPermission();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithReadHistoryVariablePermissionOnAnyProcessDefinition() {
    setReadHistoryVariableAsDefaultReadPermission();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithMultipleReadHistoryVariable() {
    setReadHistoryVariableAsDefaultReadPermission();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY_VARIABLE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindVariableWithRevokedReadHistoryVariablePermissionOnProcessDefinition() {
    setReadHistoryVariableAsDefaultReadPermission();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ_HISTORY_VARIABLE);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // historic variable instance query (multiple process instances) ////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    startMultipleProcessInstances();

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnProcessDefinition() {
    startMultipleProcessInstances();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    startMultipleProcessInstances();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 7);
  }

  @Test
  void testQueryWithReadHistoryVariablePermissionOnProcessDefinition() {
    setReadHistoryVariableAsDefaultReadPermission();

    startMultipleProcessInstances();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithReadHistoryVariablePermissionOnAnyProcessDefinition() {
    setReadHistoryVariableAsDefaultReadPermission();

    startMultipleProcessInstances();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 7);
  }

  // historic variable instance query (case variables) /////////////////////////////////////////////

  @Test
  void testQueryAfterCaseVariables() {
    // given
    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  // historic variable instance query (mixed variables) ////////////////////////////////////

  @Test
  void testMixedQueryWithoutAuthorization() {
    startMultipleProcessInstances();

    setupMultipleMixedVariables();

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 7);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  @Test
  void testMixedQueryWithReadHistoryPermissionOnProcessDefinition() {
    startMultipleProcessInstances();

    setupMultipleMixedVariables();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 10);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  @Test
  void testMixedQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    startMultipleProcessInstances();

    setupMultipleMixedVariables();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 14);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  @Test
  void testMixedQueryWithReadHistoryVariablePermissionOnProcessDefinition() {
    setReadHistoryVariableAsDefaultReadPermission();

    startMultipleProcessInstances();

    setupMultipleMixedVariables();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 10);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  @Test
  void testMixedQueryWithReadHistoryVariablePermissionOnAnyProcessDefinition() {
    setReadHistoryVariableAsDefaultReadPermission();

    startMultipleProcessInstances();

    setupMultipleMixedVariables();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY_VARIABLE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 14);

    deleteTask("one", true);
    deleteTask("two", true);
    deleteTask("three", true);
    deleteTask("four", true);
    deleteTask("five", true);
  }

  // delete deployment (cascade = false)

  @Test
  void testQueryAfterDeletingDeployment() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    disableAuthorization();
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    enableAuthorization();

    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId);
    enableAuthorization();

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 3);

    cleanUpAfterDeploymentDeletion();
  }

  @Test
  void testQueryAfterDeletingDeploymentWithReadHistoryVariable() {
    setReadHistoryVariableAsDefaultReadPermission();

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    disableAuthorization();
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }
    enableAuthorization();

    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId);
    enableAuthorization();

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    verifyQueryResults(query, 3);

    cleanUpAfterDeploymentDeletion();
  }

  // delete historic variable instance (process variables) /////////////////////////////////////////////
  @Test
  void testDeleteHistoricProcessVariableInstanceWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    disableAuthorization();
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(1L);
    enableAuthorization();

    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstance(variableInstanceId))
      .withFailMessage("Exception expected: It should not be possible to delete the historic variable instance")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(DELETE_HISTORY.getName())
      .hasMessageContaining(PROCESS_KEY)
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testDeleteHistoricProcessVariableInstanceWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, DELETE_HISTORY);

    disableAuthorization();
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(1L);
    enableAuthorization();

    assertThatCode(() -> historyService.deleteHistoricVariableInstance(variableInstanceId))
      .withFailMessage("It should be possible to delete the historic variable instance with granted permissions")
      .doesNotThrowAnyException();
    // then
    verifyVariablesDeleted();
  }

  // delete deployment (cascade = false)
  @Test
  void testDeleteHistoricProcessVariableInstanceAfterDeletingDeployment() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId);
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(1L);
    enableAuthorization();

    // when
    assertThatCode(() -> historyService.deleteHistoricVariableInstance(variableInstanceId))
      // then
      .withFailMessage("It should be possible to delete the historic variable instance with granted permissions after the process definition is deleted")
      .doesNotThrowAnyException();
    verifyVariablesDeleted();
    cleanUpAfterDeploymentDeletion();
  }

  // delete historic variable instance (case variables) /////////////////////////////////////////////
  @Test
  void testDeleteHistoricCaseVariableInstance() {
    // given
    createCaseInstanceByKey(CASE_KEY, getVariables());

    disableAuthorization();
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(1L);
    enableAuthorization();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyVariablesDeleted();
  }

  // delete historic variable instance (task variables) /////////////////////////////////////////////
  @Test
  void testDeleteHistoricStandaloneTaskVariableInstance() {
    // given
    String taskId = "myTask";
    createTask(taskId);

    disableAuthorization();
    taskService.setVariables(taskId, getVariables());
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(1L);
    enableAuthorization();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyVariablesDeleted();
    deleteTask(taskId, true);

    // XXX if CAM-6570 is implemented, there should be a check for variables of standalone tasks here as well
  }

  // delete historic variable instances (process variables) /////////////////////////////////////////////
  @Test
  void testDeleteHistoricProcessVariableInstancesWithoutAuthorization() {
    // given
    ProcessInstance instance = startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String instanceId = instance.getId();
    verifyVariablesCreated();

    // when
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstancesByProcessInstanceId(instanceId))
        // then
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(DELETE_HISTORY.getName())
        .hasMessageContaining(PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testDeleteHistoricProcessVariableInstancesWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    ProcessInstance instance = startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String instanceId = instance.getId();
    verifyVariablesCreated();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, DELETE_HISTORY);

    // when
    assertThatCode(() -> historyService.deleteHistoricVariableInstancesByProcessInstanceId(instanceId))
      // then
      .withFailMessage("It should be possible to delete the historic variable instance with granted permissions")
      .doesNotThrowAnyException();
    verifyVariablesDeleted();
  }

  // delete deployment (cascade = false)
  @Test
  void testDeleteHistoricProcessVariableInstancesAfterDeletingDeployment() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    verifyVariablesCreated();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, DELETE_HISTORY);

    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId);
    enableAuthorization();

    assertThatCode(() -> historyService.deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId))
      .withFailMessage("It should be possible to delete the historic variable instance with granted permissions after the process definition is deleted")
      .doesNotThrowAnyException();

    // then
    verifyVariablesDeleted();
    cleanUpAfterDeploymentDeletion();
  }

  // helper ////////////////////////////////////////////////////////

  protected void verifyVariablesDeleted() {
    disableAuthorization();
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
    enableAuthorization();
  }

  protected void verifyVariablesCreated() {
    disableAuthorization();
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(1L);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(1L);
    enableAuthorization();
  }

  protected void cleanUpAfterDeploymentDeletion() {
    disableAuthorization();
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance instance : instances) {
      historyService.deleteHistoricProcessInstance(instance.getId());
    }
    enableAuthorization();
  }

  protected void startMultipleProcessInstances() {
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
    startProcessInstanceByKey(MESSAGE_START_PROCESS_KEY, getVariables());
  }

  protected void setupMultipleMixedVariables() {
    createTask("one");
    createTask("two");
    createTask("three");
    createTask("four");
    createTask("five");

    disableAuthorization();
    taskService.setVariables("one", getVariables());
    taskService.setVariables("two", getVariables());
    taskService.setVariables("three", getVariables());
    taskService.setVariables("four", getVariables());
    taskService.setVariables("five", getVariables());
    enableAuthorization();

    createCaseInstanceByKey(CASE_KEY, getVariables());
    createCaseInstanceByKey(CASE_KEY, getVariables());
  }

  @Test
  void testCheckNonePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadPermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "aTaskId";
    createTask(taskId);
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testCheckNonePermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String taskId = "aTaskId";
    createTask(taskId);
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).isEmpty();

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testCheckReadPermissionOnCompletedHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOnHistoricTaskAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadPermissionOnHistoricTaskAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadVariablePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testOnlyReadPermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testIgnoreReadVariablePermissionOnHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(false);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void testCheckReadVariablePermissionOnStandaloneHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    String taskId = "aTaskId";
    createTask(taskId);
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);

    // clear
    deleteTask(taskId, true);
  }

  @Test
  void testCheckReadVariablePermissionOnCompletedHistoricTask() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckReadVariablePermissionOnHistoricTaskAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.READ_VARIABLE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOnHistoricTaskAndReadHistoryVariablePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);

    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_TASK, taskId, userId, HistoricTaskPermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY_VARIABLE);

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery()
        .list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testHistoricTaskPermissionsAuthorizationDisabled() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    disableAuthorization();

    taskService.setVariable(taskId, "foo", "bar");

    // when
    List<HistoricVariableInstance> result = historyService.createHistoricVariableInstanceQuery().list();

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  void testCheckNonePermissionOnHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance_GlobalVariable() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    disableAuthorization();
    runtimeService.setVariable(processInstanceId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadPermissionOnHistoricProcessInstance_LocalVariable() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadPermissionOnCompletedHistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckNoneOnHistoricProcessInstanceAndReadHistoryPermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.NONE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  @Test
  void testCheckReadOnHistoricProcessInstanceAndNonePermissionOnProcessDefinition() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    disableAuthorization();
    taskService.setVariable(taskId, "foo", "bar");
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(HISTORIC_PROCESS_INSTANCE, processInstanceId, userId,
        HistoricProcessInstancePermissions.READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId,
        ProcessDefinitionPermissions.NONE);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // then
    assertThat(query.list())
        .extracting("processInstanceId")
        .containsExactly(processInstanceId);
  }

  protected void setReadHistoryVariableAsDefaultReadPermission() {
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);
  }

}
