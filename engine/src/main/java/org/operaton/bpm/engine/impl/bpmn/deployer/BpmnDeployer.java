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
package org.operaton.bpm.engine.impl.bpmn.deployer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.AbstractDefinitionDeployer;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseLogger;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParser;
import org.operaton.bpm.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.operaton.bpm.engine.impl.cmd.DeleteJobsCmd;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.model.Properties;
import org.operaton.bpm.engine.impl.core.model.PropertyMapKey;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.TimerDeclarationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.impl.persistence.deploy.Deployer;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionManager;
import org.operaton.bpm.engine.impl.persistence.entity.IdentityLinkEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.task.IdentityLinkType;

/**
 * {@link Deployer} responsible to parse BPMN 2.0 XML files and create the proper
 * {@link ProcessDefinitionEntity}s. Overwrite this class if you want to gain some control over
 * this mechanism, e.g. setting different version numbers, or you want to use your own {@link BpmnParser}.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Bernd Ruecker
 */
public class BpmnDeployer extends AbstractDefinitionDeployer<ProcessDefinitionEntity> {

  public static final BpmnParseLogger LOG = ProcessEngineLogger.BPMN_PARSE_LOGGER;

  public static final String[] BPMN_RESOURCE_SUFFIXES = new String[] { "bpmn20.xml", "bpmn" };

  protected static final PropertyMapKey<String, List<JobDeclaration<?, ?>>> JOB_DECLARATIONS_PROPERTY =
      new PropertyMapKey<>("JOB_DECLARATIONS_PROPERTY");

  protected ExpressionManager expressionManager;
  protected BpmnParser bpmnParser;

  /** <!> DON'T KEEP DEPLOYMENT-SPECIFIC STATE <!> **/

  @Override
  protected String[] getResourcesSuffixes() {
    return BPMN_RESOURCE_SUFFIXES;
  }

  @Override
  protected List<ProcessDefinitionEntity> transformDefinitions(DeploymentEntity deployment, ResourceEntity resource, Properties properties) {
    byte[] bytes = resource.getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

    BpmnParse bpmnParse = bpmnParser
        .createParse()
        .sourceInputStream(inputStream)
        .deployment(deployment)
        .name(resource.getName());

    if (!deployment.isValidatingSchema()) {
      bpmnParse.setSchemaResource(null);
    }

    bpmnParse.execute();

    if (!properties.contains(JOB_DECLARATIONS_PROPERTY)) {
      properties.set(JOB_DECLARATIONS_PROPERTY, new HashMap<String, List<JobDeclaration<?, ?>>>());
    }
    properties.get(JOB_DECLARATIONS_PROPERTY).putAll(bpmnParse.getJobDeclarations());

    return bpmnParse.getProcessDefinitions();
  }

