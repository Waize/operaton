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
package org.operaton.bpm.engine.spring.test.taskassignment;

import org.operaton.bpm.engine.delegate.DelegateExecution;

import java.util.Arrays;
import java.util.List;


/**
 * @author Joram Barrez
 */
@SuppressWarnings("unused")
public class FakeLdapService {

  @SuppressWarnings("unused")
  public String findManagerForEmployee(String employee) {
    // Pretty useless LDAP service ...
    return "Kermit The Frog";
  }

  public List<String> findAllSales() {
    return Arrays.asList("kermit", "gonzo", "fozzie");
  }

  public List<String> findManagers(DelegateExecution execution, String emp) {
    if (execution == null) {
      throw new RuntimeException("Execution parameter is null");
    }

    if (emp == null || emp.isEmpty()) {
      throw new RuntimeException("emp parameter is null or empty");
    }

    return Arrays.asList("management", "directors");
  }

}
