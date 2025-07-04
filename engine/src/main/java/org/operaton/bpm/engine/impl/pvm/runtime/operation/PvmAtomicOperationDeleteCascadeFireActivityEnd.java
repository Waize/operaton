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
package org.operaton.bpm.engine.impl.pvm.runtime.operation;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.CompensationBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class PvmAtomicOperationDeleteCascadeFireActivityEnd extends PvmAtomicOperationActivityInstanceEnd {

  @Override
  protected PvmExecutionImpl eventNotificationsStarted(PvmExecutionImpl execution) {
    execution.setCanceled(true);
    return super.eventNotificationsStarted(execution);
  }

  @Override
  protected ScopeImpl getScope(PvmExecutionImpl execution) {
    ActivityImpl activity = execution.getActivity();

    if (activity!=null) {
      return activity;
    } else {
      // TODO: when can this happen?
      PvmExecutionImpl parent = execution.getParent();
      if (parent != null) {
        return getScope(execution.getParent());
      }
      return execution.getProcessDefinition();
    }
  }

  @Override
  protected String getEventName() {
    return ExecutionListener.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {

    PvmActivity activity = execution.getActivity();

    if (execution.isScope()
        && (executesNonScopeActivity(execution) || isAsyncBeforeActivity(execution))
        && !CompensationBehavior.executesNonScopeCompensationHandler(execution)) {
      execution.removeAllTasks();
      // case this is a scope execution and the activity is not a scope
      execution.leaveActivityInstance();
      execution.setActivity(getFlowScopeActivity(activity));
      execution.performOperation(DELETE_CASCADE_FIRE_ACTIVITY_END);

    } else {
      if (execution.isScope()) {
        ProcessEngineConfigurationImpl engineConfiguration = Context.getProcessEngineConfiguration();

        // execution was canceled and output mapping for activity is marked as skippable
        boolean alwaysSkipIoMappings =
            execution instanceof ExecutionEntity &&
            !execution.isProcessInstanceExecution() &&
            execution.isCanceled() &&
            engineConfiguration.isSkipOutputMappingOnCanceledActivities();

        execution.destroy(alwaysSkipIoMappings);
      }

      // remove this execution and its concurrent parent (if exists)
      execution.remove();

      boolean continueRemoval = !execution.isDeleteRoot();

      if (continueRemoval) {
        PvmExecutionImpl propagatingExecution = execution.getParent();
        if (propagatingExecution != null && !propagatingExecution.isScope() && !propagatingExecution.hasChildren()) {
          propagatingExecution.remove();
          continueRemoval = !propagatingExecution.isDeleteRoot();
          propagatingExecution = propagatingExecution.getParent();
        }

        if (continueRemoval && propagatingExecution != null && (propagatingExecution.getActivity() == null && activity != null && activity.getFlowScope() != null)) {
          // continue deletion with the next scope execution
          // set activity on parent in case the parent is an inactive scope execution and activity has been set to 'null'.
          propagatingExecution.setActivity(getFlowScopeActivity(activity));
        }
      }
    }
  }

  protected boolean executesNonScopeActivity(PvmExecutionImpl execution) {
    ActivityImpl activity = execution.getActivity();
    return activity!=null && !activity.isScope();
  }

  protected boolean isAsyncBeforeActivity(PvmExecutionImpl execution) {
    return execution.getActivityId() != null && execution.getActivityInstanceId() == null;
  }

  protected ActivityImpl getFlowScopeActivity(PvmActivity activity) {
    ScopeImpl flowScope = activity.getFlowScope();
    ActivityImpl flowScopeActivity = null;
    if(flowScope.getProcessDefinition() != flowScope) {
      flowScopeActivity = (ActivityImpl) flowScope;
    }
    return flowScopeActivity;
  }

  @Override
  public String getCanonicalName() {
    return "delete-cascade-fire-activity-end";
  }
}
