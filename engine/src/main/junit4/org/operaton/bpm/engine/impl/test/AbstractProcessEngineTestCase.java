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
package org.operaton.bpm.engine.impl.test;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import java.util.*;
import java.util.concurrent.Callable;

import junit.framework.AssertionFailedError;
import org.apache.ibatis.logging.LogFactory;
import org.slf4j.Logger;


/**
 * @author Tom Baeyens
 */
public abstract class AbstractProcessEngineTestCase extends PvmTestCase {
  /**
   * This class isn't used in the Process Engine test suite anymore.
   * However, some Test classes in the following modules still use it:
   *   * operaton-engine-plugin-spin
   *   * operaton-engine-plugin-connect
   *   * operaton-engine-spring
   *   * operaton-identity-ldap
   *
   * It should be removed once those Test classes are migrated to JUnit 4.
   */

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  static {
    // this ensures that mybatis uses slf4j logging
    LogFactory.useSlf4jLogging();
  }

  protected ProcessEngine processEngine;

  protected String deploymentId;
  protected Set<String> deploymentIds = new HashSet<>();

  protected Throwable exception;

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected FormService formService;
  protected HistoryService historyService;
  protected IdentityService identityService;
  protected ManagementService managementService;
  protected AuthorizationService authorizationService;
  protected CaseService caseService;
  protected FilterService filterService;
  protected ExternalTaskService externalTaskService;
  protected DecisionService decisionService;

  protected abstract void initializeProcessEngine();

  // Default: do nothing
  protected void closeDownProcessEngine() {
  }

  @Override
  public void runBare() throws Throwable {
    initializeProcessEngine();
    if (repositoryService==null) {
      initializeServices();
    }

    try {

      boolean hasRequiredHistoryLevel = TestHelper.annotationRequiredHistoryLevelCheck(processEngine, getClass(), getName());
      boolean runsWithRequiredDatabase = TestHelper.annotationRequiredDatabaseCheck(processEngine, getClass(), getName());
      // ignore test case when current history level is too low or database doesn't match
      if (hasRequiredHistoryLevel && runsWithRequiredDatabase) {

        deploymentId = TestHelper.annotationDeploymentSetUp(processEngine, getClass(), getName());

        super.runBare();
      }

    }
    catch (AssertionFailedError e) {
      LOG.error("ASSERTION FAILED: " + e, e);
      exception = e;
      throw e;

    }
    catch (Throwable e) {
      LOG.error("EXCEPTION: " + e, e);
      exception = e;
      throw e;

    }
    finally {

      identityService.clearAuthentication();
      processEngineConfiguration.setTenantCheckEnabled(true);

      deleteDeployments();

      deleteHistoryCleanupJobs();

      // only fail if no test failure was recorded
      TestHelper.assertAndEnsureCleanDbAndCache(processEngine, exception == null);
      TestHelper.resetIdGenerator(processEngineConfiguration);
      ClockUtil.reset();

      // Can't do this in the teardown, as the teardown will be called as part
      // of the super.runBare
      closeDownProcessEngine();
      clearServiceReferences();
    }
  }

