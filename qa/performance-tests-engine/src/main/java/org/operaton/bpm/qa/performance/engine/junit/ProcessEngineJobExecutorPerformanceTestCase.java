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
package org.operaton.bpm.qa.performance.engine.junit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

public abstract class ProcessEngineJobExecutorPerformanceTestCase extends ProcessEnginePerformanceTestCase {

  protected JobExecutor jobExecutor;

  @Override
  @BeforeEach
  public void setup() {
    super.setup();
    ProcessEngineConfigurationImpl engineConfiguration = ((ProcessEngineImpl) engine).getProcessEngineConfiguration();
    jobExecutor = engineConfiguration.getJobExecutor();
    jobExecutor.start();
  }

  @AfterEach
  public void tearDown() {
    jobExecutor.shutdown();
  }

}
