--
-- Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
-- under one or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information regarding copyright
-- ownership. Camunda licenses this file to you under the Apache License,
-- Version 2.0; you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

drop index ACT_IDX_CASE_EXEC_BUSKEY;

alter table ACT_RU_CASE_EXECUTION
    drop foreign key ACT_FK_CASE_EXE_CASE_INST;

alter table ACT_RU_CASE_EXECUTION
    drop foreign key ACT_FK_CASE_EXE_PARENT;

alter table ACT_RU_CASE_EXECUTION
    drop foreign key ACT_FK_CASE_EXE_CASE_DEF;

alter table ACT_RU_VARIABLE
    drop foreign key ACT_FK_VAR_CASE_EXE;

alter table ACT_RU_VARIABLE
    drop foreign key ACT_FK_VAR_CASE_INST;

alter table ACT_RU_TASK
    drop foreign key ACT_FK_TASK_CASE_EXE;

alter table ACT_RU_TASK
    drop foreign key ACT_FK_TASK_CASE_DEF;

alter table ACT_RU_CASE_SENTRY_PART
    drop foreign key ACT_FK_CASE_SENTRY_CASE_INST;

alter table ACT_RU_CASE_SENTRY_PART
    drop foreign key ACT_FK_CASE_SENTRY_CASE_EXEC;

-- indexes for concurrency problems - https://app.camunda.com/jira/browse/CAM-1646 --
drop index ACT_IDX_CASE_EXEC_CASE;
drop index ACT_IDX_CASE_EXEC_PARENT;
drop index ACT_IDX_VARIABLE_CASE_EXEC;
drop index ACT_IDX_VARIABLE_CASE_INST;
drop index ACT_IDX_TASK_CASE_EXEC;
drop index ACT_IDX_TASK_CASE_DEF_ID;
drop index ACT_IDX_CASE_SENTRY_CASE_INST;
drop index ACT_IDX_CASE_SENTRY_CASE_EXEC;

drop index ACT_IDX_CASE_DEF_TENANT_ID;
drop index ACT_IDX_CASE_EXEC_TENANT_ID;

-- https://app.camunda.com/jira/browse/CAM-9165
drop index ACT_IDX_CASE_EXE_CASE_INST;

drop table ACT_RE_CASE_DEF;
drop table ACT_RU_CASE_EXECUTION;
drop table ACT_RU_CASE_SENTRY_PART;
