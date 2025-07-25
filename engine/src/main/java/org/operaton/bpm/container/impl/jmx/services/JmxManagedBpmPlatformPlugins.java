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
package org.operaton.bpm.container.impl.jmx.services;

import java.util.List;

import org.operaton.bpm.container.impl.plugin.BpmPlatformPlugin;
import org.operaton.bpm.container.impl.plugin.BpmPlatformPlugins;
import org.operaton.bpm.container.impl.spi.PlatformService;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;

/**
 * @author Thorben Lindhauer
 *
 */
public class JmxManagedBpmPlatformPlugins implements PlatformService<BpmPlatformPlugins>, JmxManagedBpmPlatformPluginsMBean {

  protected BpmPlatformPlugins plugins;

  public JmxManagedBpmPlatformPlugins(BpmPlatformPlugins plugins) {
    this.plugins = plugins;
  }

  @Override
  public void start(PlatformServiceContainer mBeanServiceContainer) {
    // no callbacks or initialization in the plugins
  }

  @Override
  public void stop(PlatformServiceContainer mBeanServiceContainer) {
    // no callbacks or initialization in the plugins
  }

  @Override
  public BpmPlatformPlugins getValue() {
    return plugins;
  }

  @Override
  public String[] getPluginNames() {
    // expose names of discovered plugins in JMX
    List<BpmPlatformPlugin> pluginList = plugins.getPlugins();
    String[] names = new String[pluginList.size()];
    for (int i = 0; i < names.length; i++) {
      BpmPlatformPlugin bpmPlatformPlugin = pluginList.get(i);
      if(bpmPlatformPlugin != null) {
        names[i] = bpmPlatformPlugin.getClass().getName();
      }
    }
    return names;
  }

}
