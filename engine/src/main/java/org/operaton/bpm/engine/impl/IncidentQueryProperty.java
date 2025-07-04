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
package org.operaton.bpm.engine.impl;

import org.operaton.bpm.engine.query.QueryProperty;

/**
 * @author roman.smirnov
 */
public interface IncidentQueryProperty {

  QueryProperty INCIDENT_ID = new QueryPropertyImpl("ID_");
  QueryProperty INCIDENT_MESSAGE = new QueryPropertyImpl("INCIDENT_MSG_");
  QueryProperty INCIDENT_TIMESTAMP = new QueryPropertyImpl("INCIDENT_TIMESTAMP_");
  QueryProperty INCIDENT_TYPE = new QueryPropertyImpl("INCIDENT_TYPE_");
  QueryProperty EXECUTION_ID = new QueryPropertyImpl("EXECUTION_ID_");
  QueryProperty ACTIVITY_ID = new QueryPropertyImpl("ACTIVITY_ID_");
  QueryProperty PROCESS_INSTANCE_ID = new QueryPropertyImpl("PROC_INST_ID_");
  QueryProperty PROCESS_DEFINITION_ID = new QueryPropertyImpl("PROC_DEF_ID_");
  QueryProperty CAUSE_INCIDENT_ID = new QueryPropertyImpl("CAUSE_INCIDENT_ID_");
  QueryProperty ROOT_CAUSE_INCIDENT_ID = new QueryPropertyImpl("ROOT_CAUSE_INCIDENT_ID_");
  QueryProperty CONFIGURATION = new QueryPropertyImpl("CONFIGURATION_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");

}
