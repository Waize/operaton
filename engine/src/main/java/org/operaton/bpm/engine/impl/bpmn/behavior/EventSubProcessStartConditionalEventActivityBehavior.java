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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import org.operaton.bpm.engine.impl.bpmn.parser.ConditionalEventDefinition;
import org.operaton.bpm.engine.impl.core.variable.event.VariableEvent;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class EventSubProcessStartConditionalEventActivityBehavior extends EventSubProcessStartEventActivityBehavior implements ConditionalEventBehavior {

  protected final ConditionalEventDefinition conditionalEvent;

  public EventSubProcessStartConditionalEventActivityBehavior(ConditionalEventDefinition conditionalEvent) {
    this.conditionalEvent = conditionalEvent;
  }

  @Override
  public ConditionalEventDefinition getConditionalEventDefinition() {
    return conditionalEvent;
  }

  @Override
  public void leaveOnSatisfiedCondition(final EventSubscriptionEntity eventSubscription, final VariableEvent variableEvent) {
    PvmExecutionImpl execution = eventSubscription.getExecution();

    if (execution != null && !execution.isEnded() && execution.isScope()
        && conditionalEvent.tryEvaluate(variableEvent, execution)) {
      ActivityImpl activity = eventSubscription.getActivity();
      activity = (ActivityImpl) activity.getFlowScope();
      execution.executeEventHandlerActivity(activity);
    }
  }

}
