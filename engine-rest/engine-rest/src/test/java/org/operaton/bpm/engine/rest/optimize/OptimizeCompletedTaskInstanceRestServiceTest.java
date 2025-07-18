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
package org.operaton.bpm.engine.rest.optimize;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.operaton.bpm.engine.rest.util.DateTimeUtils.DATE_FORMAT_WITH_TIMEZONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class OptimizeCompletedTaskInstanceRestServiceTest extends AbstractRestServiceTest {

  public static final String OPTIMIZE_COMPLETED_TASK_INSTANCE_PATH =
    TEST_RESOURCE_ROOT_PATH + "/optimize/task-instance/completed";

  protected OptimizeService mockedOptimizeService;
  protected ProcessEngine namedProcessEngine;

  @ClassRule
  public static TestContainerRule rule = new TestContainerRule();

  @Before
  public void setUpRuntimeData() {
    mockedOptimizeService = mock(OptimizeService.class);
    ProcessEngineConfigurationImpl mockedConfig = mock(ProcessEngineConfigurationImpl.class);


    namedProcessEngine = getProcessEngine(MockProvider.EXAMPLE_PROCESS_ENGINE_NAME);
    when(namedProcessEngine.getProcessEngineConfiguration()).thenReturn(mockedConfig);
    when(mockedConfig.getOptimizeService()).thenReturn(mockedOptimizeService);
  }

  @Test
  public void testNoQueryParameters() {
    given()
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_TASK_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricTaskInstances(null, null, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  public void testFinishedAfterQueryParameter() {
    Date now = new Date();
    given()
      .queryParam("finishedAfter", DATE_FORMAT_WITH_TIMEZONE.format(now))
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_TASK_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricTaskInstances(now, null, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  public void testFinishedAtQueryParameter() {
    Date now = new Date();
    given()
      .queryParam("finishedAt", DATE_FORMAT_WITH_TIMEZONE.format(now))
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_TASK_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricTaskInstances(null, now, Integer.MAX_VALUE);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  public void testMaxResultsQueryParameter() {
    given()
      .queryParam("maxResults", 10)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_TASK_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricTaskInstances(null, null, 10);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

  @Test
  public void testQueryParameterCombination() {
    Date now = new Date();
    given()
      .queryParam("finishedAfter", DATE_FORMAT_WITH_TIMEZONE.format(now))
      .queryParam("finishedAt", DATE_FORMAT_WITH_TIMEZONE.format(now))
      .queryParam("maxResults", 10)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
    .when()
      .get(OPTIMIZE_COMPLETED_TASK_INSTANCE_PATH);

    verify(mockedOptimizeService).getCompletedHistoricTaskInstances(now, now, 10);
    verifyNoMoreInteractions(mockedOptimizeService);
  }

}
