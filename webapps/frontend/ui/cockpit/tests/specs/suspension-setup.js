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

'use strict';

var factory = require('../../../common/tests/setup-factory.js'),
  readResource = factory.readResource,
  combine = factory.combine,
  operation = factory.operation;

module.exports = {
  setup1: combine(
    operation('deployment', 'create', [
      {
        deploymentName: 'suspension-process',
        files: [
          {
            name: 'suspension-process.bpmn',
            content: readResource('suspension-process.bpmn')
          }
        ]
      }
    ]),

    operation('process-definition', 'start', [
      {
        key: 'suspension-process',
        businessKey: 'Instance1'
      },
      {
        key: 'suspension-process',
        businessKey: 'Instance2'
      },
      {
        key: 'suspension-process',
        businessKey: 'myBusinessKey'
      }
    ])
  )
};
