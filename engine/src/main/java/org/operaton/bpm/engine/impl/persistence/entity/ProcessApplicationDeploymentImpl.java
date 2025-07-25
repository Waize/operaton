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

import java.util.Date;
import java.util.List;

import org.operaton.bpm.application.ProcessApplicationRegistration;
import org.operaton.bpm.engine.repository.*;

/**
 * @author Daniel Meyer
 *
 */
public class ProcessApplicationDeploymentImpl implements ProcessApplicationDeployment {

  protected DeploymentWithDefinitions deployment;
  protected ProcessApplicationRegistration registration;

  public ProcessApplicationDeploymentImpl(DeploymentWithDefinitions deployment, ProcessApplicationRegistration registration) {
    this.deployment = deployment;
    this.registration = registration;
  }

  @Override
  public String getId() {
    return deployment.getId();
  }

  @Override
  public String getName() {
    return deployment.getName();
  }

  @Override
  public Date getDeploymentTime() {
    return deployment.getDeploymentTime();
  }

  @Override
  public String getSource() {
    return deployment.getSource();
  }

  @Override
  public String getTenantId() {
    return deployment.getTenantId();
  }

  @Override
  public ProcessApplicationRegistration getProcessApplicationRegistration() {
    return registration;
  }

  @Override
  public List<ProcessDefinition> getDeployedProcessDefinitions() {
    return deployment.getDeployedProcessDefinitions();
  }

  @Override
  public List<CaseDefinition> getDeployedCaseDefinitions() {
    return deployment.getDeployedCaseDefinitions();
  }

  @Override
  public List<DecisionDefinition> getDeployedDecisionDefinitions() {
    return deployment.getDeployedDecisionDefinitions();
  }

  @Override
  public List<DecisionRequirementsDefinition> getDeployedDecisionRequirementsDefinitions() {
    return deployment.getDeployedDecisionRequirementsDefinitions();
  }
}
