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

-- add deployment.lock row to property table --
INSERT INTO ACT_GE_PROPERTY
  VALUES ('deployment.lock', '0', 1);

-- add revision column to incident table --
ALTER TABLE ACT_RU_INCIDENT
  ADD REV_ INTEGER;

-- set initial incident revision to 1 --
UPDATE
  ACT_RU_INCIDENT
SET
  REV_ = 1;

-- set incident revision column to not null --
ALTER TABLE ACT_RU_INCIDENT
  MODIFY (REV_ NOT NULL);

-- case management

ALTER TABLE ACT_RU_VARIABLE
  ADD CASE_EXECUTION_ID_ NVARCHAR2(64);

ALTER TABLE ACT_RU_VARIABLE
  ADD CASE_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_RU_TASK
  ADD CASE_EXECUTION_ID_ NVARCHAR2(64);

ALTER TABLE ACT_RU_TASK
  ADD CASE_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_RU_TASK
  ADD CASE_DEF_ID_ NVARCHAR2(64);

ALTER TABLE ACT_RU_EXECUTION
  ADD SUPER_CASE_EXEC_ NVARCHAR2(64);

ALTER TABLE ACT_RU_EXECUTION
  ADD CASE_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_OP_LOG
  ADD CASE_EXECUTION_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_OP_LOG
  ADD CASE_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_OP_LOG
  ADD CASE_DEF_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_OP_LOG
  ADD PROC_DEF_KEY_ NVARCHAR2(255);

ALTER TABLE ACT_HI_PROCINST
  ADD CASE_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_TASKINST
  ADD CASE_EXECUTION_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_TASKINST
  ADD CASE_INST_ID_ NVARCHAR2(64);

ALTER TABLE ACT_HI_TASKINST
  ADD CASE_DEF_ID_ NVARCHAR2(64);

-- create case definition table --

create table ACT_RE_CASE_DEF (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    CATEGORY_ NVARCHAR2(255),
    NAME_ NVARCHAR2(255),
    KEY_ NVARCHAR2(255) NOT NULL,
    VERSION_ INTEGER NOT NULL,
    DEPLOYMENT_ID_ NVARCHAR2(64),
    RESOURCE_NAME_ NVARCHAR2(2000),
    DGRM_RESOURCE_NAME_ NVARCHAR2(2000),
    primary key (ID_)
);

-- create case execution table --

create table ACT_RU_CASE_EXECUTION (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    CASE_INST_ID_ NVARCHAR2(64),
    SUPER_CASE_EXEC_ NVARCHAR2(64),
    BUSINESS_KEY_ NVARCHAR2(255),
    PARENT_ID_ NVARCHAR2(64),
    CASE_DEF_ID_ NVARCHAR2(64),
    ACT_ID_ NVARCHAR2(255),
    PREV_STATE_ INTEGER,
    CURRENT_STATE_ INTEGER,
    primary key (ID_)
);

-- create case sentry part table --

create table ACT_RU_CASE_SENTRY_PART (
    ID_ NVARCHAR2(64) NOT NULL,
    REV_ INTEGER,
    CASE_INST_ID_ NVARCHAR2(64),
    CASE_EXEC_ID_ NVARCHAR2(64),
    SENTRY_ID_ NVARCHAR2(255),
    TYPE_ NVARCHAR2(255),
    SOURCE_CASE_EXEC_ID_ NVARCHAR2(64),
    STANDARD_EVENT_ NVARCHAR2(255),
    SATISFIED_ NUMBER(1,0) CHECK (SATISFIED_ IN (1,0)),
    primary key (ID_)
);

-- create unique constraint on ACT_RE_CASE_DEF --
alter table ACT_RE_CASE_DEF
    add constraint ACT_UNIQ_CASE_DEF
    unique (KEY_,VERSION_);

-- create index on business key --
create index ACT_IDX_CASE_EXEC_BUSKEY on ACT_RU_CASE_EXECUTION(BUSINESS_KEY_);

