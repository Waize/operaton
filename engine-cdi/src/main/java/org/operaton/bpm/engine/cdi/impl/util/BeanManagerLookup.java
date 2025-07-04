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
package org.operaton.bpm.engine.cdi.impl.util;

import jakarta.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.operaton.bpm.engine.ProcessEngineException;

public class BeanManagerLookup {

  /** holds a local beanManager if no jndi is available */
  public static BeanManager localInstance;

  /** provide a custom jndi lookup name */
  public static String jndiName;

  private BeanManagerLookup() {
  }

  public static BeanManager getBeanManager() {

    BeanManager beanManager = lookupBeanManagerInJndi();

    if(beanManager != null) {
      return beanManager;

    } else {
      if (localInstance != null) {
        return localInstance;
      } else {
        throw new ProcessEngineException(
            "Could not lookup beanmanager in jndi. If no jndi is available, set the beanmanger to the 'localInstance' property of this class.");
      }
    }
  }

  private static BeanManager lookupBeanManagerInJndi() {

    if (jndiName != null) {
      try {
        return (BeanManager) InitialContext.doLookup(jndiName);
      } catch (NamingException e) {
        throw new ProcessEngineException("Could not lookup beanmanager in jndi using name: '" + jndiName + "'.", e);
      }
    }

    try {
      // in an application server
      return (BeanManager) InitialContext.doLookup("java:comp/BeanManager");
    } catch (NamingException e) {
      // silently ignore
    }

    try {
      // in a servlet container
      return (BeanManager) InitialContext.doLookup("java:comp/env/BeanManager");
    } catch (NamingException e) {
      // silently ignore
    }

    return null;

  }
}
