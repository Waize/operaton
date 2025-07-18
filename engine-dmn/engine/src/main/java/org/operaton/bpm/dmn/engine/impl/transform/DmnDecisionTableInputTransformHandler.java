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
package org.operaton.bpm.dmn.engine.impl.transform;

import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.instance.Input;

public class DmnDecisionTableInputTransformHandler implements DmnElementTransformHandler<Input, DmnDecisionTableInputImpl> {

  @Override
  public DmnDecisionTableInputImpl handleElement(DmnElementTransformContext context, Input input) {
    return createFromInput(context, input);
  }

  protected DmnDecisionTableInputImpl createFromInput(DmnElementTransformContext context, Input input) {
    DmnDecisionTableInputImpl decisionTableInput = createDmnElement(context, input);

    decisionTableInput.setId(input.getId());
    decisionTableInput.setName(input.getLabel());
    decisionTableInput.setInputVariable(input.getOperatonInputVariable());

    return decisionTableInput;
  }

  @SuppressWarnings("unused")
  protected DmnDecisionTableInputImpl createDmnElement(DmnElementTransformContext context, Input input) {
    return new DmnDecisionTableInputImpl();
  }

}
