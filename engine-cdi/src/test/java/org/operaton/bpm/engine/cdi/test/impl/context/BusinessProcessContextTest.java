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
package org.operaton.bpm.engine.cdi.test.impl.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.CreditCard;
import org.operaton.bpm.engine.test.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Daniel Meyer
 */
@RunWith(Arquillian.class)
public class BusinessProcessContextTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment
  public void testResolution() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    businessProcess.startProcessByKey("testResolution").getId();

    assertThat(getBeanInstance(CreditCard.class)).isNotNull();
  }

  @Test
  // no @Deployment for this test
  public void testResolutionBeforeProcessStart() {
    // assert that @BusinessProcessScoped beans can be resolved in the absence of an underlying process instance:
    assertThat(getBeanInstance(CreditCard.class)).isNotNull();
  }

  @Test
  @Deployment
  public void testChangeProcessScopedBeanProperty() {

    // resolve the creditcard bean (@BusinessProcessScoped) and set a value:
    getBeanInstance(CreditCard.class).setCreditcardNumber("123");
    String pid = getBeanInstance(BusinessProcess.class).startProcessByKey("testConversationalBeanStoreFlush").getId();

    getBeanInstance(BusinessProcess.class).startTask(taskService.createTaskQuery().singleResult().getId());

    // assert that the value of creditCardNumber is '123'
    assertThat(getBeanInstance(CreditCard.class).getCreditcardNumber()).isEqualTo("123");
    // set a different value:
    getBeanInstance(CreditCard.class).setCreditcardNumber("321");
    // complete the task
    getBeanInstance(BusinessProcess.class).completeTask();

    getBeanInstance(BusinessProcess.class).associateExecutionById(pid);

    // now assert that the value of creditcard is "321":
    assertThat(getBeanInstance(CreditCard.class).getCreditcardNumber()).isEqualTo("321");

    // complete the task to allow the process instance to terminate
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

  }

}
