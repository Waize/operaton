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
package org.operaton.bpm.engine.cdi.test.impl.el;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.MessageBean;
import org.operaton.bpm.engine.cdi.test.impl.el.beans.DependentScopedBean;
import org.operaton.bpm.engine.test.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Meyer
 */
@RunWith(Arquillian.class)
public class ElTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment
  public void testSetBeanProperty() {
    MessageBean messageBean = getBeanInstance(MessageBean.class);
    runtimeService.startProcessInstanceByKey("setBeanProperty");
    assertThat(messageBean.getMessage()).isEqualTo("Greetings from Berlin");
  }

  @Test
  @Deployment
  public void testDependentScoped() {

    DependentScopedBean.reset();

    runtimeService.startProcessInstanceByKey("testProcess");

    // make sure the complete bean lifecycle (including invocation of @PreDestroy) was executed.
    // This ensures that the @Dependent scoped bean was properly destroyed.
    assertThat(DependentScopedBean.lifecycle).isEqualTo(Arrays.asList("post-construct-invoked", "bean-invoked", "pre-destroy-invoked"));
  }

}
