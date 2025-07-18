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
package org.operaton.bpm.engine.delegate;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.model.bpmn.instance.UserTask;

/**
 * @author Joram Barrez
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public interface DelegateTask extends VariableScope, BpmnModelExecutionContext, ProcessEngineServicesAware {

  /** DB id of the task. */
  String getId();

  /** Name or title of the task. */
  String getName();

  /** Change the name of the task. */
  void setName(String name);

  /** Free text description of the task. */
  String getDescription();

  /** Change the description of the task */
  void setDescription(String description);

  /** indication of how important/urgent this task is with a number between
   * 0 and 100 where higher values mean a higher priority and lower values mean
   * lower priority: [0..19] lowest, [20..39] low, [40..59] normal, [60..79] high
   * [80..100] highest */
  int getPriority();

  /** indication of how important/urgent this task is with a number between
   * 0 and 100 where higher values mean a higher priority and lower values mean
   * lower priority: [0..19] lowest, [20..39] low, [40..59] normal, [60..79] high
   * [80..100] highest */
  void setPriority(int priority);

  /** Reference to the process instance or null if it is not related to a process instance. */
  String getProcessInstanceId();

  /** Reference to the path of execution or null if it is not related to a process instance. */
  String getExecutionId();

  /** Reference to the process definition or null if it is not related to a process. */
  String getProcessDefinitionId();

  /** Reference to the case instance or null if it is not related to a case instance. */
  String getCaseInstanceId();

  /** Reference to the case execution or null if it is not related to a case instance. */
  String getCaseExecutionId();

  /** Reference to the case definition or null if it is not related to a case. */
  String getCaseDefinitionId();

  /** The date/time when this task was created */
  Date getCreateTime();

  /**
   * The date/time when this task was last updated.
   * All operations that fire {@link TaskListener#EVENTNAME_UPDATE} count as an update to the task.
   * Returns null if the task was never updated before (i.e. it was only created).
   * */
  Date getLastUpdated();

  /** The id of the activity in the process defining this task or null if this is not related to a process */
  String getTaskDefinitionKey();

  /** Returns the execution currently at the task. */
  DelegateExecution getExecution();

  /** Returns the case execution currently at the task. */
  DelegateCaseExecution getCaseExecution();

  /** Returns the event name which triggered the task listener to fire for this task. */
  String getEventName();

  /** Adds the given user as a candidate user to this task. */
  void addCandidateUser(String userId);

  /** Adds multiple users as candidate user to this task. */
  void addCandidateUsers(Collection<String> candidateUsers);

  /** Adds the given group as candidate group to this task */
  void addCandidateGroup(String groupId);

  /** Adds multiple groups as candidate group to this task. */
  void addCandidateGroups(Collection<String> candidateGroups);

  /** The {@link User.getId() userId} of the person responsible for this task. */
  String getOwner();

  /** The {@link User.getId() userId} of the person responsible for this task.*/
  void setOwner(String owner);

  /** The {@link User.getId() userId} of the person to which this task is delegated. */
  String getAssignee();

  /** The {@link User.getId() userId} of the person to which this task is delegated. */
  void setAssignee(String assignee);

  /** Due date of the task. */
  Date getDueDate();

  /** Change due date of the task. */
  void setDueDate(Date dueDate);

  /** Get delete reason of the task. */
  String getDeleteReason();

  /**
   * Involves a user with a task. The type of identity link is defined by the given identityLinkType.
   * @param userId id of the user involve, cannot be null.
   * @param identityLinkType type of identityLink, cannot be null (@see {@link IdentityLinkType}).
   * @throws ProcessEngineException when the task or user doesn't exist.
   */
  void addUserIdentityLink(String userId, String identityLinkType);

  /**
   * Involves a group with group task. The type of identityLink is defined by the given identityLink.
   * @param groupId id of the group to involve, cannot be null.
   * @param identityLinkType type of identity, cannot be null (@see {@link IdentityLinkType}).
   * @throws ProcessEngineException when the task or group doesn't exist.
   */
  void addGroupIdentityLink(String groupId, String identityLinkType);

  /**
   * Convenience shorthand for {@link #deleteUserIdentityLink(String, String)}; with type {@link IdentityLinkType#CANDIDATE}
   * @param userId id of the user to use as candidate, cannot be null.
   * @throws ProcessEngineException when the task or user doesn't exist.
   */
  void deleteCandidateUser(String userId);

  /**
   * Convenience shorthand for {@link #deleteGroupIdentityLink(String, String, String)}; with type {@link IdentityLinkType#CANDIDATE}
   * @param groupId id of the group to use as candidate, cannot be null.
   * @throws ProcessEngineException when the task or group doesn't exist.
   */
  void deleteCandidateGroup(String groupId);

  /**
   * Removes the association between a user and a task for the given identityLinkType.
   * @param userId id of the user involve, cannot be null.
   * @param identityLinkType type of identityLink, cannot be null (@see {@link IdentityLinkType}).
   * @throws ProcessEngineException when the task or user doesn't exist.
   */
  void deleteUserIdentityLink(String userId, String identityLinkType);

  /**
   * Removes the association between a group and a task for the given identityLinkType.
   * @param groupId id of the group to involve, cannot be null.
   * @param identityLinkType type of identity, cannot be null (@see {@link IdentityLinkType}).
   * @throws ProcessEngineException when the task or group doesn't exist.
   */
  void deleteGroupIdentityLink(String groupId, String identityLinkType);

  /**
   * Retrieves the candidate users and groups associated with the task.
   * @return set of {@link IdentityLink}s of type {@link IdentityLinkType#CANDIDATE}.
   */
  Set<IdentityLink> getCandidates();

  /**
   * Provides access to the current {@link UserTask} Element from the Bpmn Model.
   * @return the current {@link UserTask} Element from the Bpmn Model.
   */
  @Override
  UserTask getBpmnModelElementInstance();

  /**
   * Return the id of the tenant this task belongs to. Can be <code>null</code>
   * if the task belongs to no single tenant.
   */
  String getTenantId();

  /** Follow-up date of the task. */
  Date getFollowUpDate();

  /** Change follow-up date of the task. */
  void setFollowUpDate(Date followUpDate);

  /**
   * set status to complete.
   *
   * @throws IllegalStateException if performed on completion or deletion
   */
  void complete();
}
