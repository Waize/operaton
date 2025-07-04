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
package org.operaton.bpm.engine.test.standalone.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.history.HistoricFormField;
import org.operaton.bpm.engine.history.HistoricFormProperty;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.cmd.SubmitStartFormCmd;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.DummySerializable;
import org.operaton.bpm.engine.test.api.runtime.util.CustomSerializable;
import org.operaton.bpm.engine.test.api.runtime.util.FailingSerializable;
import org.operaton.bpm.engine.test.history.SerializableVariable;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;

/**
 * @author Tom Baeyens
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Christian Lipphardt (Camunda)
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class FullHistoryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  TaskService taskService;
  FormService formService;
  RepositoryService repositoryService;
  CaseService caseService;

  @Test
  @Deployment
  @SuppressWarnings("deprecation")
  void testVariableUpdates() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("number", "one");
    variables.put("character", "a");
    variables.put("bytes", ":-(".getBytes());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveTask", variables);
    runtimeService.setVariable(processInstance.getId(), "number", "two");
    runtimeService.setVariable(processInstance.getId(), "bytes", ":-)".getBytes());

    // Start-task should be added to history
    HistoricActivityInstance historicStartEvent = historyService.createHistoricActivityInstanceQuery()
      .processInstanceId(processInstance.getId())
      .activityId("theStart")
      .singleResult();
    assertThat(historicStartEvent).isNotNull();

    HistoricActivityInstance waitStateActivity = historyService.createHistoricActivityInstanceQuery()
      .processInstanceId(processInstance.getId())
      .activityId("waitState")
      .singleResult();
    assertThat(waitStateActivity).isNotNull();

    HistoricActivityInstance serviceTaskActivity = historyService.createHistoricActivityInstanceQuery()
      .processInstanceId(processInstance.getId())
      .activityId("serviceTask")
      .singleResult();
    assertThat(serviceTaskActivity).isNotNull();

    List<HistoricDetail> historicDetails = historyService
      .createHistoricDetailQuery()
      .orderByVariableName().asc()
      .orderByVariableRevision().asc()
      .list();

    assertThat(historicDetails).hasSize(10);

    HistoricVariableUpdate historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("bytes");
    assertThat(new String((byte[]) historicVariableUpdate.getValue())).isEqualTo(":-(");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(processInstance.getProcessInstanceId());

    // Variable is updated when process was in waitstate
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(1);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("bytes");
    assertThat(new String((byte[]) historicVariableUpdate.getValue())).isEqualTo(":-)");
    assertThat(historicVariableUpdate.getRevision()).isEqualTo(1);
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(waitStateActivity.getId());

    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(2);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("character");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("a");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(processInstance.getProcessInstanceId());

    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(3);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("number");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("one");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(processInstance.getProcessInstanceId());

    // Variable is updated when process was in waitstate
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(4);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("number");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("two");
    assertThat(historicVariableUpdate.getRevision()).isEqualTo(1);
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(waitStateActivity.getId());

    // Variable set from process-start execution listener
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(5);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar1");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: start");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(processInstance.getProcessInstanceId());

    // Variable set from transition take execution listener
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(6);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar2");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: take");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isNull();

    // Variable set from activity start execution listener on the servicetask
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(7);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar3");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: start");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(serviceTaskActivity.getId());

    // Variable set from activity end execution listener on the servicetask
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(8);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zVar4");
    assertThat(historicVariableUpdate.getValue()).isEqualTo("Event: end");
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(serviceTaskActivity.getId());

    // Variable set from service-task
    historicVariableUpdate = (HistoricVariableUpdate) historicDetails.get(9);
    assertThat(historicVariableUpdate.getVariableName()).isEqualTo("zzz");
    assertThat(historicVariableUpdate.getValue()).isEqualTo(123456789L);
    assertThat(historicVariableUpdate.getRevision()).isZero();
    assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(serviceTaskActivity.getId());

    // trigger receive task
    runtimeService.signal(processInstance.getId());
    testHelper.assertProcessEnded(processInstance.getId());

    // check for historic process variables set
    HistoricVariableInstanceQuery historicProcessVariableQuery = historyService
            .createHistoricVariableInstanceQuery()
            .orderByVariableName().asc();

    assertThat(historicProcessVariableQuery.count()).isEqualTo(8);

    List<HistoricVariableInstance> historicVariables = historicProcessVariableQuery.list();

    // Variable status when process is finished
    HistoricVariableInstance historicVariable = historicVariables.get(0);
    assertThat(historicVariable.getVariableName()).isEqualTo("bytes");
    assertThat(new String((byte[]) historicVariable.getValue())).isEqualTo(":-)");

    historicVariable = historicVariables.get(1);
    assertThat(historicVariable.getVariableName()).isEqualTo("character");
    assertThat(historicVariable.getValue()).isEqualTo("a");

    historicVariable = historicVariables.get(2);
    assertThat(historicVariable.getVariableName()).isEqualTo("number");
    assertThat(historicVariable.getValue()).isEqualTo("two");

    historicVariable = historicVariables.get(3);
    assertThat(historicVariable.getVariableName()).isEqualTo("zVar1");
    assertThat(historicVariable.getValue()).isEqualTo("Event: start");

    historicVariable = historicVariables.get(4);
    assertThat(historicVariable.getVariableName()).isEqualTo("zVar2");
    assertThat(historicVariable.getValue()).isEqualTo("Event: take");

    historicVariable = historicVariables.get(5);
    assertThat(historicVariable.getVariableName()).isEqualTo("zVar3");
    assertThat(historicVariable.getValue()).isEqualTo("Event: start");

    historicVariable = historicVariables.get(6);
    assertThat(historicVariable.getVariableName()).isEqualTo("zVar4");
    assertThat(historicVariable.getValue()).isEqualTo("Event: end");

    historicVariable = historicVariables.get(7);
    assertThat(historicVariable.getVariableName()).isEqualTo("zzz");
    assertThat(historicVariable.getValue()).isEqualTo(123456789L);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/standalone/history/FullHistoryTest.testVariableUpdates.bpmn20.xml")
  @SuppressWarnings("deprecation")
  void testHistoricVariableInstanceQuery() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("process", "one");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveTask", variables);
    runtimeService.signal(processInstance.getProcessInstanceId());

    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("process").count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "one").count()).isEqualTo(1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("process", "two");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("receiveTask", variables2);
    runtimeService.signal(processInstance2.getProcessInstanceId());

    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("process").count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "one").count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "two").count()).isEqualTo(1);

    HistoricVariableInstance historicProcessVariable = historyService.createHistoricVariableInstanceQuery().variableValueEquals("process", "one").singleResult();
    assertThat(historicProcessVariable.getVariableName()).isEqualTo("process");
    assertThat(historicProcessVariable.getValue()).isEqualTo("one");
    assertThat(historicProcessVariable.getVariableTypeName()).isEqualTo(ValueType.STRING.getName());
    assertThat(historicProcessVariable.getTypeName()).isEqualTo(ValueType.STRING.getName());
    assertThat(historicProcessVariable.getTypedValue().getValue()).isEqualTo(historicProcessVariable.getValue());
    assertThat(historicProcessVariable.getTypedValue().getType().getName()).isEqualTo(historicProcessVariable.getTypeName());

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("long", 1000L);
    variables3.put("double", 25.43d);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("receiveTask", variables3);
    runtimeService.signal(processInstance3.getProcessInstanceId());

    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("long").count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("long", 1000L).count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("double").count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("double", 25.43d).count()).isEqualTo(1);

  }

  @Test
  @Deployment
  @SuppressWarnings("deprecation")
  void testHistoricVariableUpdatesAllTypes() throws Exception {

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss SSS");
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "initial value");

    Date startedDate = sdf.parse("01/01/2001 01:23:45 000");

    // In the javaDelegate, the current time is manipulated
    Date updatedDate = sdf.parse("01/01/2001 01:23:46 000");

    ClockUtil.setCurrentTime(startedDate);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricVariableUpdateProcess", variables);

    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
      .variableUpdates()
      .processInstanceId(processInstance.getId())
      .orderByVariableName().asc()
      .orderByTime().asc()
      .list();

    // 8 variable updates should be present, one performed when starting process
    // the other 7 are set in VariableSetter serviceTask
    assertThat(details).hasSize(9);

    // Since we order by varName, first entry should be aVariable update from startTask
    HistoricVariableUpdate startVarUpdate = (HistoricVariableUpdate) details.get(0);
    assertThat(startVarUpdate.getVariableName()).isEqualTo("aVariable");
    assertThat(startVarUpdate.getValue()).isEqualTo("initial value");
    assertThat(startVarUpdate.getRevision()).isZero();
    assertThat(startVarUpdate.getProcessInstanceId()).isEqualTo(processInstance.getId());
    // Date should the one set when starting
    assertThat(startVarUpdate.getTime()).isEqualTo(startedDate);

    HistoricVariableUpdate updatedStringVariable = (HistoricVariableUpdate) details.get(1);
    assertThat(updatedStringVariable.getVariableName()).isEqualTo("aVariable");
    assertThat(updatedStringVariable.getValue()).isEqualTo("updated value");
    assertThat(updatedStringVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    // Date should be the updated date
    assertThat(updatedStringVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate intVariable = (HistoricVariableUpdate) details.get(2);
    assertThat(intVariable.getVariableName()).isEqualTo("bVariable");
    assertThat(intVariable.getValue()).isEqualTo(123);
    assertThat(intVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(intVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate longVariable = (HistoricVariableUpdate) details.get(3);
    assertThat(longVariable.getVariableName()).isEqualTo("cVariable");
    assertThat(longVariable.getValue()).isEqualTo(12345L);
    assertThat(longVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(longVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate doubleVariable = (HistoricVariableUpdate) details.get(4);
    assertThat(doubleVariable.getVariableName()).isEqualTo("dVariable");
    assertThat(doubleVariable.getValue()).isEqualTo(1234.567);
    assertThat(doubleVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(doubleVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate shortVariable = (HistoricVariableUpdate) details.get(5);
    assertThat(shortVariable.getVariableName()).isEqualTo("eVariable");
    assertThat(shortVariable.getValue()).isEqualTo((short) 12);
    assertThat(shortVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(shortVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate dateVariable = (HistoricVariableUpdate) details.get(6);
    assertThat(dateVariable.getVariableName()).isEqualTo("fVariable");
    assertThat(dateVariable.getValue()).isEqualTo(sdf.parse("01/01/2001 01:23:45 678"));
    assertThat(dateVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(dateVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate serializableVariable = (HistoricVariableUpdate) details.get(7);
    assertThat(serializableVariable.getVariableName()).isEqualTo("gVariable");
    assertThat(serializableVariable.getValue()).isEqualTo(new SerializableVariable("hello hello"));
    assertThat(serializableVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(serializableVariable.getTime()).isEqualTo(updatedDate);

    HistoricVariableUpdate byteArrayVariable = (HistoricVariableUpdate) details.get(8);
    assertThat(byteArrayVariable.getVariableName()).isEqualTo("hVariable");
    assertThat(new String((byte[]) byteArrayVariable.getValue())).isEqualTo(";-)");
    assertThat(byteArrayVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(byteArrayVariable.getTime()).isEqualTo(updatedDate);

    // end process instance
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());
    testHelper.assertProcessEnded(processInstance.getId());

    // check for historic process variables set
    HistoricVariableInstanceQuery historicProcessVariableQuery = historyService
            .createHistoricVariableInstanceQuery()
            .orderByVariableName().asc();

    assertThat(historicProcessVariableQuery.count()).isEqualTo(8);

    List<HistoricVariableInstance> historicVariables = historicProcessVariableQuery.list();

 // Variable status when process is finished
    HistoricVariableInstance historicVariable = historicVariables.get(0);
    assertThat(historicVariable.getVariableName()).isEqualTo("aVariable");
    assertThat(historicVariable.getValue()).isEqualTo("updated value");
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(1);
    assertThat(historicVariable.getVariableName()).isEqualTo("bVariable");
    assertThat(historicVariable.getValue()).isEqualTo(123);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(2);
    assertThat(historicVariable.getVariableName()).isEqualTo("cVariable");
    assertThat(historicVariable.getValue()).isEqualTo(12345L);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(3);
    assertThat(historicVariable.getVariableName()).isEqualTo("dVariable");
    assertThat(historicVariable.getValue()).isEqualTo(1234.567);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(4);
    assertThat(historicVariable.getVariableName()).isEqualTo("eVariable");
    assertThat(historicVariable.getValue()).isEqualTo((short) 12);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(5);
    assertThat(historicVariable.getVariableName()).isEqualTo("fVariable");
    assertThat(historicVariable.getValue()).isEqualTo(sdf.parse("01/01/2001 01:23:45 678"));
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(6);
    assertThat(historicVariable.getVariableName()).isEqualTo("gVariable");
    assertThat(historicVariable.getValue()).isEqualTo(new SerializableVariable("hello hello"));
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    historicVariable = historicVariables.get(7);
    assertThat(historicVariable.getVariableName()).isEqualTo("hVariable");
    assertThat(new String((byte[]) historicVariable.getValue())).as(";-)").isEqualTo(";-)");
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

  @Test
  @Deployment
  @SuppressWarnings("deprecation")
  void testHistoricFormProperties() throws Exception {
    Date startedDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss SSS").parse("01/01/2001 01:23:46 000");

    ClockUtil.setCurrentTime(startedDate);

    Map<String, String> formProperties = new HashMap<>();
    formProperties.put("formProp1", "Activiti rocks");
    formProperties.put("formProp2", "12345");

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("historicFormPropertiesProcess").singleResult();

    ProcessInstance processInstance = formService.submitStartFormData(procDef.getId() , formProperties);

    // Submit form-properties on the created task
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();

    // Out execution only has a single activity waiting, the task
    List<String> activityIds = runtimeService.getActiveActivityIds(task.getExecutionId());
    assertThat(activityIds)
            .isNotNull()
            .hasSize(1);

    String taskActivityId = activityIds.get(0);

    // Submit form properties
    formProperties = new HashMap<>();
    formProperties.put("formProp3", "Activiti still rocks!!!");
    formProperties.put("formProp4", "54321");
    formService.submitTaskFormData(task.getId(), formProperties);

    // 4 historic form properties should be created. 2 when process started, 2 when task completed
    List<HistoricDetail> props = historyService.createHistoricDetailQuery()
      .formProperties()
      .processInstanceId(processInstance.getId())
      .orderByFormPropertyId().asc()
      .list();

    HistoricFormProperty historicProperty1 = (HistoricFormProperty) props.get(0);
    assertThat(historicProperty1.getPropertyId()).isEqualTo("formProp1");
    assertThat(historicProperty1.getPropertyValue()).isEqualTo("Activiti rocks");
    assertThat(historicProperty1.getTime()).isEqualTo(startedDate);
    assertThat(historicProperty1.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicProperty1.getTaskId()).isNull();

    assertThat(historicProperty1.getActivityInstanceId()).isEqualTo(processInstance.getId());

    HistoricFormProperty historicProperty2 = (HistoricFormProperty) props.get(1);
    assertThat(historicProperty2.getPropertyId()).isEqualTo("formProp2");
    assertThat(historicProperty2.getPropertyValue()).isEqualTo("12345");
    assertThat(historicProperty2.getTime()).isEqualTo(startedDate);
    assertThat(historicProperty2.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(historicProperty2.getTaskId()).isNull();

    assertThat(historicProperty2.getActivityInstanceId()).isEqualTo(processInstance.getId());

    HistoricFormProperty historicProperty3 = (HistoricFormProperty) props.get(2);
    assertThat(historicProperty3.getPropertyId()).isEqualTo("formProp3");
    assertThat(historicProperty3.getPropertyValue()).isEqualTo("Activiti still rocks!!!");
    assertThat(historicProperty3.getTime()).isEqualTo(startedDate);
    assertThat(historicProperty3.getProcessInstanceId()).isEqualTo(processInstance.getId());
    String activityInstanceId = historicProperty3.getActivityInstanceId();
    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityInstanceId(activityInstanceId).singleResult();
    assertThat(historicActivityInstance).isNotNull();
    assertThat(historicActivityInstance.getActivityId()).isEqualTo(taskActivityId);
    assertThat(historicProperty3.getTaskId()).isNotNull();

    HistoricFormProperty historicProperty4 = (HistoricFormProperty) props.get(3);
    assertThat(historicProperty4.getPropertyId()).isEqualTo("formProp4");
    assertThat(historicProperty4.getPropertyValue()).isEqualTo("54321");
    assertThat(historicProperty4.getTime()).isEqualTo(startedDate);
    assertThat(historicProperty4.getProcessInstanceId()).isEqualTo(processInstance.getId());
    activityInstanceId = historicProperty4.getActivityInstanceId();
    historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityInstanceId(activityInstanceId).singleResult();
    assertThat(historicActivityInstance).isNotNull();
    assertThat(historicActivityInstance.getActivityId()).isEqualTo(taskActivityId);
    assertThat(historicProperty4.getTaskId()).isNotNull();

    assertThat(props).hasSize(4);
  }

  @Test
  @Deployment(
    resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  void testHistoricVariableQuery() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "activiti rocks!");
    variables.put("longVar", 12345L);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // Query on activity-instance, activity instance null will return all vars set when starting process
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().activityInstanceId(null).count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().activityInstanceId("unexisting").count()).isZero();

    // Query on process-instance
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId("unexisting").count()).isZero();

    // Query both process-instance and activity-instance
    assertThat(historyService.createHistoricDetailQuery().variableUpdates()
        .activityInstanceId(null)
        .processInstanceId(processInstance.getId()).count()).isEqualTo(2);

    // end process instance
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());
    testHelper.assertProcessEnded(processInstance.getId());

    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

    // Query on process-instance
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId("unexisting").count()).isZero();
  }

  @Test
  @Deployment(
    resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  void testHistoricVariableQueryExcludeTaskRelatedDetails() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "activiti rocks!");
    variables.put("longVar", 12345L);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // Set a local task-variable
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();
    taskService.setVariableLocal(task.getId(), "taskVar", "It is I, le Variable");

    // Query on process-instance
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

    // Query on process-instance, excluding task-details
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId())
        .excludeTaskDetails().count()).isEqualTo(2);

    // Check task-id precedence on excluding task-details
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId())
        .excludeTaskDetails().taskId(task.getId()).count()).isEqualTo(1);
  }

  @Test
  @Deployment(
    resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @SuppressWarnings("deprecation")
  void testHistoricFormPropertiesQuery() {
    Map<String, String> formProperties = new HashMap<>();
    formProperties.put("stringVar", "activiti rocks!");
    formProperties.put("longVar", "12345");

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
    ProcessInstance processInstance = formService.submitStartFormData(procDef.getId() , formProperties);

    // Query on activity-instance, activity instance null will return all vars set when starting process
    assertThat(historyService.createHistoricDetailQuery().formProperties().activityInstanceId(null).count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().activityInstanceId("unexisting").count()).isZero();

    // Query on process-instance
    assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId("unexisting").count()).isZero();

    // Complete the task by submitting the task properties
    Task task = taskService.createTaskQuery().singleResult();
    formProperties = new HashMap<>();
    formProperties.put("taskVar", "task form property");
    formService.submitTaskFormData(task.getId(), formProperties);

    assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
    assertThat(historyService.createHistoricDetailQuery().formProperties().processInstanceId("unexisting").count()).isZero();
  }

  @Test
  @Deployment(
    resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  void testHistoricVariableQuerySorting() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "activiti rocks!");
    variables.put("longVar", 12345L);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().asc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().asc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().asc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().asc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().asc().count()).isEqualTo(2);

    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().desc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().desc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().desc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().desc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().desc().count()).isEqualTo(2);

    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().asc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().asc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().asc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().asc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().asc().list()).hasSize(2);

    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByProcessInstanceId().desc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByTime().desc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableName().desc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableRevision().desc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().orderByVariableType().desc().list()).hasSize(2);
  }

  @Test
  @Deployment(
    resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  @SuppressWarnings("deprecation")
  void testHistoricFormPropertySorting() {

    Map<String, String> formProperties = new HashMap<>();
    formProperties.put("stringVar", "activiti rocks!");
    formProperties.put("longVar", "12345");

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
    formService.submitStartFormData(procDef.getId() , formProperties);

    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().asc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().asc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().asc().count()).isEqualTo(2);

    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().desc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().desc().count()).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().desc().count()).isEqualTo(2);

    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().asc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().asc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().asc().list()).hasSize(2);

    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByProcessInstanceId().desc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByTime().desc().list()).hasSize(2);
    assertThat(historyService.createHistoricDetailQuery().formProperties().orderByFormPropertyId().desc().list()).hasSize(2);
  }

  @Test
  @Deployment
  @SuppressWarnings("deprecation")
  void testHistoricDetailQueryMixed() {

    Map<String, String> formProperties = new HashMap<>();
    formProperties.put("formProp1", "activiti rocks!");
    formProperties.put("formProp2", "12345");

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("historicDetailMixed").singleResult();
    ProcessInstance processInstance = formService.submitStartFormData(procDef.getId() , formProperties);

    List<HistoricDetail> details = historyService
      .createHistoricDetailQuery()
      .processInstanceId(processInstance.getId())
      .orderByVariableName().asc()
      .list();

    assertThat(details).hasSize(4);

    assertThat(details.get(0)).isInstanceOf(HistoricFormProperty.class);
    HistoricFormProperty formProp1 = (HistoricFormProperty) details.get(0);
    assertThat(formProp1.getPropertyId()).isEqualTo("formProp1");
    assertThat(formProp1.getPropertyValue()).isEqualTo("activiti rocks!");

    assertThat(details.get(1)).isInstanceOf(HistoricFormProperty.class);
    HistoricFormProperty formProp2 = (HistoricFormProperty) details.get(1);
    assertThat(formProp2.getPropertyId()).isEqualTo("formProp2");
    assertThat(formProp2.getPropertyValue()).isEqualTo("12345");


    assertThat(details.get(2)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate varUpdate1 = (HistoricVariableUpdate) details.get(2);
    assertThat(varUpdate1.getVariableName()).isEqualTo("variable1");
    assertThat(varUpdate1.getValue()).isEqualTo("activiti rocks!");


    // This variable should be of type LONG since this is defined in the process-definition
    assertThat(details.get(3)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate varUpdate2 = (HistoricVariableUpdate) details.get(3);
    assertThat(varUpdate2.getVariableName()).isEqualTo("variable2");
    assertThat(varUpdate2.getValue()).isEqualTo(12345L);
  }

  @Test
  void testHistoricDetailQueryInvalidSorting() {
    var historicDetailQuery = historyService.createHistoricDetailQuery();
    try {
      historicDetailQuery.asc();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      historicDetailQuery.desc();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }

    HistoricDetailQuery queryOrderByProcessInstanceId = historicDetailQuery.orderByProcessInstanceId();
    try {
      queryOrderByProcessInstanceId.list();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }

    HistoricDetailQuery queryOrderByTime = historicDetailQuery.orderByTime();
    try {
      queryOrderByTime.list();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }

    HistoricDetailQuery queryOrderByVariableName = historicDetailQuery.orderByVariableName();
    try {
      queryOrderByVariableName.list();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }

    HistoricDetailQuery queryOrderByVariableRevision = historicDetailQuery.orderByVariableRevision();
    try {
      queryOrderByVariableRevision.list();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }

    HistoricDetailQuery queryByVariableType = historicDetailQuery.orderByVariableType();
    try {
      queryByVariableType.list();
      fail("");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  @Deployment
  void testHistoricTaskInstanceVariableUpdates() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest").getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    runtimeService.setVariable(processInstanceId, "deadline", "yesterday");

    taskService.setVariableLocal(taskId, "bucket", "23c");
    taskService.setVariableLocal(taskId, "mop", "37i");

    taskService.complete(taskId);

    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);

    List<HistoricDetail> historicTaskVariableUpdates = historyService.createHistoricDetailQuery()
      .taskId(taskId)
      .variableUpdates()
      .orderByVariableName().asc()
      .list();

    assertThat(historicTaskVariableUpdates).hasSize(2);

    historyService.deleteHistoricTaskInstance(taskId);

    // Check if the variable updates have been removed as well
    historicTaskVariableUpdates = historyService.createHistoricDetailQuery()
      .taskId(taskId)
      .variableUpdates()
      .orderByVariableName().asc()
      .list();

    assertThat(historicTaskVariableUpdates).isEmpty();
  }

  // ACT-592
  @Test
  @Deployment
  void testSetVariableOnProcessInstanceWithTimer() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerVariablesProcess");
    runtimeService.setVariable(processInstance.getId(), "myVar", 123456L);
    assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo(123456L);
  }

  @Test
  @Deployment
  void testDeleteHistoricProcessInstance() {
    // Start process-instance with some variables set
    Map<String, Object> vars = new HashMap<>();
    vars.put("processVar", 123L);
    vars.put("anotherProcessVar", new DummySerializable());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest", vars);
    assertThat(processInstance).isNotNull();

    // Set 2 task properties
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setVariableLocal(task.getId(), "taskVar", 45678);
    taskService.setVariableLocal(task.getId(), "anotherTaskVar", "value");

    // Finish the task, this end the process-instance
    taskService.complete(task.getId());

    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);
    assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

    // Delete the historic process-instance
    historyService.deleteHistoricProcessInstance(processInstance.getId());

    // Verify no traces are left in the history tables
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

    try {
      // Delete the historic process-instance, which is still running
      historyService.deleteHistoricProcessInstance("unexisting");
      fail("Exception expected when deleting process-instance that is still running");
    } catch(ProcessEngineException ae) {
      // Expected exception
      assertThat(ae.getMessage()).contains("No historic process instance found with id: unexisting");
    }
  }

  @Test
  @Deployment
  void testDeleteRunningHistoricProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");
    assertThat(processInstance).isNotNull();
    var processInstanceId = processInstance.getId();

    try {
      // Delete the historic process-instance, which is still running
      historyService.deleteHistoricProcessInstance(processInstanceId);
      fail("Exception expected when deleting process-instance that is still running");
    } catch(ProcessEngineException ae) {
      // Expected exception
      assertThat(ae.getMessage()).contains("Process instance is still running, cannot delete historic process instance");
    }
  }

  @Test
  @Deployment
  void testDeleteCachedHistoricDetails() {
    final String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();


    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {
      Map<String, Object> formProperties = new HashMap<>();
      formProperties.put("formProp1", "value1");

      ProcessInstance processInstance = new SubmitStartFormCmd(processDefinitionId, null, formProperties).execute(commandContext);

      // two historic details should be in cache: one form property and one variable update
      commandContext.getHistoricDetailManager().deleteHistoricDetailsByProcessInstanceIds(List.of(processInstance.getId()));
      return null;
    });

    // the historic process instance should still be there
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(1);

    // the historic details should be deleted
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
  }

  /**
   * Test created to validate ACT-621 fix.
   */
  @Test
  @Deployment
  @SuppressWarnings("deprecation")
  void testHistoricFormPropertiesOnReEnteringActivity() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("comeBack", Boolean.TRUE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricFormPropertiesProcess", variables);
    assertThat(processInstance).isNotNull();

    // Submit form on task
    Map<String, String> data = new HashMap<>();
    data.put("formProp1", "Property value");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    formService.submitTaskFormData(task.getId(), data);

    // Historic property should be available
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
      .formProperties()
      .processInstanceId(processInstance.getId())
      .list();
    assertThat(details)
            .isNotNull()
            .hasSize(1);

    // Task should be active in the same activity as the previous one
    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    formService.submitTaskFormData(task.getId(), data);

    details = historyService.createHistoricDetailQuery()
      .formProperties()
      .processInstanceId(processInstance.getId())
      .list();
    assertThat(details)
            .isNotNull()
            .hasSize(2);

    // Should have 2 different historic activity instance ID's, with the same activityId
    Assertions.assertNotSame(details.get(0).getActivityInstanceId(), details.get(1).getActivityInstanceId());

    HistoricActivityInstance historicActInst1 = historyService.createHistoricActivityInstanceQuery()
      .activityInstanceId(details.get(0).getActivityInstanceId())
      .singleResult();

    HistoricActivityInstance historicActInst2 = historyService.createHistoricActivityInstanceQuery()
      .activityInstanceId(details.get(1).getActivityInstanceId())
      .singleResult();

    assertThat(historicActInst2.getActivityId()).isEqualTo(historicActInst1.getActivityId());
  }

  @Test
  @Deployment
  void testHistoricTaskInstanceQueryTaskVariableValueEquals() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set some variables on the task
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 12345L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    taskService.setVariablesLocal(task.getId(), variables);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().taskId(task.getId()).count()).isEqualTo(7);

    // Query Historic task instances based on variable
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("longVar", 12345L).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("dateVar", date).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("nullVar", null).count()).isEqualTo(1);

    // Update the variables
    variables.put("longVar", 67890L);
    variables.put("shortVar", (short) 456);
    variables.put("integerVar", 5678);
    variables.put("stringVar", "updatedStringValue");
    variables.put("booleanVar", false);
    Calendar otherCal = Calendar.getInstance();
    otherCal.add(Calendar.DAY_OF_MONTH, 1);
    Date otherDate = otherCal.getTime();
    variables.put("dateVar", otherDate);
    variables.put("nullVar", null);

    taskService.setVariablesLocal(task.getId(), variables);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().taskId(task.getId()).count()).isEqualTo(14);

    // Previous values should NOT match
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("longVar", 12345L).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("integerVar", 1234).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("booleanVar", true).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("dateVar", date).count()).isZero();

    // New values should match
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("longVar", 67890L).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("shortVar", (short) 456).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("integerVar", 5678).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("stringVar", "updatedStringValue").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("booleanVar", false).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("dateVar", otherDate).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("nullVar", null).count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testHistoricTaskInstanceQueryTaskVariableValueEqualsOverwriteType() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set a long variable on a task
    taskService.setVariableLocal(task.getId(), "var", 12345L);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().taskId(task.getId()).count()).isEqualTo(1);

    // Query Historic task instances based on variable
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", 12345L).count()).isEqualTo(1);

    // Update the variables to an int variable
    taskService.setVariableLocal(task.getId(), "var", 12345);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().taskId(task.getId()).count()).isEqualTo(2);

    // The previous long value should not match
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", 12345L).count()).isZero();

    // The previous int value should not match
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", 12345).count()).isEqualTo(1);
  }

  @Test
  @Deployment
  void testHistoricTaskInstanceQueryVariableInParallelBranch() {
    runtimeService.startProcessInstanceByKey("parallelGateway");

    // when there are two process variables of the same name but different types
    Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
    runtimeService.setVariableLocal(task1Execution.getId(), "var", 12345L);
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    runtimeService.setVariableLocal(task2Execution.getId(), "var", 12345);

    // then the task query should be able to filter by both variables and return both tasks
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", 12345).count()).isEqualTo(2);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", 12345L).count()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/standalone/history/FullHistoryTest.testHistoricTaskInstanceQueryVariableInParallelBranch.bpmn20.xml")
  void testHistoricTaskInstanceQueryVariableOfSameTypeInParallelBranch() {
    runtimeService.startProcessInstanceByKey("parallelGateway");

    // when there are two process variables of the same name but different types
    Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
    runtimeService.setVariableLocal(task1Execution.getId(), "var", 12345L);
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    runtimeService.setVariableLocal(task2Execution.getId(), "var", 45678L);

    // then the task query should be able to filter by both variables and return both tasks
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", 12345L).count()).isEqualTo(2);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", 45678L).count()).isEqualTo(2);
  }

  @Test
  @Deployment
  void testHistoricTaskInstanceQueryProcessVariableValueEquals() {
    // Set some variables on the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 12345L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest", variables);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

    // Query Historic task instances based on process variable
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 12345L).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("dateVar", date).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("nullVar", null).count()).isEqualTo(1);

    // Update the variables
    variables.put("longVar", 67890L);
    variables.put("shortVar", (short) 456);
    variables.put("integerVar", 5678);
    variables.put("stringVar", "updatedStringValue");
    variables.put("booleanVar", false);
    Calendar otherCal = Calendar.getInstance();
    otherCal.add(Calendar.DAY_OF_MONTH, 1);
    Date otherDate = otherCal.getTime();
    variables.put("dateVar", otherDate);
    variables.put("nullVar", null);

    runtimeService.setVariables(processInstance.getId(), variables);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(14);

    // Previous values should NOT match
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 12345L).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("shortVar", (short) 123).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("integerVar", 1234).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("stringVar", "stringValue").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("booleanVar", true).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("dateVar", date).count()).isZero();

    // New values should match
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 67890L).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("shortVar", (short) 456).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("integerVar", 5678).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("stringVar", "updatedStringValue").count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("booleanVar", false).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("dateVar", otherDate).count()).isEqualTo(1);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("nullVar", null).count()).isEqualTo(1);

    // Set a task-variables, shouldn't affect the process-variable matches
    taskService.setVariableLocal(task.getId(), "longVar", 9999L);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 9999L).count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("longVar", 67890L).count()).isEqualTo(1);
  }

  @Test
  @Deployment
  void testHistoricProcessInstanceVariableValueEquals() {
    // Set some variables on the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 12345L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricProcessInstanceTest", variables);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/standalone/history/FullHistoryTest.testHistoricProcessInstanceVariableValueEquals.bpmn20.xml"})
  void testHistoricProcessInstanceVariableValueNotEquals() {
    // Set some variables on the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 12345L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    Calendar otherCal = Calendar.getInstance();
    otherCal.add(Calendar.DAY_OF_MONTH, 1);
    Date otherDate = otherCal.getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricProcessInstanceTest", variables);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

    // Query Historic process instances based on process variable, shouldn't match
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("longVar", 12345L).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("shortVar", (short) 123).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("integerVar", 1234).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "stringValue").count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("booleanVar", true).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", date).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("nullVar", null).count()).isZero();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/standalone/history/FullHistoryTest.testHistoricProcessInstanceVariableValueEquals.bpmn20.xml"})
  void testHistoricProcessInstanceVariableValueLessThanAndGreaterThan() {
    // Set some variables on the process instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 12345L);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricProcessInstanceTest", variables);

    // Validate all variable-updates are present in DB
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("longVar", 12345L).count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/standalone/history/FullHistoryTest.testVariableUpdatesAreLinkedToActivity.bpmn20.xml"})
  void testVariableUpdatesLinkedToActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("ProcessWithSubProcess");

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    Map<String, Object> variables = new HashMap<>();
    variables.put("test", "1");
    taskService.complete(task.getId(), variables);

    // now we are in the subprocess
    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    variables.clear();
    variables.put("test", "2");
    taskService.complete(task.getId(), variables);

    // now we are ended
    testHelper.assertProcessEnded(pi.getId());

    // check history
    List<HistoricDetail> updates = historyService.createHistoricDetailQuery().variableUpdates().list();
    assertThat(updates).hasSize(2);

    Map<String, HistoricVariableUpdate> updatesMap = new HashMap<>();
    HistoricVariableUpdate update = (HistoricVariableUpdate) updates.get(0);
    updatesMap.put((String)update.getValue(), update);
    update = (HistoricVariableUpdate) updates.get(1);
    updatesMap.put((String)update.getValue(), update);

    HistoricVariableUpdate update1 = updatesMap.get("1");
    HistoricVariableUpdate update2 = updatesMap.get("2");

    assertThat(update1.getActivityInstanceId()).isNotNull();
    assertThat(update1.getExecutionId()).isNotNull();
    HistoricActivityInstance historicActivityInstance1 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update1.getActivityInstanceId()).singleResult();
    assertThat(update1.getExecutionId()).isEqualTo(historicActivityInstance1.getExecutionId());
    assertThat(historicActivityInstance1.getActivityId()).isEqualTo("usertask1");

    assertThat(update2.getActivityInstanceId()).isNotNull();
    HistoricActivityInstance historicActivityInstance2 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update2.getActivityInstanceId()).singleResult();
    assertThat(historicActivityInstance2.getActivityId()).isEqualTo("usertask2");

    /*
     * This is OK! The variable is set on the root execution, on an execution never run through the activity, where the process instances
     * stands when calling the set Variable. But the ActivityId of this flow node is used. So the execution id's doesn't have to be equal.
     *
     * execution id: On which execution it was set
     * activity id: in which activity was the process instance when setting the variable
     */
    assertThat(update2.getExecutionId()).isNotEqualTo(historicActivityInstance2.getExecutionId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml"})
  void testHistoricDetailQueryByVariableInstanceId() {
    Map<String, Object> params = new HashMap<>();
    params.put("testVar", "testValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", params);

    HistoricVariableInstance testVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("testVar")
      .singleResult();

    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    query.variableInstanceId(testVariable.getId());

    assertThat(query.count()).isEqualTo(1);
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testHistoricDetailQueryByInvalidVariableInstanceId() {
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    query.variableInstanceId("invalid");
    assertThat(query.count()).isZero();

    try {
      query.variableInstanceId(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      query.variableInstanceId((String)null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  @Deployment
  void testHistoricDetailActivityInstanceIdForInactiveScopeExecution() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.setVariable(pi.getId(), "foo", "bar");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery().singleResult();
    assertThat(historicDetail.getActivityInstanceId()).isNotNull();
    assertThat(historicDetail.getActivityInstanceId()).isNotEqualTo(pi.getId());
  }

  @Test
  @Deployment
  void testHistoricDetailActivityInstanceIdForInactiveScopeExecutionAsyncBefore() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.setVariable(pi.getId(), "foo", "bar");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery().singleResult();
    assertThat(historicDetail.getActivityInstanceId()).isNotNull();
    assertThat(historicDetail.getActivityInstanceId()).isNotEqualTo(pi.getId());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testHistoricDetailQueryById() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "someName";
    String variableValue = "someValue";
    taskService.setVariable(newTask.getId(), variableName, variableValue);

    HistoricDetail result = historyService.createHistoricDetailQuery()
      .singleResult();

    HistoricDetail resultById = historyService.createHistoricDetailQuery().detailId(result.getId()).singleResult();
    assertThat(resultById).isNotNull();
    assertThat(resultById.getId()).isEqualTo(result.getId());
    assertThat(((HistoricVariableUpdate) resultById).getVariableName()).isEqualTo(variableName);
    assertThat(((HistoricVariableUpdate) resultById).getValue()).isEqualTo(variableValue);
    assertThat(((HistoricVariableUpdate) resultById).getVariableTypeName()).isEqualTo(ValueType.STRING.getName());
    assertThat(((HistoricVariableUpdate) resultById).getTypeName()).isEqualTo(ValueType.STRING.getName());

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testHistoricDetailQueryByNonExistingId() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "someName";
    String variableValue = "someValue";
    taskService.setVariable(newTask.getId(), variableName, variableValue);

    HistoricDetail result = historyService.createHistoricDetailQuery().detailId("non-existing").singleResult();
    assertThat(result).isNull();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testBinaryFetchingEnabled() {

    // by default, binary fetching is enabled

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "binaryVariableName";
    taskService.setVariable(newTask.getId(), variableName, "some bytes".getBytes());

    HistoricDetail result = historyService.createHistoricDetailQuery()
      .variableUpdates()
      .singleResult();

    assertThat(((HistoricVariableUpdate) result).getValue()).isNotNull();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testBinaryFetchingDisabled() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "binaryVariableName";
    taskService.setVariable(newTask.getId(), variableName, "some bytes".getBytes());

    HistoricDetail result = historyService.createHistoricDetailQuery()
      .disableBinaryFetching()
      .variableUpdates()
      .singleResult();

    assertThat(((HistoricVariableUpdate) result).getValue()).isNull();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
  void testDisableBinaryFetchingForFileValues() {
    // given
    String fileName = "text.txt";
    String encoding = "crazy-encoding";
    String mimeType = "martini/dry";

    FileValue fileValue = Variables
        .fileValue(fileName)
        .file("ABC".getBytes())
        .encoding(encoding)
        .mimeType(mimeType)
        .create();

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValueTyped("fileVar", fileValue));

    // when enabling binary fetching
    HistoricVariableUpdate fileVariableInstance =
        (HistoricVariableUpdate) historyService.createHistoricDetailQuery().singleResult();

    // then the binary value is accessible
    assertThat(fileVariableInstance.getValue()).isNotNull();

    // when disabling binary fetching
    fileVariableInstance =
        (HistoricVariableUpdate) historyService.createHistoricDetailQuery().disableBinaryFetching().singleResult();

    // then the byte value is not fetched
    assertThat(fileVariableInstance).isNotNull();
    assertThat(fileVariableInstance.getVariableName()).isEqualTo("fileVar");

    assertThat(fileVariableInstance.getValue()).isNull();

    FileValue typedValue = (FileValue) fileVariableInstance.getTypedValue();
    assertThat(typedValue.getValue()).isNull();

    // but typed value metadata is accessible
    assertThat(typedValue.getType()).isEqualTo(ValueType.FILE);
    assertThat(typedValue.getFilename()).isEqualTo(fileName);
    assertThat(typedValue.getEncoding()).isEqualTo(encoding);
    assertThat(typedValue.getMimeType()).isEqualTo(mimeType);

  }

  @Test
  void testDisableCustomObjectDeserialization() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    Map<String, Object> variables = new HashMap<>();
    variables.put("customSerializable", new CustomSerializable());
    variables.put("failingSerializable", new FailingSerializable());
    taskService.setVariables(newTask.getId(), variables);

    List<HistoricDetail> results = historyService.createHistoricDetailQuery()
        .disableBinaryFetching()
        .disableCustomObjectDeserialization()
        .variableUpdates()
        .list();

    // both variables are not deserialized, but their serialized values are available
    assertThat(results).hasSize(2);

    for (HistoricDetail update : results) {
      HistoricVariableUpdate variableUpdate = (HistoricVariableUpdate) update;
      assertThat(variableUpdate.getErrorMessage()).isNull();

      ObjectValue typedValue = (ObjectValue) variableUpdate.getTypedValue();
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.isDeserialized()).isFalse();
      // cannot access the deserialized value
      try {
        typedValue.getValue();
      }
      catch(IllegalStateException e) {
        assertThat(e.getMessage()).contains("Object is not deserialized");
      }
      assertThat(typedValue.getValueSerialized()).isNotNull();
    }

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testErrorMessage() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "failingSerializable";
    taskService.setVariable(newTask.getId(), variableName, new FailingSerializable());

    HistoricDetail result = historyService.createHistoricDetailQuery()
        .disableBinaryFetching()
        .variableUpdates()
        .singleResult();

    assertThat(((HistoricVariableUpdate) result).getValue()).isNull();
    assertThat(((HistoricVariableUpdate) result).getErrorMessage()).isNotNull();

    taskService.deleteTask(newTask.getId(), true);

  }

  @Test
  void testVariableInstance() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "someName";
    String variableValue = "someValue";
    taskService.setVariable(newTask.getId(), variableName, variableValue);

    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .singleResult();
    assertThat(variable).isNotNull();

    HistoricVariableUpdate result = (HistoricVariableUpdate) historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .singleResult();
    assertThat(result).isNotNull();

    assertThat(result.getVariableInstanceId()).isEqualTo(variable.getId());

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testHistoricVariableUpdateProcessDefinitionProperty() {
    // given
    String key = "oneTaskProcess";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);

    String processInstanceId = processInstance.getId();
    String taskId = taskService.createTaskQuery().singleResult().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "aValue");
    taskService.setVariableLocal(taskId, "aLocalVariable", "anotherValue");

    String firstVariable = runtimeService
        .createVariableInstanceQuery()
        .variableName("aVariable")
        .singleResult()
        .getId();

    String secondVariable = runtimeService
        .createVariableInstanceQuery()
        .variableName("aLocalVariable")
        .singleResult()
        .getId();

    // when (1)
    HistoricVariableUpdate instance = (HistoricVariableUpdate) historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(firstVariable)
        .singleResult();

    // then (1)
    assertThat(instance.getProcessDefinitionKey()).isNotNull();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(key);

    assertThat(instance.getProcessDefinitionId()).isNotNull();
    assertThat(instance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();

    // when (2)
    instance = (HistoricVariableUpdate) historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(secondVariable)
        .singleResult();

    // then (2)
    assertThat(instance.getProcessDefinitionKey()).isNotNull();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(key);

    assertThat(instance.getProcessDefinitionId()).isNotNull();
    assertThat(instance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  void testHistoricVariableUpdateCaseDefinitionProperty() {
    // given
    String key = "oneTaskCase";
    CaseInstance caseInstance = caseService.createCaseInstanceByKey(key);

    String caseInstanceId = caseInstance.getId();

    String humanTask = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();
    String taskId = taskService.createTaskQuery().singleResult().getId();

    caseService.setVariable(caseInstanceId, "aVariable", "aValue");
    taskService.setVariableLocal(taskId, "aLocalVariable", "anotherValue");

    String firstVariable = runtimeService
        .createVariableInstanceQuery()
        .variableName("aVariable")
        .singleResult()
        .getId();

    String secondVariable = runtimeService
        .createVariableInstanceQuery()
        .variableName("aLocalVariable")
        .singleResult()
        .getId();


    // when (1)
    HistoricVariableUpdate instance = (HistoricVariableUpdate) historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(firstVariable)
        .singleResult();

    // then (1)
    assertThat(instance.getCaseDefinitionKey()).isNotNull();
    assertThat(instance.getCaseDefinitionKey()).isEqualTo(key);

    assertThat(instance.getCaseDefinitionId()).isNotNull();
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());

    assertThat(instance.getProcessDefinitionKey()).isNull();
    assertThat(instance.getProcessDefinitionId()).isNull();

    // when (2)
    instance = (HistoricVariableUpdate) historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(secondVariable)
        .singleResult();

    // then (2)
    assertThat(instance.getCaseDefinitionKey()).isNotNull();
    assertThat(instance.getCaseDefinitionKey()).isEqualTo(key);

    assertThat(instance.getCaseDefinitionId()).isNotNull();
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());

    assertThat(instance.getProcessDefinitionKey()).isNull();
    assertThat(instance.getProcessDefinitionId()).isNull();
  }

  @Test
  void testHistoricVariableUpdateStandaloneTaskDefinitionProperties() {
    // given
    String taskId = "myTask";
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);

    taskService.setVariable(taskId, "aVariable", "anotherValue");

    String firstVariable = runtimeService
        .createVariableInstanceQuery()
        .variableName("aVariable")
        .singleResult()
        .getId();

    // when
    HistoricVariableUpdate instance = (HistoricVariableUpdate) historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(firstVariable)
        .singleResult();

    // then
    assertThat(instance.getProcessDefinitionKey()).isNull();
    assertThat(instance.getProcessDefinitionId()).isNull();
    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();

    taskService.deleteTask(taskId, true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testHistoricFormFieldProcessDefinitionProperty() {
    // given
    String key = "oneTaskProcess";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    formService.submitTaskForm(taskId, Variables.createVariables().putValue("aVariable", "aValue"));

    // when
    HistoricFormField instance = (HistoricFormField) historyService
        .createHistoricDetailQuery()
        .formFields()
        .singleResult();

    // then
    assertThat(instance.getProcessDefinitionKey()).isNotNull();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(key);

    assertThat(instance.getProcessDefinitionId()).isNotNull();
    assertThat(instance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testDeleteProcessInstanceSkipCustomListener() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null, true);

    // then
    HistoricProcessInstance instance = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();
    assertThat(instance).isNotNull();

    assertThat(instance.getId()).isEqualTo(processInstanceId);
    assertThat(instance.getEndTime()).isNotNull();
  }

}
