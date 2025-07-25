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
package org.operaton.bpm.engine.test.api.authorization.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Thorben Lindhauer
 *
 */
public class AuthorizationTestBaseRule extends TestWatcher {

  protected ProcessEngineRule engineRule;

  protected List<User> users = new ArrayList<>();
  protected List<Group> groups = new ArrayList<>();
  protected List<Authorization> authorizations = new ArrayList<>();

  public AuthorizationTestBaseRule(ProcessEngineRule engineRule) {
    this.engineRule = engineRule;
  }

  public void enableAuthorization(String userId) {
    engineRule.getProcessEngine().getProcessEngineConfiguration().setAuthorizationEnabled(true);
    if (userId != null) {
      engineRule.getIdentityService().setAuthenticatedUserId(userId);
    }
  }

  public void disableAuthorization() {
    engineRule.getProcessEngine().getProcessEngineConfiguration().setAuthorizationEnabled(false);
    engineRule.getIdentityService().clearAuthentication();
  }

  @Override
  protected void finished(Description description) {
    engineRule.getIdentityService().clearAuthentication();

    deleteManagedAuthorizations();

    super.finished(description);

    assertThat(users).as("Users have been created but not deleted").isEmpty();
    assertThat(groups).as("Groups have been created but not deleted").isEmpty();
  }

  public void manageAuthorization(Authorization authorization) {
    this.authorizations.add(authorization);
  }

  protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
    Authorization authorization = engineRule.getAuthorizationService().createNewAuthorization(type);

    authorization.setResource(resource);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }

    return authorization;
  }

  public void createGrantAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createAuthorization(Authorization.AUTH_TYPE_GRANT, resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }

    engineRule.getAuthorizationService().saveAuthorization(authorization);
    manageAuthorization(authorization);
  }

  public void createRevokeAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createAuthorization(Authorization.AUTH_TYPE_REVOKE, resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.removePermission(permission);
    }

    engineRule.getAuthorizationService().saveAuthorization(authorization);
    manageAuthorization(authorization);
  }

  protected void deleteManagedAuthorizations() {
    for (Authorization authorization : authorizations) {
      engineRule.getAuthorizationService().deleteAuthorization(authorization.getId());
    }
  }

  public void createUserAndGroup(String userId, String groupId) {

    User user = engineRule.getIdentityService().newUser(userId);
    engineRule.getIdentityService().saveUser(user);
    users.add(user);

    Group group = engineRule.getIdentityService().newGroup(groupId);
    engineRule.getIdentityService().saveGroup(group);
    groups.add(group);
  }

  public void deleteUsersAndGroups() {
    for (User user : users) {
      engineRule.getIdentityService().deleteUser(user.getId());
    }
    users.clear();

    for (Group group : groups) {
      engineRule.getIdentityService().deleteGroup(group.getId());
    }
    groups.clear();
  }
}
