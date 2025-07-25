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

import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.NativeHistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

/**
 * @author Philipp Ossler
 */
public class NativeHistoryDecisionInstanceQueryImpl extends AbstractNativeQuery<NativeHistoricDecisionInstanceQuery, HistoricDecisionInstance>
    implements NativeHistoricDecisionInstanceQuery {

  private static final long serialVersionUID = 1L;

  public NativeHistoryDecisionInstanceQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public NativeHistoryDecisionInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
    return commandContext
        .getHistoricDecisionInstanceManager()
        .findHistoricDecisionInstanceCountByNativeQuery(parameterMap);
  }

  @Override
  public List<HistoricDecisionInstance> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return commandContext
        .getHistoricDecisionInstanceManager()
        .findHistoricDecisionInstancesByNativeQuery(parameterMap, firstResult, maxResults);
  }

}
