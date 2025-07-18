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
package org.operaton.bpm.client.backoff;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.client.task.impl.ExternalTaskImpl;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nikola Koevski
 */
class ExponentialBackoffStrategyTest {

  protected ExponentialBackoffStrategy backoffStrategy;

  @BeforeEach
  void setup() {
    backoffStrategy = new ExponentialBackoffStrategy();
  }

  @Test
  void shouldAdvanceBackoffStrategy() {
    // given
    long initialWaitingTime = backoffStrategy.calculateBackoffTime();
    assertThat(initialWaitingTime).isZero();

    // when
    // in consecutive iterations, no external tasks are available
    backoffStrategy.reconfigure(emptyList());
    long waitingTime1 = backoffStrategy.calculateBackoffTime();
    backoffStrategy.reconfigure(emptyList());
    long waitingTime2 = backoffStrategy.calculateBackoffTime();

    // then
    assertThat(waitingTime1).isEqualTo(500L);
    assertThat(waitingTime2).isEqualTo(1000L);
  }

  @Test
  void shouldResetBackoffStrategy() {
    // given
    backoffStrategy.reconfigure(emptyList());
    long waitingTime1 = backoffStrategy.calculateBackoffTime();
    assertThat(waitingTime1).isEqualTo(500L);

    // when
    // a not-empty response is received
    backoffStrategy.reconfigure(Lists.newArrayList(new ExternalTaskImpl()));

    // then
    long waitingTime2 = backoffStrategy.calculateBackoffTime();
    assertThat(waitingTime2).isZero();
  }

  @Test
  void shouldCapWaitingTime() {
    // given
    long waitingTime = backoffStrategy.calculateBackoffTime();
    assertThat(waitingTime).isZero();

    // when
    // reach maximum waiting time
    for (int i=0; i<8; i++) {
      backoffStrategy.reconfigure(List.of());
    }

    // then
    waitingTime = backoffStrategy.calculateBackoffTime();
    assertThat(waitingTime).isEqualTo(60000L);
  }
}
