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
package org.operaton.bpm.integrationtest.deployment.war.beans;

import javax.script.ScriptEngineManager;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptBindingsFactory;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines;

public class GroovyProcessEnginePlugin implements ProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    processEngineConfiguration.setScriptingEngines(
        new ScriptingEngines(
            new ScriptBindingsFactory(processEngineConfiguration.getResolverFactories()),
            new DefaultScriptEngineResolver(new ScriptEngineManager())
        )
    );
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {

  }
}