  protected void deleteHistoryCleanupJobs() {
    final List<Job> jobs = historyService.findHistoryCleanupJobs();
    for (final Job job: jobs) {
      processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        return null;
      });
    }
  }

  protected void deleteDeployments() {
    if(deploymentId != null) {
      deploymentIds.add(deploymentId);
    }

    for(String deployment : deploymentIds) {
      TestHelper.annotationDeploymentTearDown(processEngine, deployment, getClass(), getName());
    }

    deploymentId = null;
    deploymentIds.clear();
  }

  protected void initializeServices() {
    processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
    repositoryService = processEngine.getRepositoryService();
    runtimeService = processEngine.getRuntimeService();
    taskService = processEngine.getTaskService();
    formService = processEngine.getFormService();
    historyService = processEngine.getHistoryService();
    identityService = processEngine.getIdentityService();
    managementService = processEngine.getManagementService();
    authorizationService = processEngine.getAuthorizationService();
    caseService = processEngine.getCaseService();
    filterService = processEngine.getFilterService();
    externalTaskService = processEngine.getExternalTaskService();
    decisionService = processEngine.getDecisionService();
  }

  protected void clearServiceReferences() {
    processEngineConfiguration = null;
    repositoryService = null;
    runtimeService = null;
    taskService = null;
    formService = null;
    historyService = null;
    identityService = null;
    managementService = null;
    authorizationService = null;
    caseService = null;
    filterService = null;
    externalTaskService = null;
    decisionService = null;
  }

  public void assertProcessEnded(final String processInstanceId) {
    ProcessInstance processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    if (processInstance!=null) {
      throw new AssertionFailedError("Expected finished process instance '"+processInstanceId+"' but it was still in the db");
    }
  }

  public void assertProcessNotEnded(final String processInstanceId) {
    ProcessInstance processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    if (processInstance==null) {
      throw new AssertionFailedError("Expected process instance '"+processInstanceId+"' to be still active but it was not in the db");
    }
  }

  public void assertCaseEnded(final String caseInstanceId) {
    CaseInstance caseInstance = processEngine
      .getCaseService()
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstanceId)
      .singleResult();

    if (caseInstance!=null) {
      throw new AssertionFailedError("Expected finished case instance '"+caseInstanceId+"' but it was still in the db");
    }
  }

  /**
   * @deprecated Use {@link JobExecutorWaitUtils#waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration, long, long)} instead
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {
    JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait, intervalMillis);
  }

  /**
   * @deprecated Use {@link JobExecutorWaitUtils#waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration, long)} instead
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {
   JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait);
  }

  /**
   * @deprecated Use {@link JobExecutorWaitUtils#waitForCondition(Callable, long, long)} instead
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void waitForJobExecutorOnCondition(long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
    JobExecutorWaitUtils.waitForCondition(condition, maxMillisToWait, intervalMillis);
  }

  /**
   * Execute all available jobs recursively till no more jobs found.
   */
  public void executeAvailableJobs() {
    executeAvailableJobs(0, Integer.MAX_VALUE, true, true);
  }

  /**
   * Execute all available jobs recursively till no more jobs found or the number of executions is higher than expected.
   *
   * @param expectedExecutions number of expected job executions
   *
   * @throws AssertionFailedError when execute less or more jobs than expected
   *
   * @see #executeAvailableJobs()
   */
  public void executeAvailableJobs(int expectedExecutions){
    executeAvailableJobs(0, expectedExecutions, false, true);
  }

  public void executeAvailableJobs(boolean recursive){
    executeAvailableJobs(0, Integer.MAX_VALUE, true, recursive);
  }

  private void executeAvailableJobs(int jobsExecuted, int expectedExecutions, boolean ignoreLessExecutions, boolean recursive) {
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();

    if (jobs.isEmpty()) {
      assertTrue("executed less jobs than expected. expected <" + expectedExecutions + "> actual <" + jobsExecuted + ">",
          jobsExecuted == expectedExecutions || ignoreLessExecutions);
      return;
    }

    for (Job job : jobs) {
      try {
        managementService.executeJob(job.getId());
        jobsExecuted += 1;
      } catch (Exception e) {}
    }

    assertTrue("executed more jobs than expected. expected <" + expectedExecutions + "> actual <" + jobsExecuted + ">",
        jobsExecuted <= expectedExecutions);

    if (recursive) {
      executeAvailableJobs(jobsExecuted, expectedExecutions, ignoreLessExecutions, recursive);
    }
  }

  @Deprecated
  protected List<ActivityInstance> getInstancesForActivitiyId(ActivityInstance activityInstance, String activityId) {
    return getInstancesForActivityId(activityInstance, activityId);
  }

  protected List<ActivityInstance> getInstancesForActivityId(ActivityInstance activityInstance, String activityId) {
    List<ActivityInstance> result = new ArrayList<>();
    if(activityInstance.getActivityId().equals(activityId)) {
      result.add(activityInstance);
    }
    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      result.addAll(getInstancesForActivityId(childInstance, activityId));
    }
    return result;
  }

  protected void runAsUser(String userId, List<String> groupIds, Runnable r) {
    try {
      identityService.setAuthenticatedUserId(userId);
      processEngineConfiguration.setAuthorizationEnabled(true);

      r.run();

    } finally {
      identityService.setAuthenticatedUserId(null);
      processEngineConfiguration.setAuthorizationEnabled(false);
    }
  }

  protected String deployment(BpmnModelInstance... bpmnModelInstances) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

    return deployment(deploymentBuilder, bpmnModelInstances);
  }

  protected String deployment(String... resources) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

    return deployment(deploymentBuilder, resources);
  }

  protected String deploymentForTenant(String tenantId, BpmnModelInstance... bpmnModelInstances) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().tenantId(tenantId);

    return deployment(deploymentBuilder, bpmnModelInstances);
  }

  protected String deploymentForTenant(String tenantId, String... resources) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().tenantId(tenantId);

    return deployment(deploymentBuilder, resources);
  }

  protected String deploymentForTenant(String tenantId, String classpathResource, BpmnModelInstance modelInstance) {
    return deployment(repositoryService.createDeployment()
        .tenantId(tenantId)
        .addClasspathResource(classpathResource), modelInstance);
  }

  protected String deployment(DeploymentBuilder deploymentBuilder, BpmnModelInstance... bpmnModelInstances) {
    for (int i = 0; i < bpmnModelInstances.length; i++) {
      BpmnModelInstance bpmnModelInstance = bpmnModelInstances[i];
      deploymentBuilder.addModelInstance("testProcess-"+i+".bpmn", bpmnModelInstance);
    }

    return deploymentWithBuilder(deploymentBuilder);
  }

  protected String deployment(DeploymentBuilder deploymentBuilder, String... resources) {
    for (int i = 0; i < resources.length; i++) {
      deploymentBuilder.addClasspathResource(resources[i]);
    }

    return deploymentWithBuilder(deploymentBuilder);
  }

  protected String deploymentWithBuilder(DeploymentBuilder builder) {
    deploymentId = builder.deploy().getId();
    deploymentIds.add(deploymentId);

    return deploymentId;
  }

}
