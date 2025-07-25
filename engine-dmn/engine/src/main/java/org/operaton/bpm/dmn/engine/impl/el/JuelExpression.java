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
package org.operaton.bpm.dmn.engine.impl.el;

import jakarta.el.ELContext;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.engine.variable.context.VariableContext;
import jakarta.el.ValueExpression;

/**
 * @author Daniel Meyer
 *
 */
public class JuelExpression implements ElExpression {

  protected final ValueExpression expression;
  protected final JuelElContextFactory elContextFactory;

  public JuelExpression(ValueExpression expression, JuelElContextFactory elContextFactory) {
    this.expression = expression;
    this.elContextFactory = elContextFactory;
  }

  @Override
  public Object getValue(VariableContext variableContext) {
    ELContext elContext = elContextFactory.createElContext(variableContext);
    return expression.getValue(elContext);
  }

  @Override
  public String toString() {
    return "JuelExpression{" +
      "expression=" + expression +
      ", elContextFactory=" + elContextFactory +
      '}';
  }

}
