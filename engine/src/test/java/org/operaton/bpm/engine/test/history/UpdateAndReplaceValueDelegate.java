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
package org.operaton.bpm.engine.test.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

/**
 * @author Thorben Lindhauer
 *
 */
public class UpdateAndReplaceValueDelegate implements JavaDelegate, Serializable {

  private static final long serialVersionUID = 1L;

  public static final String NEW_ELEMENT = "new element";

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    List<String> list = (List<String>) execution.getVariable("listVar");

    // implicitly update the previous list, should update the variable value
    list.add(NEW_ELEMENT);

    // replace the list by another object
    execution.setVariable("listVar", new ArrayList<String>());

    // note that this is the condensed form of more realistic scenarios like
    // an implicit update in task 1 and an explicit update in the following task 2,
    // both in the same transaction.
  }

}
