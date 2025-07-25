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
package org.operaton.bpm.engine.impl.cmmn.delegate;

import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.delegate.DelegateCaseExecution;
import org.operaton.bpm.engine.impl.delegate.DelegateInvocation;

/**
 * Class handling invocations of ExecutionListeners
 *
 * @author Roman Smirnov
 */
public class CaseExecutionListenerInvocation extends DelegateInvocation {

  protected final CaseExecutionListener listenerInstance;
  protected final DelegateCaseExecution caseExecution;

  public CaseExecutionListenerInvocation(CaseExecutionListener listenerInstance, DelegateCaseExecution caseExecution) {
    super(caseExecution, null);
    this.listenerInstance = listenerInstance;
    this.caseExecution = caseExecution;
  }

  protected void invoke() throws Exception {
    listenerInstance.notify(caseExecution);
  }

}
