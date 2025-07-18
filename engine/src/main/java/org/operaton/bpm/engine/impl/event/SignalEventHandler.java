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
package org.operaton.bpm.engine.impl.event;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

import java.util.Map;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Daniel Meyer
 */
public class SignalEventHandler extends EventHandlerImpl {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  public SignalEventHandler() {
    super(EventType.SIGNAL);
  }

  @SuppressWarnings("unused")
  protected void handleStartEvent(EventSubscriptionEntity eventSubscription, Map<String, Object> payload, String businessKey, CommandContext commandContext) {
    String processDefinitionId = eventSubscription.getConfiguration();
    ensureNotNull("Configuration of signal start event subscription '" + eventSubscription.getId() + "' contains no process definition id.",
        processDefinitionId);

    DeploymentCache deploymentCache = Context.getProcessEngineConfiguration().getDeploymentCache();
    ProcessDefinitionEntity processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);
    if (processDefinition == null || processDefinition.isSuspended()) {
      // ignore event subscription
      LOG.debugIgnoringEventSubscription(eventSubscription, processDefinitionId);
    } else {
      ActivityImpl signalStartEvent = processDefinition.findActivity(eventSubscription.getActivityId());
      PvmProcessInstance processInstance = processDefinition.createProcessInstance(businessKey, signalStartEvent);
      processInstance.start(payload);
    }
  }

  @Override
  public void handleEvent(EventSubscriptionEntity eventSubscription,
                          Object payload,
                          Object payloadLocal,
                          Object payloadToTriggeredScope,
                          String businessKey,
                          CommandContext commandContext) {
    if (eventSubscription.getExecutionId() != null) {
      handleIntermediateEvent(eventSubscription, payload, payloadLocal, null, commandContext);
    }
    else {
      handleStartEvent(eventSubscription, (Map<String, Object>) payload, businessKey, commandContext);
    }
  }

}
