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
package org.operaton.bpm.integrationtest.jobexecutor;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;

@Named
public class SendSignalDelegate implements JavaDelegate {

  @Inject
  private RuntimeService runtimeService;

  @Inject
  private BusinessProcess businessProcess;

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    businessProcess.setVariable("processName", "throwSignal-visited (was " + businessProcess.getVariable("processName")  + ")");

    String signalProcessInstanceId = (String) execution.getVariable("signalProcessInstanceId");
    String executionId = runtimeService.createExecutionQuery().processInstanceId(signalProcessInstanceId).signalEventSubscriptionName("alert").singleResult().getId();

    CommandContext commandContext = Context.getCommandContext();
    List<EventSubscriptionEntity> findSignalEventSubscriptionsByEventName = commandContext
            .getEventSubscriptionManager()
            .findSignalEventSubscriptionsByNameAndExecution("alert", executionId);

    for (EventSubscriptionEntity signalEventSubscriptionEntity : findSignalEventSubscriptionsByEventName) {
        signalEventSubscriptionEntity.eventReceived(null, true);
    }
  }
}
