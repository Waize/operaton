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
package org.operaton.bpm.engine.test.cmmn.handler.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateListener;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.cmmn.listener.DelegateExpressionCaseExecutionListener;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonCaseExecutionListener;

public class DelegateExpressionExecutionListenerSpec extends AbstractExecutionListenerSpec {

  protected static final String DELEGATE_EXPRESSION = "${myDelegateExpression}";

  public DelegateExpressionExecutionListenerSpec(String eventName) {
    super(eventName);
  }

  @Override
  protected void configureCaseExecutionListener(CmmnModelInstance modelInstance, OperatonCaseExecutionListener listener) {
    listener.setOperatonDelegateExpression(DELEGATE_EXPRESSION);
  }

  @Override
  public void verifyListener(DelegateListener<? extends BaseDelegateExecution> listener) {
    assertThat(listener).isInstanceOf(DelegateExpressionCaseExecutionListener.class);

    DelegateExpressionCaseExecutionListener delegateExpressionListener = (DelegateExpressionCaseExecutionListener) listener;
    assertThat(delegateExpressionListener.getExpressionText()).isEqualTo(DELEGATE_EXPRESSION);

    List<FieldDeclaration> fieldDeclarations = delegateExpressionListener.getFieldDeclarations();
    assertThat(fieldDeclarations).hasSize(fieldSpecs.size());

    for (int i = 0; i < fieldDeclarations.size(); i++) {
      FieldDeclaration declaration = fieldDeclarations.get(i);
      FieldSpec matchingFieldSpec = fieldSpecs.get(i);
      matchingFieldSpec.verify(declaration);
    }
  }

}
