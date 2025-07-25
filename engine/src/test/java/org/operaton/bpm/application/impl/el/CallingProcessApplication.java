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
package org.operaton.bpm.application.impl.el;

import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.application.ProcessApplication;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.impl.el.ReadOnlyMapELResolver;
import jakarta.el.ELResolver;

/**
 * @author Thorben Lindhauer
 *
 */
@ProcessApplication(
    value="calling-app",
    deploymentDescriptors={"org/operaton/bpm/application/impl/el/calling-process-app.xml"}
)
public class CallingProcessApplication extends EmbeddedProcessApplication {

  @Override
  protected ELResolver initProcessApplicationElResolver() {
    Map<Object, Object> resolvableValues = new HashMap<>();
    resolvableValues.put("shouldTakeFlow", true);

    return new ReadOnlyMapELResolver(resolvableValues);
  }
}