  @Override
  protected ProcessDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    return getProcessDefinitionManager().findProcessDefinitionByDeploymentAndKey(deploymentId, definitionKey);
  }

  @Override
  protected ProcessDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    return getProcessDefinitionManager().findLatestProcessDefinitionByKeyAndTenantId(definitionKey, tenantId);
  }

  @Override
  protected void persistDefinition(ProcessDefinitionEntity definition) {
    getProcessDefinitionManager().insertProcessDefinition(definition);
  }

  @Override
  protected void addDefinitionToDeploymentCache(DeploymentCache deploymentCache, ProcessDefinitionEntity definition) {
    deploymentCache.addProcessDefinition(definition);
  }

  @Override
  protected void definitionAddedToDeploymentCache(DeploymentEntity deployment, ProcessDefinitionEntity definition, Properties properties) {
    List<JobDeclaration<?, ?>> declarations = properties.get(JOB_DECLARATIONS_PROPERTY).get(definition.getKey());

    updateJobDeclarations(declarations, definition, deployment.isNew());

    ProcessDefinitionEntity latestDefinition = findLatestDefinitionByKeyAndTenantId(definition.getKey(), definition.getTenantId());

    if (deployment.isNew()) {
      adjustStartEventSubscriptions(definition, latestDefinition);

      // add "authorizations"
      addAuthorizations(definition);
    }
  }

  @Override
  protected void persistedDefinitionLoaded(DeploymentEntity deployment, ProcessDefinitionEntity definition, ProcessDefinitionEntity persistedDefinition) {
    definition.setSuspensionState(persistedDefinition.getSuspensionState());
  }

  @Override
  protected void handlePersistedDefinition(ProcessDefinitionEntity definition, ProcessDefinitionEntity persistedDefinition, DeploymentEntity deployment, Properties properties) {
    //check if persisted definition is not null, since the process definition can be deleted by the user
    //in such cases we don't want to handle them
    //we can't do this in the parent method, since other siblings want to handle them like {@link DecisionDefinitionDeployer}
    if (persistedDefinition != null) {
      super.handlePersistedDefinition(definition, persistedDefinition, deployment, properties);
    }
  }

  protected void updateJobDeclarations(List<JobDeclaration<?, ?>> jobDeclarations, ProcessDefinitionEntity processDefinition, boolean isNewDeployment) {

    if(jobDeclarations == null || jobDeclarations.isEmpty()) {
      return;
    }

    final JobDefinitionManager jobDefinitionManager = getJobDefinitionManager();

    if(isNewDeployment) {
      // create new job definitions:
      for (JobDeclaration<?, ?> jobDeclaration : jobDeclarations) {
        createJobDefinition(processDefinition, jobDeclaration);
      }

    } else {
      // query all job definitions and update the declarations with their Ids
      List<JobDefinitionEntity> existingDefinitions = jobDefinitionManager.findByProcessDefinitionId(processDefinition.getId());

      LegacyBehavior.migrateMultiInstanceJobDefinitions(processDefinition, existingDefinitions);

      for (JobDeclaration<?, ?> jobDeclaration : jobDeclarations) {
        boolean jobDefinitionExists = false;
        // find matching job definition entity
        for (JobDefinition jobDefinitionEntity : existingDefinitions) {

          // activity id needs to match
          boolean activityIdMatches = jobDeclaration.getActivityId().equals(jobDefinitionEntity.getActivityId());
          // handler type (e.g. 'async-continuation' needs to match
          boolean handlerTypeMatches = jobDeclaration.getJobHandlerType().equals(jobDefinitionEntity.getJobType());
          // configuration (e.g. 'async-before', 'async-after' needs to match
          boolean configurationMatches = jobDeclaration.getJobConfiguration().equals(jobDefinitionEntity.getJobConfiguration());

          if(activityIdMatches && handlerTypeMatches && configurationMatches) {
            jobDeclaration.setJobDefinitionId(jobDefinitionEntity.getId());
            jobDefinitionExists = true;
            break;
          }
        }

        if(!jobDefinitionExists) {
          // not found: create new definition
          createJobDefinition(processDefinition, jobDeclaration);
        }

      }
    }

  }

  protected void createJobDefinition(ProcessDefinition processDefinition, JobDeclaration<?, ?> jobDeclaration) {
    final JobDefinitionManager jobDefinitionManager = getJobDefinitionManager();

    JobDefinitionEntity jobDefinitionEntity = new JobDefinitionEntity(jobDeclaration);
    jobDefinitionEntity.setProcessDefinitionId(processDefinition.getId());
    jobDefinitionEntity.setProcessDefinitionKey(processDefinition.getKey());
    jobDefinitionEntity.setTenantId(processDefinition.getTenantId());
    jobDefinitionManager.insert(jobDefinitionEntity);
    jobDeclaration.setJobDefinitionId(jobDefinitionEntity.getId());
  }

  /**
   * adjust all event subscriptions responsible to start process instances
   * (timer start event, message start event). The default behavior is to remove the old
   * subscriptions and add new ones for the new deployed process definitions.
   */
  protected void adjustStartEventSubscriptions(ProcessDefinitionEntity newLatestProcessDefinition, ProcessDefinitionEntity oldLatestProcessDefinition) {
    removeObsoleteTimers(newLatestProcessDefinition);
    removeObsoleteEventSubscriptions(newLatestProcessDefinition, oldLatestProcessDefinition);

    addTimerDeclarations(newLatestProcessDefinition);
    addEventSubscriptions(newLatestProcessDefinition);
  }

  @SuppressWarnings("unchecked")
  public void addTimerDeclarations(ProcessDefinitionEntity processDefinition) {
    List<TimerDeclarationImpl> timerDeclarations = (List<TimerDeclarationImpl>) processDefinition.getProperty(BpmnParse.PROPERTYNAME_START_TIMER);
    if (timerDeclarations!=null) {
      for (TimerDeclarationImpl timerDeclaration : timerDeclarations) {
        String deploymentId = processDefinition.getDeploymentId();
        timerDeclaration.createStartTimerInstance(deploymentId);
      }
    }
  }

  protected void removeObsoleteTimers(ProcessDefinitionEntity processDefinition) {
    List<JobEntity> jobsToDelete = getJobManager()
      .findJobsByConfiguration(TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());

    for (JobEntity job :jobsToDelete) {
        new DeleteJobsCmd(job.getId()).execute(Context.getCommandContext());
    }
  }

  protected void removeObsoleteEventSubscriptions(ProcessDefinitionEntity newLatestProcessDefinition, ProcessDefinitionEntity latestProcessDefinition) {
    List<EventSubscriptionEntity> orphanSubscriptions = getOrphanSubscriptionEvents(newLatestProcessDefinition);
    if(!orphanSubscriptions.isEmpty()) { // remove orphan subscriptions if any
      for (EventSubscriptionEntity eventSubscriptionEntity : orphanSubscriptions) {
        getEventSubscriptionManager().deleteEventSubscription(eventSubscriptionEntity);
      }
    }

    if (latestProcessDefinition != null) { // remove all subscriptions for the previous version
      List<EventSubscriptionEntity> previousSubscriptions = getPreviousSubscriptionEvents(latestProcessDefinition);
      for (EventSubscriptionEntity eventSubscriptionEntity : previousSubscriptions) {
        eventSubscriptionEntity.delete();
      }
    }
  }

  protected List<EventSubscriptionEntity> getPreviousSubscriptionEvents(ProcessDefinitionEntity latestProcessDefinition) {
    EventSubscriptionManager eventSubscriptionManager = getEventSubscriptionManager();

    List<EventSubscriptionEntity> subscriptionsToDelete = new ArrayList<>();

    List<EventSubscriptionEntity> messageEventSubscriptions = eventSubscriptionManager
        .findEventSubscriptionsByConfiguration(EventType.MESSAGE.name(), latestProcessDefinition.getId());
    subscriptionsToDelete.addAll(messageEventSubscriptions);

    List<EventSubscriptionEntity> signalEventSubscriptions = eventSubscriptionManager
        .findEventSubscriptionsByConfiguration(EventType.SIGNAL.name(), latestProcessDefinition.getId());
    subscriptionsToDelete.addAll(signalEventSubscriptions);

    List<EventSubscriptionEntity> conditionalEventSubscriptions = eventSubscriptionManager
        .findEventSubscriptionsByConfiguration(EventType.CONDITONAL.name(), latestProcessDefinition.getId());
    subscriptionsToDelete.addAll(conditionalEventSubscriptions);
    return subscriptionsToDelete;
  }

  protected List<EventSubscriptionEntity> getOrphanSubscriptionEvents(ProcessDefinitionEntity processDefinition) {
    String configurationLike = processDefinition.getKey() + ":%:%";
    return getEventSubscriptionManager().findStartEventSubscriptionsByConfigurationLike(configurationLike)
        .stream()
        .filter(this::isOrphan)
        .toList();
  }

  protected boolean isOrphan(EventSubscriptionEntity entity) {
    return entity.getConfiguration() != null && getProcessDefinitionManager().findLatestProcessDefinitionById(entity.getConfiguration()) == null;
  }

  public void addEventSubscriptions(ProcessDefinitionEntity processDefinition) {
    Map<String, EventSubscriptionDeclaration> eventDefinitions = processDefinition.getProperties().get(BpmnProperties.EVENT_SUBSCRIPTION_DECLARATIONS);
    for (EventSubscriptionDeclaration eventDefinition : eventDefinitions.values()) {
      addEventSubscription(processDefinition, eventDefinition);
    }
  }

  protected void addEventSubscription(ProcessDefinitionEntity processDefinition, EventSubscriptionDeclaration eventDefinition) {
    if (eventDefinition.isStartEvent()) {
      String eventType = eventDefinition.getEventType();

      if (eventType.equals(EventType.MESSAGE.name())) {
        addMessageStartEventSubscription(eventDefinition, processDefinition);
      } else if (eventType.equals(EventType.SIGNAL.name())) {
        addSignalStartEventSubscription(eventDefinition, processDefinition);
      } else if (eventType.equals(EventType.CONDITONAL.name())) {
        addConditionalStartEventSubscription(eventDefinition, processDefinition);
      }
    }
  }

  protected void addMessageStartEventSubscription(EventSubscriptionDeclaration messageEventDefinition, ProcessDefinitionEntity processDefinition) {

    String tenantId = processDefinition.getTenantId();

    if(isSameMessageStartEventSubscriptionAlreadyPresent(messageEventDefinition, tenantId)) {
      throw LOG.messageEventSubscriptionWithSameNameExists(processDefinition.getResourceName(), messageEventDefinition.getUnresolvedEventName());
    }

    EventSubscriptionEntity newSubscription = messageEventDefinition.createSubscriptionForStartEvent(processDefinition);
    newSubscription.insert();

  }

  protected boolean isSameMessageStartEventSubscriptionAlreadyPresent(EventSubscriptionDeclaration eventSubscription,
                                                                      String tenantId) {
    // look for subscriptions for the same name in db:
    EventSubscriptionEntity subscriptionForSameMessageName = getEventSubscriptionManager()
        .findMessageStartEventSubscriptionByNameAndTenantId(eventSubscription.getUnresolvedEventName(), tenantId);

    // also look for subscriptions created in the session:
    List<EventSubscriptionEntity> cachedSubscriptions = getDbEntityManager()
        .getCachedEntitiesByType(EventSubscriptionEntity.class);

    for (EventSubscriptionEntity cachedSubscription : cachedSubscriptions) {
      if (eventSubscription.getUnresolvedEventName().equals(cachedSubscription.getEventName()) &&
          hasTenantId(cachedSubscription, tenantId) &&
          !cachedSubscription.equals(subscriptionForSameMessageName) &&
          !isSubscriptionOfDifferentTypeAsDeclaration(cachedSubscription, eventSubscription)) {

        subscriptionForSameMessageName = cachedSubscription;
        break;
      }
    }

    return subscriptionForSameMessageName != null && !getDbEntityManager().isDeleted(subscriptionForSameMessageName);
  }

  protected boolean hasTenantId(EventSubscriptionEntity cachedSubscription, String tenantId) {
    if(tenantId == null) {
      return cachedSubscription.getTenantId() == null;
    } else {
      return tenantId.equals(cachedSubscription.getTenantId());
    }
  }

  protected boolean isSubscriptionOfDifferentTypeAsDeclaration(EventSubscriptionEntity subscriptionEntity,
      EventSubscriptionDeclaration declaration) {

    return (declaration.isStartEvent() && isSubscriptionForIntermediateEvent(subscriptionEntity))
        || (!declaration.isStartEvent() && isSubscriptionForStartEvent(subscriptionEntity));
  }

  protected boolean isSubscriptionForStartEvent(EventSubscriptionEntity subscriptionEntity) {
    return subscriptionEntity.getExecutionId() == null;
  }

  protected boolean isSubscriptionForIntermediateEvent(EventSubscriptionEntity subscriptionEntity) {
    return subscriptionEntity.getExecutionId() != null;
  }

  protected void addSignalStartEventSubscription(EventSubscriptionDeclaration signalEventDefinition, ProcessDefinitionEntity processDefinition) {
    EventSubscriptionEntity newSubscription = signalEventDefinition.createSubscriptionForStartEvent(processDefinition);

    newSubscription.insert();
  }

  protected void addConditionalStartEventSubscription(EventSubscriptionDeclaration conditionalEventDefinition, ProcessDefinitionEntity processDefinition) {
    EventSubscriptionEntity newSubscription = conditionalEventDefinition.createSubscriptionForStartEvent(processDefinition);

    newSubscription.insert();
  }

  enum ExprType {
	  USER, GROUP;

  }

  protected void addAuthorizationsFromIterator(Set<Expression> exprSet, ProcessDefinitionEntity processDefinition, ExprType exprType) {
    if (exprSet != null) {
      for (Expression expr : exprSet) {
        IdentityLinkEntity identityLink = new IdentityLinkEntity();
        identityLink.setProcessDef(processDefinition);
        if (exprType.equals(ExprType.USER)) {
          identityLink.setUserId(expr.toString());
        } else if (exprType.equals(ExprType.GROUP)) {
          identityLink.setGroupId(expr.toString());
        }
        identityLink.setType(IdentityLinkType.CANDIDATE);
        identityLink.setTenantId(processDefinition.getTenantId());
        identityLink.insert();
      }
    }
  }

  protected void addAuthorizations(ProcessDefinitionEntity processDefinition) {
    addAuthorizationsFromIterator(processDefinition.getCandidateStarterUserIdExpressions(), processDefinition, ExprType.USER);
    addAuthorizationsFromIterator(processDefinition.getCandidateStarterGroupIdExpressions(), processDefinition, ExprType.GROUP);
  }

  // context ///////////////////////////////////////////////////////////////////////////////////////////

  protected DbEntityManager getDbEntityManager() {
    return getCommandContext().getDbEntityManager();
  }

  protected JobManager getJobManager() {
    return getCommandContext().getJobManager();
  }

  protected JobDefinitionManager getJobDefinitionManager() {
    return getCommandContext().getJobDefinitionManager();
  }

  protected EventSubscriptionManager getEventSubscriptionManager() {
    return getCommandContext().getEventSubscriptionManager();
  }

  protected ProcessDefinitionManager getProcessDefinitionManager() {
    return getCommandContext().getProcessDefinitionManager();
  }

  // getters/setters ///////////////////////////////////////////////////////////////////////////////////

  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  public BpmnParser getBpmnParser() {
    return bpmnParser;
  }

  public void setBpmnParser(BpmnParser bpmnParser) {
    this.bpmnParser = bpmnParser;
  }

}
