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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper.ThreadControl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
class JobExecutorShutdownTest {

  protected static final BpmnModelInstance TWO_ASYNC_TASKS = Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask("task1")
      .operatonClass(SyncDelegate.class.getName())
      .operatonAsyncBefore()
      .operatonExclusive(true)
      .serviceTask("task2")
      .operatonClass(SyncDelegate.class.getName())
      .operatonAsyncBefore()
      .operatonExclusive(true)
      .endEvent()
      .done();

  protected static final BpmnModelInstance SINGLE_ASYNC_TASK = Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask("task1")
      .operatonClass(SyncDelegate.class.getName())
      .operatonAsyncBefore()
      .operatonExclusive(true)
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterEachTest()
    .configurator(configuration -> {
      configuration.setJobExecutor(buildControllableJobExecutor());
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ControllableJobExecutor jobExecutor;
  protected ThreadControl acquisitionThread;
  protected static ThreadControl executionThread;

  @BeforeEach
  void setUp() {
    jobExecutor = (ControllableJobExecutor)
        ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration()).getJobExecutor();
    jobExecutor.setMaxJobsPerAcquisition(2);
    acquisitionThread = jobExecutor.getAcquisitionThreadControl();
    executionThread = jobExecutor.getExecutionThreadControl();
  }

  @AfterEach
  void shutdownJobExecutor() {
    jobExecutor.shutdown();
  }

  @Test
  void testConcurrentShutdownAndExclusiveFollowUpJob() {
    // given
    Deployment deployment = engineRule.getRepositoryService()
        .createDeployment()
        .addModelInstance("foo.bpmn", TWO_ASYNC_TASKS)
        .deploy();
    engineRule.manageDeployment(deployment);

    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    Job firstAsyncJob = engineRule.getManagementService().createJobQuery().singleResult();

    jobExecutor.start();

    // wait before acquisition
    acquisitionThread.waitForSync();
    // wait for no more acquisition syncs
    acquisitionThread.ignoreFutureSyncs();
    acquisitionThread.makeContinue();

    // when waiting during execution of first job
    executionThread.waitForSync();

    // and shutting down the job executor
    jobExecutor.shutdown();

    // and continuing job execution
    executionThread.waitUntilDone();

    // then the current job has completed successfully
    assertThat(engineRule.getManagementService().createJobQuery().jobId(firstAsyncJob.getId()).count()).isZero();

    // but the exclusive follow-up job is not executed and is not locked
    JobEntity secondAsyncJob = (JobEntity) engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(secondAsyncJob).isNotNull();
    assertThat(firstAsyncJob.getId()).isNotEqualTo(secondAsyncJob.getId());
    assertThat(secondAsyncJob.getLockOwner()).isNull();
    assertThat(secondAsyncJob.getLockExpirationTime()).isNull();

  }

  @Test
  void testShutdownAndMultipleLockedJobs() {
    // given
    Deployment deployment = engineRule.getRepositoryService()
        .createDeployment()
        .addModelInstance("foo.bpmn", SINGLE_ASYNC_TASK)
        .deploy();
    engineRule.manageDeployment(deployment);

    // add two jobs by starting two process instances
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    jobExecutor.start();

    // wait before acquisition
    acquisitionThread.waitForSync();
    // wait for no more acquisition syncs
    acquisitionThread.ignoreFutureSyncs();

    acquisitionThread.makeContinue();

    // when waiting during execution of first job
    executionThread.waitForSync();

    // jobs must now be locked
    List<Job> lockedJobList = engineRule.getManagementService().createJobQuery().list();
    assertThat(lockedJobList).hasSize(2);
    for(Job job : lockedJobList) {
      JobEntity jobEntity = (JobEntity)job;
      assertThat(jobEntity.getLockOwner()).isNotNull();
    }

    // shut down the job executor while first job is executing
    jobExecutor.shutdown();

    // then let first job continue
    executionThread.waitUntilDone();

    // check that only one job left, which is not executed nor locked
    JobEntity jobEntity = (JobEntity) engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(jobEntity).isNotNull();
    assertThat(lockedJobList.get(1).getId().equals(jobEntity.getId()) || lockedJobList.get(0).getId().equals(jobEntity.getId())).isTrue();
    assertThat(jobEntity.getLockOwner()).isNull();
    assertThat(jobEntity.getLockExpirationTime()).isNull();
  }

  protected static ControllableJobExecutor buildControllableJobExecutor() {
    ControllableJobExecutor jobExecutor = new ControllableJobExecutor();
    jobExecutor.setMaxJobsPerAcquisition(2);
    jobExecutor.proceedAndWaitOnShutdown(false);
    return jobExecutor;
  }

  public static class SyncDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      executionThread.sync();
    }

  }

}
