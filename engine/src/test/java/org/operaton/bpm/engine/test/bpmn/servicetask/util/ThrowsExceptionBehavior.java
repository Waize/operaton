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
package org.operaton.bpm.engine.test.bpmn.servicetask.util;

import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;


/**
 * @author Joram Barrez
 */
public class ThrowsExceptionBehavior implements ActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    String variable = (String) execution.getVariable("var");

    PvmTransition transition;
    try {
      executeLogic(variable);
      transition = execution.getActivity().findOutgoingTransition("no-exception");
    } catch (Exception e) {
      transition = execution.getActivity().findOutgoingTransition("exception");
    }
    execution.leaveActivityViaTransition(transition);
  }

  protected void executeLogic(String value) {
    if (value.equals("throw-exception")) {
      throw new RuntimeException();
    }
  }

}
