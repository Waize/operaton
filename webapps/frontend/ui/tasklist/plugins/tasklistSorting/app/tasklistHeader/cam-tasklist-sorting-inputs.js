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

var template = require('./cam-tasklist-sorting-inputs.html?raw');

module.exports = [
  '$translate',
  function($translate) {
    return {
      restrict: 'AC',

      replace: true,

      template: template,

      scope: {
        change: '=',
        applyHandler: '&',
        resetFunction: '=',
        variable: '='
      },

      controller: [
        '$scope',
        function($scope) {
          $scope.variableTypes = {
            Boolean: $translate.instant('BOOLEAN'),
            Double: $translate.instant('DOUBLE'),
            Date: $translate.instant('DATE'),
            Integer: $translate.instant('INTEGER'),
            Long: $translate.instant('LONG'),
            Short: $translate.instant('SHORT'),
            String: $translate.instant('STRING')
          };

          $scope.applySorting = function(evt) {
            $scope.applyHandler({$event: evt});
          };
        }
      ]
    };
  }
];
