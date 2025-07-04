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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.impl.MessageCorrelationBuilderImpl;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionVariableSnapshotObserver;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.runtime.CorrelationHandlerResult;
import org.operaton.bpm.engine.impl.runtime.MessageCorrelationResultImpl;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultType;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Thorben Lindhauer
 * @author Daniel Meyer
 * @author Michael Scholz
 * @author Christopher Zell
 */
public abstract class AbstractCorrelateMessageCmd {

  protected final String messageName;

  protected final MessageCorrelationBuilderImpl builder;

  protected ExecutionVariableSnapshotObserver variablesListener;
  protected boolean variablesInResultEnabled = false;
  protected long variablesCount = 0;
  protected boolean deserializeVariableValues = false;

  /**
   * Initialize the command with a builder
   *
   * @param builder
   */
  protected AbstractCorrelateMessageCmd(MessageCorrelationBuilderImpl builder) {
    this.builder = builder;
    this.messageName = builder.getMessageName();
    countVariables();

  }

  protected AbstractCorrelateMessageCmd(MessageCorrelationBuilderImpl builder, boolean variablesEnabled, boolean deserializeVariableValues) {
    this(builder);
    this.variablesInResultEnabled = variablesEnabled;
    this.deserializeVariableValues = deserializeVariableValues;
  }

  protected void triggerExecution(CommandContext commandContext, CorrelationHandlerResult correlationResult) {
    String executionId = correlationResult.getExecutionEntity().getId();

    MessageEventReceivedCmd command = new MessageEventReceivedCmd(messageName,
                                                                  executionId,
                                                                  builder.getPayloadProcessInstanceVariables(),
                                                                  builder.getPayloadProcessInstanceVariablesLocal(),
                                                                  builder.getPayloadProcessInstanceVariablesToTriggeredScope(),
                                                                  builder.isExclusiveCorrelation());
    command.execute(commandContext);
  }

  @SuppressWarnings("unused")
  protected ProcessInstance instantiateProcess(CommandContext commandContext, CorrelationHandlerResult correlationResult) {
    ProcessDefinitionEntity processDefinitionEntity = correlationResult.getProcessDefinitionEntity();

    ActivityImpl messageStartEvent = processDefinitionEntity.findActivity(correlationResult.getStartEventActivityId());
    ExecutionEntity processInstance = processDefinitionEntity.createProcessInstance(builder.getBusinessKey(), messageStartEvent);

    if (variablesInResultEnabled) {
      variablesListener = new ExecutionVariableSnapshotObserver(processInstance, false, deserializeVariableValues);
    }

    VariableMap startVariables = resolveVariables();

    processInstance.start(startVariables);

    return processInstance;
  }

  protected void checkAuthorization(CorrelationHandlerResult correlation) {
    CommandContext commandContext = Context.getCommandContext();

    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      if (MessageCorrelationResultType.Execution.equals(correlation.getResultType())) {
        ExecutionEntity execution = correlation.getExecutionEntity();
        checker.checkUpdateProcessInstanceById(execution.getProcessInstanceId());

      } else {
        ProcessDefinitionEntity definition = correlation.getProcessDefinitionEntity();

        checker.checkCreateProcessInstance(definition);
      }
    }
  }

  protected MessageCorrelationResultImpl createMessageCorrelationResult(final CommandContext commandContext, final CorrelationHandlerResult handlerResult) {
    MessageCorrelationResultImpl resultWithVariables = new MessageCorrelationResultImpl(handlerResult);
    if (MessageCorrelationResultType.Execution.equals(handlerResult.getResultType())) {
      ExecutionEntity execution = findProcessInstanceExecution(commandContext, handlerResult);

      ProcessInstance processInstance = execution.getProcessInstance();
      resultWithVariables.setProcessInstance(processInstance);

      if (variablesInResultEnabled && execution != null) {
        variablesListener = new ExecutionVariableSnapshotObserver(execution, false, deserializeVariableValues);
      }
      triggerExecution(commandContext, handlerResult);
    } else {
      ProcessInstance instance = instantiateProcess(commandContext, handlerResult);
      resultWithVariables.setProcessInstance(instance);
    }

    if (variablesListener != null) {
      resultWithVariables.setVariables(variablesListener.getVariables());
    }

    return resultWithVariables;
  }

  protected ExecutionEntity findProcessInstanceExecution(final CommandContext commandContext, final CorrelationHandlerResult handlerResult) {
    return commandContext.getExecutionManager().findExecutionById(handlerResult.getExecution().getProcessInstanceId());
  }

  protected VariableMap resolveVariables() {
    VariableMap mergedVariables = Variables.createVariables();
    mergedVariables.putAll(builder.getPayloadProcessInstanceVariables());
    mergedVariables.putAll(builder.getPayloadProcessInstanceVariablesLocal());
    mergedVariables.putAll(builder.getPayloadProcessInstanceVariablesToTriggeredScope());
    return mergedVariables;
  }

  protected void countVariables() {
    if(builder.getPayloadProcessInstanceVariables() != null) {
      variablesCount += builder.getPayloadProcessInstanceVariables().size();
    }
    if(builder.getPayloadProcessInstanceVariablesLocal() != null) {
      variablesCount += builder.getPayloadProcessInstanceVariablesLocal().size();
    }
    if(builder.getPayloadProcessInstanceVariablesToTriggeredScope() != null) {
      variablesCount += builder.getPayloadProcessInstanceVariablesToTriggeredScope().size();
    }
  }
}
