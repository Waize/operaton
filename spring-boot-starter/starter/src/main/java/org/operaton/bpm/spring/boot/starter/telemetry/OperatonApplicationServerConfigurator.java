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
package org.operaton.bpm.spring.boot.starter.telemetry;

import jakarta.servlet.ServletContext;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ManagementServiceImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;


public class OperatonApplicationServerConfigurator implements InitializingBean {

  @Autowired
  protected ProcessEngine processEngine;

  @Autowired
  protected ApplicationContext applicationContext;

  @Override
  public void afterPropertiesSet() throws Exception {
    ServletContext servletContext = null;
    try {
      servletContext = (ServletContext) applicationContext.getBean("servletContext");
    } catch (Exception e) {
    }

    if (servletContext != null) {
      String serverInfo = servletContext.getServerInfo();
      if (serverInfo != null) {
        ((ManagementServiceImpl) processEngine.getManagementService()).addApplicationServerInfoToTelemetry(serverInfo);
      }
    }
  }
}