-- create foreign key constraints on ACT_RU_CASE_EXECUTION --
create index ACT_IDX_CASE_EXE_CASE_INST on ACT_RU_CASE_EXECUTION(CASE_INST_ID_);
alter table ACT_RU_CASE_EXECUTION
    add constraint ACT_FK_CASE_EXE_CASE_INST
    foreign key (CASE_INST_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

create index ACT_IDX_CASE_EXE_PARENT on ACT_RU_CASE_EXECUTION(PARENT_ID_);
alter table ACT_RU_CASE_EXECUTION
    add constraint ACT_FK_CASE_EXE_PARENT
    foreign key (PARENT_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

create index ACT_IDX_CASE_EXE_CASE_DEF on ACT_RU_CASE_EXECUTION(CASE_DEF_ID_);
alter table ACT_RU_CASE_EXECUTION
    add constraint ACT_FK_CASE_EXE_CASE_DEF
    foreign key (CASE_DEF_ID_)
    references ACT_RE_CASE_DEF(ID_);

-- create foreign key constraints on ACT_RU_VARIABLE --
create index ACT_IDX_VAR_CASE_EXE on ACT_RU_VARIABLE(CASE_EXECUTION_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_CASE_EXE
    foreign key (CASE_EXECUTION_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

create index ACT_IDX_VAR_CASE_INST_ID on ACT_RU_VARIABLE(CASE_INST_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_CASE_INST
    foreign key (CASE_INST_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

-- create foreign key constraints on ACT_RU_TASK --
create index ACT_IDX_TASK_CASE_EXEC on ACT_RU_TASK(CASE_EXECUTION_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_CASE_EXE
    foreign key (CASE_EXECUTION_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

create index ACT_IDX_TASK_CASE_DEF_ID on ACT_RU_TASK(CASE_DEF_ID_);
alter table ACT_RU_TASK
  add constraint ACT_FK_TASK_CASE_DEF
  foreign key (CASE_DEF_ID_)
  references ACT_RE_CASE_DEF(ID_);

-- create foreign key constraints on ACT_RU_CASE_SENTRY_PART --
create index ACT_IDX_CASE_SENTRY_CASE_INST on ACT_RU_CASE_SENTRY_PART(CASE_INST_ID_);
alter table ACT_RU_CASE_SENTRY_PART
    add constraint ACT_FK_CASE_SENTRY_CASE_INST
    foreign key (CASE_INST_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

create index ACT_IDX_CASE_SENTRY_CASE_EXEC on ACT_RU_CASE_SENTRY_PART(CASE_EXEC_ID_);
alter table ACT_RU_CASE_SENTRY_PART
    add constraint ACT_FK_CASE_SENTRY_CASE_EXEC
    foreign key (CASE_EXEC_ID_)
    references ACT_RU_CASE_EXECUTION(ID_);

-- create filter table
create table ACT_RU_FILTER (
  ID_ NVARCHAR2(64) not null,
  REV_ integer not null,
  RESOURCE_TYPE_ NVARCHAR2(255) not null,
  NAME_ NVARCHAR2(255) not null,
  OWNER_ NVARCHAR2(255),
  QUERY_ NCLOB not null,
  PROPERTIES_ NCLOB,
  primary key (ID_)
);

-- add index to improve job executor performance
create index ACT_IDX_JOB_PROCINST on ACT_RU_JOB(PROCESS_INSTANCE_ID_);

-- create historic case instance/activity table and indexes --
create table ACT_HI_CASEINST (
    ID_ NVARCHAR2(64) not null,
    CASE_INST_ID_ NVARCHAR2(64) not null,
    BUSINESS_KEY_ NVARCHAR2(255),
    CASE_DEF_ID_ NVARCHAR2(64) not null,
    CREATE_TIME_ TIMESTAMP(6) not null,
    CLOSE_TIME_ TIMESTAMP(6),
    DURATION_ NUMBER(19,0),
    STATE_ INTEGER,
    CREATE_USER_ID_ NVARCHAR2(255),
    SUPER_CASE_INSTANCE_ID_ NVARCHAR2(64),
    primary key (ID_),
    unique (CASE_INST_ID_)
);

create table ACT_HI_CASEACTINST (
    ID_ NVARCHAR2(64) not null,
    PARENT_ACT_INST_ID_ NVARCHAR2(64),
    CASE_DEF_ID_ NVARCHAR2(64) not null,
    CASE_INST_ID_ NVARCHAR2(64) not null,
    CASE_ACT_ID_ NVARCHAR2(255) not null,
    TASK_ID_ NVARCHAR2(64),
    CALL_PROC_INST_ID_ NVARCHAR2(64),
    CALL_CASE_INST_ID_ NVARCHAR2(64),
    CASE_ACT_NAME_ NVARCHAR2(255),
    CASE_ACT_TYPE_ NVARCHAR2(255),
    CREATE_TIME_ TIMESTAMP(6) not null,
    END_TIME_ TIMESTAMP(6),
    DURATION_ NUMBER(19,0),
    STATE_ INTEGER,
    primary key (ID_)
);

create index ACT_IDX_HI_CAS_I_CLOSE on ACT_HI_CASEINST(CLOSE_TIME_);
create index ACT_IDX_HI_CAS_I_BUSKEY on ACT_HI_CASEINST(BUSINESS_KEY_);
create index ACT_IDX_HI_CAS_A_I_CREATE on ACT_HI_CASEACTINST(CREATE_TIME_);
create index ACT_IDX_HI_CAS_A_I_END on ACT_HI_CASEACTINST(END_TIME_);
create index ACT_IDX_HI_CAS_A_I_COMP on ACT_HI_CASEACTINST(CASE_ACT_ID_, END_TIME_, ID_);

create index ACT_IDX_TASK_ASSIGNEE on ACT_RU_TASK(ASSIGNEE_);

-- add case instance/execution to historic variable instance and detail --
alter table ACT_HI_VARINST
  add CASE_INST_ID_ nvarchar2(64);

alter table ACT_HI_VARINST
  add CASE_EXECUTION_ID_ nvarchar2(64);

alter table ACT_HI_DETAIL
  add CASE_INST_ID_ nvarchar2(64);

alter table ACT_HI_DETAIL
  add CASE_EXECUTION_ID_ nvarchar2(64);

create index ACT_IDX_HI_DETAIL_CASE_INST on ACT_HI_DETAIL(CASE_INST_ID_);
create index ACT_IDX_HI_DETAIL_CASE_EXEC on ACT_HI_DETAIL(CASE_EXECUTION_ID_);
create index ACT_IDX_HI_CASEVAR_CASE_INST on ACT_HI_VARINST(CASE_INST_ID_);
