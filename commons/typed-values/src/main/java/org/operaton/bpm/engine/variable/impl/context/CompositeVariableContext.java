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
package org.operaton.bpm.engine.variable.impl.context;

import java.util.HashSet;
import java.util.Set;

import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public class CompositeVariableContext implements VariableContext {

  protected final VariableContext[] delegateContexts;

  public CompositeVariableContext(VariableContext[] delegateContexts) {
    this.delegateContexts = delegateContexts;
  }

  @Override
  public TypedValue resolve(String variableName) {
    for (VariableContext variableContext : delegateContexts) {
      TypedValue resolvedValue = variableContext.resolve(variableName);
      if(resolvedValue != null) {
        return resolvedValue;
      }
    }

    return null;
  }

  @Override
  public boolean containsVariable(String name) {
    for (VariableContext variableContext : delegateContexts) {
      if(variableContext.containsVariable(name)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Set<String> keySet() {
    Set<String> keySet = new HashSet<>();
    for (VariableContext variableContext : delegateContexts) {
      keySet.addAll(variableContext.keySet());
    }
    return keySet;
  }

  public static CompositeVariableContext compose(VariableContext... variableContexts) {
    return new CompositeVariableContext(variableContexts);
  }

}
