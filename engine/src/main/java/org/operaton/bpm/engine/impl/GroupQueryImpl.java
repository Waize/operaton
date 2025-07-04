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
package org.operaton.bpm.engine.impl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;


/**
 * @author Joram Barrez
 */
public abstract class GroupQueryImpl extends AbstractQuery<GroupQuery, Group> implements GroupQuery {

  private static final long serialVersionUID = 1L;
  protected String id;
  protected String[] ids;
  protected String name;
  protected String nameLike;
  protected String type;
  protected String userId;
  protected String procDefId;
  protected String tenantId;

  protected GroupQueryImpl() {
  }

  protected GroupQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public GroupQuery groupId(String id) {
    ensureNotNull("Provided id", id);
    this.id = id;
    return this;
  }

  @Override
  public GroupQuery groupIdIn(String... ids) {
    ensureNotNull("Provided ids", (Object[]) ids);
    this.ids = ids;
    return this;
  }

  @Override
  public GroupQuery groupName(String name) {
    ensureNotNull("Provided name", name);
    this.name = name;
    return this;
  }

  @Override
  public GroupQuery groupNameLike(String nameLike) {
    ensureNotNull("Provided nameLike", nameLike);
    this.nameLike = nameLike;
    return this;
  }

  @Override
  public GroupQuery groupType(String type) {
    ensureNotNull("Provided type", type);
    this.type = type;
    return this;
  }

  @Override
  public GroupQuery groupMember(String userId) {
    ensureNotNull("Provided userId", userId);
    this.userId = userId;
    return this;
  }

  @Override
  public GroupQuery potentialStarter(String procDefId) {
    ensureNotNull("Provided processDefinitionId", procDefId);
    this.procDefId = procDefId;
    return this;
  }

  @Override
  public GroupQuery memberOfTenant(String tenantId) {
    ensureNotNull("Provided tenantId", tenantId);
    this.tenantId = tenantId;
    return this;
  }

  //sorting ////////////////////////////////////////////////////////

  @Override
  public GroupQuery orderByGroupId() {
    return orderBy(GroupQueryProperty.GROUP_ID);
  }

  @Override
  public GroupQuery orderByGroupName() {
    return orderBy(GroupQueryProperty.NAME);
  }

  @Override
  public GroupQuery orderByGroupType() {
    return orderBy(GroupQueryProperty.TYPE);
  }

  //getters ////////////////////////////////////////////////////////

  public String getId() {
    return id;
  }
  public String getName() {
    return name;
  }
  public String getNameLike() {
    return nameLike;
  }
  public String getType() {
    return type;
  }
  public String getUserId() {
    return userId;
  }
  public String getTenantId() {
    return tenantId;
  }
  public String[] getIds() {
    return ids;
  }
}
