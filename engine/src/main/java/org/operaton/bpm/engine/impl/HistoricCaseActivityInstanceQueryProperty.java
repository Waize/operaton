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

import org.operaton.bpm.engine.history.HistoricCaseActivityInstanceQuery;
import org.operaton.bpm.engine.query.QueryProperty;


/**
 * Contains the possible properties which can be used in a {@link HistoricCaseActivityInstanceQuery}.
 *
 * @author Sebastian Menski
 */
public interface HistoricCaseActivityInstanceQueryProperty {

  QueryProperty HISTORIC_CASE_ACTIVITY_INSTANCE_ID = new QueryPropertyImpl("ID_");
  QueryProperty CASE_INSTANCE_ID = new QueryPropertyImpl("CASE_INST_ID_");
  QueryProperty CASE_ACTIVITY_ID = new QueryPropertyImpl("CASE_ACT_ID_");
  QueryProperty CASE_ACTIVITY_NAME = new QueryPropertyImpl("CASE_ACT_NAME_");
  QueryProperty CASE_ACTIVITY_TYPE = new QueryPropertyImpl("CASE_ACT_TYPE_");
  QueryProperty CASE_DEFINITION_ID = new QueryPropertyImpl("CASE_DEF_ID_");
  QueryProperty CREATE = new QueryPropertyImpl("CREATE_TIME_");
  QueryProperty END = new QueryPropertyImpl("END_TIME_");
  QueryProperty DURATION = new QueryPropertyImpl("DURATION_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");

}
