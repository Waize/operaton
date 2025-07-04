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
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

class BatchJobPriorityRangeTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(rule);
  BatchMigrationHelper helper = new BatchMigrationHelper(rule, migrationRule);

  ProcessEngineConfigurationImpl configuration;

  protected long defaultBatchJobPriority;
  protected int defaultBatchJobsPerSeed;
  protected long defaultJobExecutorPriorityRangeMin;
  protected long defaultJobExecutorPriorityRangeMax;
  protected boolean defaultIsJobExecutorAcquireByPriority;

  @BeforeEach
  void setup() {
    defaultBatchJobPriority = configuration.getHistoryCleanupJobPriority();
    defaultBatchJobsPerSeed = configuration.getBatchJobsPerSeed();
    defaultBatchJobPriority = configuration.getBatchJobPriority();
    defaultJobExecutorPriorityRangeMin = configuration.getJobExecutorPriorityRangeMin();
    defaultJobExecutorPriorityRangeMax = configuration.getJobExecutorPriorityRangeMax();
    defaultIsJobExecutorAcquireByPriority = configuration.isJobExecutorAcquireByPriority();

  }

  @AfterEach
  void tearDown() {
    configuration.setBatchJobPriority(defaultBatchJobPriority);
    configuration.setBatchJobsPerSeed(defaultBatchJobsPerSeed);
    configuration.setJobExecutorPriorityRangeMin(defaultJobExecutorPriorityRangeMin);
    configuration.setJobExecutorPriorityRangeMax(defaultJobExecutorPriorityRangeMax);
    configuration.setJobExecutorAcquireByPriority(defaultIsJobExecutorAcquireByPriority);
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void shouldSetConfiguredPriorityOnBatchCleanupJob() {
    // given
    configuration.setBatchJobPriority(10L);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then
    Job seedJob = helper.getSeedJob(batch);
    helper.completeSeedJobs(batch);
    Job monitorJob = helper.getMonitorJob(batch);
    Job executionJob = helper.getExecutionJobs(batch).get(0);

    assertThat(seedJob.getPriority()).isEqualTo(10L);
    assertThat(monitorJob.getPriority()).isEqualTo(10L);
    assertThat(executionJob.getPriority()).isEqualTo(10L);
  }

  @Test
  void shouldAcquireBatchJobInPriorityRange() {
    // given
    configuration.setJobExecutorPriorityRangeMin(5L);
    configuration.setJobExecutorPriorityRangeMax(15L);
    configuration.setBatchJobPriority(10L);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then
    checkAndCompleteSeedJob(batch, 10L);
    checkAndCompleteMonitorAndExecutionJobs(batch, 10L);
  }

  @Test
  void shouldNotAcquireBatchJobOutsidePriorityRange() {
    // given
    configuration.setJobExecutorAcquireByPriority(true);
    configuration.setJobExecutorPriorityRangeMin(5L);
    configuration.setJobExecutorPriorityRangeMax(15L);
    configuration.setBatchJobPriority(20L);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(1);

    // then
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).isEmpty();

    Job seedJob = helper.getSeedJob(batch);
    assertThat(seedJob.getPriority()).isEqualTo(20L);
    helper.completeSeedJobs(batch);

    List<Job> executionJobs = helper.getExecutionJobs(batch);
    assertThat(executionJobs).hasSize(1);
    assertThat(executionJobs.get(0).getPriority()).isEqualTo(20L);
    helper.completeExecutionJobs(batch);

    Job monitorJob = helper.getMonitorJob(batch);
    assertThat(monitorJob.getPriority()).isEqualTo(20L);
    helper.completeMonitorJobs(batch);
  }

  private void checkAndCompleteSeedJob(Batch batch, Long priority) {
    List<AcquirableJobEntity> jobs = findAcquirableJobs();
    Job seedJob = helper.getSeedJob(batch);

    assertThat(jobs).hasSize(1);
    assertThat(jobs.get(0).getId()).isEqualTo(seedJob.getId());
    assertThat(seedJob.getPriority()).isEqualTo(priority);

    helper.completeSeedJobs(batch);
  }

  private void checkAndCompleteMonitorAndExecutionJobs(Batch batch, Long priority) {
    List<AcquirableJobEntity> jobs = findAcquirableJobs();
    Job monitorJob = helper.getMonitorJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch);

    assertThat(jobs).hasSize(2);
    assertThat(executionJobs).hasSize(1);
    assertThat(jobs).extracting("id").containsExactlyInAnyOrder(monitorJob.getId(), executionJobs.get(0).getId());
    assertThat(monitorJob.getPriority()).isEqualTo(priority);
    assertThat(executionJobs.get(0).getPriority()).isEqualTo(priority);

    helper.completeExecutionJobs(batch);
    helper.completeMonitorJobs(batch);
  }

  private List<AcquirableJobEntity> findAcquirableJobs() {
    return configuration.getCommandExecutorTxRequired().execute(commandContext -> commandContext.getJobManager().findNextJobsToExecute(new Page(0, 100)));
  }
}
