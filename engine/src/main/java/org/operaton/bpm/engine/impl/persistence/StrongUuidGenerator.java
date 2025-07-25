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
package org.operaton.bpm.engine.impl.persistence;

import org.operaton.bpm.engine.impl.cfg.IdGenerator;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * {@link IdGenerator} implementation based on the current time and the ethernet
 * address of the machine it is running on.
 *
 * @author Daniel Meyer
 */
public class StrongUuidGenerator implements IdGenerator {

  // different ProcessEngines on the same classloader share one generator.
  protected static TimeBasedGenerator timeBasedGenerator;

  public StrongUuidGenerator() {
    ensureGeneratorInitialized();
  }

  protected synchronized void ensureGeneratorInitialized() {
    if (timeBasedGenerator == null) {
      timeBasedGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
    }
  }

  @Override
  public String getNextId() {
    return timeBasedGenerator.generate().toString();
  }

}
