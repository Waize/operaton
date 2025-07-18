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

var angular = require('operaton-commons-ui/vendor/angular');

// Services
var observeBpmnElements = require('./services/observe-bpmn-elements');

// Components
var externalTaskActivityLink = require('./components/external-task-activity-link');
var externalTasksTab = require('./components/external-tasks-tab');
var externalTaskErrorMessageLink = require('./components/external-task-error-message-link');

// Controllers
var ExternalTaskActivityLinkController = require('./controllers/external-task-activity-link-controller');
var ExternalTasksTabController = require('./controllers/external-tasks-tab-controller');
var ExternalTaskErrorMessageLinkController = require('./controllers/external-task-error-message-link-controller');

var ngModule = angular.module('cam-common.external-tasks-common', []);

// Services
ngModule.factory('observeBpmnElements', observeBpmnElements);

// Components
ngModule.directive('externalTaskActivityLink', externalTaskActivityLink);
ngModule.directive('externalTasksTab', externalTasksTab);
ngModule.directive(
  'externalTaskErrorMessageLink',
  externalTaskErrorMessageLink
);

// Controllers
ngModule.controller(
  'ExternalTaskActivityLinkController',
  ExternalTaskActivityLinkController
);
ngModule.controller('ExternalTasksTabController', ExternalTasksTabController);
ngModule.controller(
  'ExternalTaskErrorMessageLinkController',
  ExternalTaskErrorMessageLinkController
);

module.exports = ngModule;
