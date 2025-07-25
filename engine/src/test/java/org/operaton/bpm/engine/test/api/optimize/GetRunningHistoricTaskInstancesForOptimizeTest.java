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
package org.operaton.bpm.engine.test.api.optimize;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class GetRunningHistoricTaskInstancesForOptimizeTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  OptimizeService optimizeService;

  String userId = "test";

  IdentityService identityService;
  RuntimeService runtimeService;
  AuthorizationService authorizationService;
  TaskService taskService;
  HistoryService historyService;

  @BeforeEach
  void init() {
    ProcessEngineConfigurationImpl config =
      engineRule.getProcessEngineConfiguration();
    optimizeService = config.getOptimizeService();

    createUser(userId);
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
    for (HistoricTaskInstance task : historyService.createHistoricTaskInstanceQuery().list()) {
      historyService.deleteHistoricTaskInstance(task.getId());
    }
    ClockUtil.reset();
  }

  @Test
  void getRunningHistoricTaskInstances() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
        .name("task")
        .operatonAssignee(userId)
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(null, null, 10);

    // then
    assertThat(runningHistoricTaskInstances).hasSize(1);
    assertThatTasksHaveAllImportantInformation(runningHistoricTaskInstances.get(0));
  }

  @Test
  void startedAfterParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    ProcessInstance processInstance2 = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(now, null, 10);

    // then
    assertThat(runningHistoricTaskInstances).hasSize(1);
    assertThat(runningHistoricTaskInstances.get(0).getProcessInstanceId()).isEqualTo(processInstance2.getId());
  }

  @Test
  void startedAtParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    ProcessInstance processInstance1 = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(null, now, 10);

    // then
    assertThat(runningHistoricTaskInstances).hasSize(1);
    assertThat(runningHistoricTaskInstances.get(0).getProcessInstanceId()).isEqualTo(processInstance1.getId());
  }

  @Test
  void startedAfterAndFinishedAtParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(now, now, 10);

    // then
    assertThat(runningHistoricTaskInstances).isEmpty();
  }

  @Test
  void maxResultsParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(pastDate(), null, 3);

    // then
    assertThat(runningHistoricTaskInstances).hasSize(3);
  }

  @Test
  void resultIsSortedByStartTime() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    ProcessInstance processInstance1 = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    ProcessInstance processInstance2 = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    ProcessInstance processInstance3 = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricTaskInstances).hasSize(3);
    assertThat(runningHistoricTaskInstances.get(0).getProcessInstanceId()).isEqualTo(processInstance1.getId());
    assertThat(runningHistoricTaskInstances.get(1).getProcessInstanceId()).isEqualTo(processInstance2.getId());
    assertThat(runningHistoricTaskInstances.get(2).getProcessInstanceId()).isEqualTo(processInstance3.getId());
  }

  @Test
  void fetchOnlyRunningTasks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask1")
      .userTask("userTask2")
      .userTask("userTask3")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();

    // when
    List<HistoricTaskInstance> runningHistoricTaskInstances =
      optimizeService.getRunningHistoricTaskInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricTaskInstances).hasSize(1);
    assertThat(runningHistoricTaskInstances.get(0).getTaskDefinitionKey()).isEqualTo("userTask2");
  }

  private Date pastDate() {
    return new Date(2L);
  }

  private void completeAllUserTasks() {
    List<Task> list = taskService.createTaskQuery().list();
    for (Task task : list) {
      taskService.claim(task.getId(), userId);
      taskService.complete(task.getId());
    }
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
  }

  private void assertThatTasksHaveAllImportantInformation(HistoricTaskInstance completedHistoricTaskInstance) {
    assertThat(completedHistoricTaskInstance).isNotNull();
    assertThat(completedHistoricTaskInstance.getId()).isNotNull();
    assertThat(completedHistoricTaskInstance.getTaskDefinitionKey()).isEqualTo("userTask");
    assertThat(completedHistoricTaskInstance.getName()).isEqualTo("task");
    assertThat(completedHistoricTaskInstance.getStartTime()).isNotNull();
    assertThat(completedHistoricTaskInstance.getEndTime()).isNull();
    assertThat(completedHistoricTaskInstance.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(completedHistoricTaskInstance.getProcessDefinitionId()).isNotNull();
    assertThat(completedHistoricTaskInstance.getAssignee()).isEqualTo(userId);
  }

}
