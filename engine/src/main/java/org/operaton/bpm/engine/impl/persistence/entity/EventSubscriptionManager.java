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
package org.operaton.bpm.engine.impl.persistence.entity;

import org.operaton.bpm.engine.impl.EventSubscriptionQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.commons.utils.EnsureUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionManager extends AbstractManager {
  private static final String EVENT_NAME = "eventName";
  private static final String EVENT_TYPE = "eventType";
  private static final String EXECUTION_ID = "executionId";
  private static final String LOCK_RESULT = "lockResult";
  private static final String MESSAGE_NAME = "messageName";

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  /** keep track of subscriptions created in the current command */
  protected List<EventSubscriptionEntity> createdSignalSubscriptions = new ArrayList<>();

  public void insert(EventSubscriptionEntity persistentObject) {
    super.insert(persistentObject);
    if (persistentObject.isSubscriptionForEventType(EventType.SIGNAL)) {
      createdSignalSubscriptions.add(persistentObject);
    }
  }

  public void deleteEventSubscription(EventSubscriptionEntity persistentObject) {
    getDbEntityManager().delete(persistentObject);
    if (persistentObject.isSubscriptionForEventType(EventType.SIGNAL)) {
      createdSignalSubscriptions.remove(persistentObject);
    }

    // if the event subscription has been triggered asynchronously but not yet executed
    List<JobEntity> asyncJobs = getJobManager().findJobsByConfiguration(ProcessEventJobHandler.TYPE, persistentObject.getId(), persistentObject.getTenantId());
    for (JobEntity asyncJob : asyncJobs) {
      asyncJob.delete();
    }
  }

  public void deleteAndFlushEventSubscription(EventSubscriptionEntity persistentObject) {
    deleteEventSubscription(persistentObject);
    getDbEntityManager().flushEntity(persistentObject);
  }

  public EventSubscriptionEntity findEventSubscriptionById(String id) {
    return (EventSubscriptionEntity) getDbEntityManager().selectOne("selectEventSubscription", id);
  }

  public long findEventSubscriptionCountByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
    configureQuery(eventSubscriptionQueryImpl);
    return (Long) getDbEntityManager().selectOne("selectEventSubscriptionCountByQueryCriteria", eventSubscriptionQueryImpl);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl, Page page) {
    configureQuery(eventSubscriptionQueryImpl);
    return getDbEntityManager().selectList("selectEventSubscriptionByQueryCriteria", eventSubscriptionQueryImpl, page);
  }

  /**
   * Find all signal event subscriptions with the given event name for any tenant.
   *
   * @see #findSignalEventSubscriptionsByEventNameAndTenantId(String, String)
   */
  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findSignalEventSubscriptionsByEventName(String eventName) {
    final String query = "selectSignalEventSubscriptionsByEventName";
    Set<EventSubscriptionEntity> eventSubscriptions = new HashSet<>( getDbEntityManager().selectList(query, configureParameterizedQuery(eventName)));

    // add events created in this command (not visible yet in query)
    for (EventSubscriptionEntity entity : createdSignalSubscriptions) {
      if(eventName.equals(entity.getEventName())) {
        eventSubscriptions.add(entity);
      }
    }
    return new ArrayList<>(eventSubscriptions);
  }

  /**
   * Find all signal event subscriptions with the given event name and tenant.
   */
  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findSignalEventSubscriptionsByEventNameAndTenantId(String eventName, String tenantId) {
    final String query = "selectSignalEventSubscriptionsByEventNameAndTenantId";

    Map<String, Object> parameter = new HashMap<>();
    parameter.put(EVENT_NAME, eventName);
    parameter.put(TENANT_ID, tenantId);
    Set<EventSubscriptionEntity> eventSubscriptions = new HashSet<>( getDbEntityManager().selectList(query, parameter));

    // add events created in this command (not visible yet in query)
    for (EventSubscriptionEntity entity : createdSignalSubscriptions) {
      if(eventName.equals(entity.getEventName()) && hasTenantId(entity, tenantId)) {
        eventSubscriptions.add(entity);
      }
    }
    return new ArrayList<>(eventSubscriptions);
  }

  /**
   * Find all signal event subscriptions with the given event name which belongs to the given tenant or no tenant.
   */
  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findSignalEventSubscriptionsByEventNameAndTenantIdIncludeWithoutTenantId(String eventName, String tenantId) {
    final String query = "selectSignalEventSubscriptionsByEventNameAndTenantIdIncludeWithoutTenantId";

    Map<String, Object> parameter = new HashMap<>();
    parameter.put(EVENT_NAME, eventName);
    parameter.put(TENANT_ID, tenantId);
    Set<EventSubscriptionEntity> eventSubscriptions = new HashSet<>( getDbEntityManager().selectList(query, parameter));

    // add events created in this command (not visible yet in query)
    for (EventSubscriptionEntity entity : createdSignalSubscriptions) {
      if(eventName.equals(entity.getEventName()) && (entity.getTenantId() == null || hasTenantId(entity, tenantId))) {
        eventSubscriptions.add(entity);
      }
    }
    return new ArrayList<>(eventSubscriptions);
  }

  protected boolean hasTenantId(EventSubscriptionEntity entity, String tenantId) {
    if (tenantId == null) {
      return entity.getTenantId() == null;
    } else {
      return tenantId.equals(entity.getTenantId());
    }
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findSignalEventSubscriptionsByExecution(String executionId) {
    final String query = "selectSignalEventSubscriptionsByExecution";
    Set<EventSubscriptionEntity> selectList = new HashSet<>( getDbEntityManager().selectList(query, executionId));

    // add events created in this command (not visible yet in query)
    for (EventSubscriptionEntity entity : createdSignalSubscriptions) {
      if(executionId.equals(entity.getExecutionId())) {
        selectList.add(entity);
      }
    }
    return new ArrayList<>(selectList);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(String name, String executionId) {
    final String query = "selectSignalEventSubscriptionsByNameAndExecution";
    Map<String,String> params = new HashMap<>();
    params.put(EXECUTION_ID, executionId);
    params.put(EVENT_NAME, name);
    Set<EventSubscriptionEntity> selectList = new HashSet<>( getDbEntityManager().selectList(query, params));

    // add events created in this command (not visible yet in query)
    for (EventSubscriptionEntity entity : createdSignalSubscriptions) {
      if(executionId.equals(entity.getExecutionId())
         && name.equals(entity.getEventName())) {
        selectList.add(entity);
      }
    }
    return new ArrayList<>(selectList);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(String executionId, String type, boolean lockResult) {
    final String query = "selectEventSubscriptionsByExecutionAndType";
    Map<String, Object> params = new HashMap<>();
    params.put(EXECUTION_ID, executionId);
    params.put(EVENT_TYPE, type);
    params.put(LOCK_RESULT, lockResult);
    return getDbEntityManager().selectList(query, params);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptionsByExecution(String executionId) {
    final String query = "selectEventSubscriptionsByExecution";
    return getDbEntityManager().selectList(query, executionId);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptions(String executionId, String type, String activityId) {
    final String query = "selectEventSubscriptionsByExecutionTypeAndActivity";
    Map<String,String> params = new HashMap<>();
    params.put(EXECUTION_ID, executionId);
    params.put(EVENT_TYPE, type);
    params.put(ACTIVITY_ID, activityId);
    return getDbEntityManager().selectList(query, params);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptionsByConfiguration(String type, String configuration) {
    final String query = "selectEventSubscriptionsByConfiguration";
    Map<String,String> params = new HashMap<>();
    params.put(EVENT_TYPE, type);
    params.put("configuration", configuration);
    return getDbEntityManager().selectList(query, params);
  }

  public List<EventSubscriptionEntity> findStartEventSubscriptionsByConfigurationLike(String configuration) {
    final String query = "selectStartEventSubscriptionsByConfigurationLike";
    Map<String,String> params = new HashMap<>();
    params.put("configuration", configuration);
    return getDbEntityManager().selectList(query, params);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndTenantId(String type, String eventName, String tenantId) {
    final String query = "selectEventSubscriptionsByNameAndTenantId";
    Map<String,String> params = new HashMap<>();
    params.put(EVENT_TYPE, type);
    params.put(EVENT_NAME, eventName);
    params.put(TENANT_ID, tenantId);
    return getDbEntityManager().selectList(query, params);
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId, boolean lockResult) {
    // first check cache in case entity is already loaded
    ExecutionEntity cachedExecution = getDbEntityManager().getCachedEntity(ExecutionEntity.class, executionId);
    if(cachedExecution != null && !lockResult) {
      List<EventSubscriptionEntity> eventSubscriptions = cachedExecution.getEventSubscriptions();
      List<EventSubscriptionEntity> result = new ArrayList<>();
      for (EventSubscriptionEntity subscription : eventSubscriptions) {
        if(matchesSubscription(subscription, type, eventName)) {
          result.add(subscription);
        }
      }
      return result;
    }
    else {
      final String query = "selectEventSubscriptionsByNameAndExecution";
      Map<String, Object> params = new HashMap<>();
      params.put(EVENT_TYPE, type);
      params.put(EVENT_NAME, eventName);
      params.put(EXECUTION_ID, executionId);
      params.put(LOCK_RESULT, lockResult);
      return getDbEntityManager().selectList(query, params);
    }
  }

  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceId(String processInstanceId) {
    return getDbEntityManager().selectList("selectEventSubscriptionsByProcessInstanceId", processInstanceId);
  }

  /**
   * @return the message start event subscriptions with the given message name (from any tenant)
   *
   * @see #findMessageStartEventSubscriptionByNameAndTenantId(String, String)
   */
  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findMessageStartEventSubscriptionByName(String messageName) {
    return getDbEntityManager().selectList("selectMessageStartEventSubscriptionByName", configureParameterizedQuery(messageName));
  }

  /**
   * @return the message start event subscription with the given message name and tenant id
   *
   * @see #findMessageStartEventSubscriptionByName(String)
   */
  public EventSubscriptionEntity findMessageStartEventSubscriptionByNameAndTenantId(String messageName, String tenantId) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(MESSAGE_NAME, messageName);
    parameters.put(TENANT_ID, tenantId);

    return (EventSubscriptionEntity) getDbEntityManager().selectOne("selectMessageStartEventSubscriptionByNameAndTenantId", parameters);
  }

  /**
   * @param tenantId
   * @return the conditional start event subscriptions with the given tenant id
   *
   */
  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findConditionalStartEventSubscriptionByTenantId(String tenantId) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TENANT_ID, tenantId);

    configureParameterizedQuery(parameters);
    return getDbEntityManager().selectList("selectConditionalStartEventSubscriptionByTenantId", parameters);
  }

  /**
   * @return the conditional start event subscriptions (from any tenant)
   *
   */
  @SuppressWarnings("unchecked")
  public List<EventSubscriptionEntity> findConditionalStartEventSubscription() {
    ListQueryParameterObject parameter = new ListQueryParameterObject();

    configurParameterObject(parameter);
    return getDbEntityManager().selectList("selectConditionalStartEventSubscription", parameter);
  }

  protected void configurParameterObject(ListQueryParameterObject parameter) {
    getAuthorizationManager().configureConditionalEventSubscriptionQuery(parameter);
    getTenantManager().configureQuery(parameter);
  }

  protected void configureQuery(EventSubscriptionQueryImpl query) {
    getAuthorizationManager().configureEventSubscriptionQuery(query);
    getTenantManager().configureQuery(query);
  }

  protected ListQueryParameterObject configureParameterizedQuery(Object parameter) {
    return getTenantManager().configureQuery(parameter);
  }

  protected boolean matchesSubscription(EventSubscriptionEntity subscription, String type, String eventName) {
    EnsureUtil.ensureNotNull("event type", type);
    String subscriptionEventName = subscription.getEventName();

    return type.equals(subscription.getEventType()) &&
          ((eventName == null && subscriptionEventName == null) || (eventName != null && eventName.equals(subscriptionEventName)));
  }

}
