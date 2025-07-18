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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.test.jobexecutor.TweetHandler.TweetJobConfiguration;

public class TweetHandler implements JobHandler<TweetJobConfiguration> {

  List<String> messages = new ArrayList<>();

  @Override
  public String getType() {
    return "tweet";
  }

  @Override
  public void execute(TweetJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    messages.add(configuration.getMessage());
    assertThat(commandContext).isNotNull();
  }

  public List<String> getMessages() {
    return messages;
  }

  @Override
  public TweetJobConfiguration newConfiguration(String canonicalString) {
    TweetJobConfiguration config = new TweetJobConfiguration();
    config.message = canonicalString;

    return config;
  }

  public static class TweetJobConfiguration implements JobHandlerConfiguration {
    protected String message;

    public String getMessage() {
      return message;
    }

    @Override
    public String toCanonicalString() {
      return message;
    }
  }

  @Override
  public void onDelete(TweetJobConfiguration configuration, JobEntity jobEntity) {
    // do nothing
  }

}
