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
package org.operaton.bpm.engine.test.util;

import java.util.Properties;

import org.operaton.bpm.engine.impl.util.PropertiesUtil;

public class TestconfigProperties {

  protected static final String PROPERTIES_FILE_PATH = "/testconfig.properties";
  protected static final String VERSION_PROPERTY = "operaton.version";
  protected static final Properties INSTANCE = PropertiesUtil.getProperties(PROPERTIES_FILE_PATH);

  protected TestconfigProperties() {
  }

  public static String getEngineVersion() {
    return INSTANCE.getProperty(VERSION_PROPERTY);
  }

}
