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
package org.operaton.bpm.engine.rest.impl.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.UriInfo;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricIncidentDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricIncidentQueryDto;
import org.operaton.bpm.engine.rest.history.HistoricIncidentRestService;
import org.operaton.bpm.engine.rest.util.QueryUtil;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricIncidentRestServiceImpl implements HistoricIncidentRestService {

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricIncidentRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public List<HistoricIncidentDto> getHistoricIncidents(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricIncidentQueryDto queryDto = new HistoricIncidentQueryDto(objectMapper, uriInfo.getQueryParameters());
    HistoricIncidentQuery query = queryDto.toQuery(processEngine);

    List<HistoricIncident> queryResult = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricIncidentDto> result = new ArrayList<>();
    for (HistoricIncident historicIncident : queryResult) {
      HistoricIncidentDto dto = HistoricIncidentDto.fromHistoricIncident(historicIncident);
      result.add(dto);
    }

    return result;
  }

  @Override
  public CountResultDto getHistoricIncidentsCount(UriInfo uriInfo) {
    HistoricIncidentQueryDto queryDto = new HistoricIncidentQueryDto(objectMapper, uriInfo.getQueryParameters());
    HistoricIncidentQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

}
