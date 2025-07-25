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
package org.operaton.bpm.engine.impl.management;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.cmd.SetJobsRetriesBatchCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.management.SetJobRetriesAsyncBuilder;
import org.operaton.bpm.engine.management.SetJobRetriesByJobsAsyncBuilder;
import org.operaton.bpm.engine.runtime.JobQuery;

public class SetJobRetriesByJobsAsyncBuilderImpl implements SetJobRetriesByJobsAsyncBuilder {

  protected static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  protected final CommandExecutor commandExecutor;

  protected List<String> jobIds;
  protected JobQuery jobQuery;
  protected Integer retries;
  protected Date dueDate;
  protected boolean isDueDateSet;

  public SetJobRetriesByJobsAsyncBuilderImpl(CommandExecutor commandExecutor, int retries) {
    this.commandExecutor = commandExecutor;
    this.retries = retries;
  }

  @Override
  public SetJobRetriesByJobsAsyncBuilder jobQuery(JobQuery query) {
    this.jobQuery = query;
    return this;
  }

  @Override
  public SetJobRetriesByJobsAsyncBuilder jobIds(List<String> jobIds) {
    this.jobIds = jobIds;
    return this;
  }

  @Override
  public SetJobRetriesAsyncBuilder dueDate(Date dueDate) {
    this.dueDate = dueDate;
    isDueDateSet = true;
    return this;
  }

  @Override
  public Batch executeAsync() {
    validateParameters();
    return commandExecutor.execute(new SetJobsRetriesBatchCmd(jobIds, jobQuery, retries, dueDate, isDueDateSet));
  }

  protected void validateParameters() {
    ensureNotNull("commandExecutor", commandExecutor);
    ensureNotNull("retries", retries);

    if((jobIds == null || jobIds.isEmpty()) && jobQuery == null) {
      throw LOG.exceptionSettingJobRetriesAsyncNoJobsSpecified();
    }

  }

}
