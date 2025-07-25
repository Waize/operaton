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

var Base = require('./base');

module.exports = Base.extend({
  diagramElement: function() {
    return element(by.css('[cam-widget-bpmn-viewer]'));
  },

  instancesBadgeFor: function(activityName) {
    return element(
      by.css(
        '[data-container-id="' +
          activityName +
          '"] .badge[uib-tooltip="Running Activity Instances"]'
      )
    );
  },

  incidentsBadgeFor: function(activityName) {
    return element(
      by.css(
        '[data-container-id="' +
          activityName +
          '"] .badge[uib-tooltip="Open Incidents"]'
      )
    );
  },

  diagramActivity: function(activityName) {
    return element(
      by.css('*[data-element-id=' + '"' + activityName + '"' + ']')
    );
  },

  selectActivity: function(activityName) {
    this.diagramActivity(activityName).click();
  },

  deselectAll: function() {
    this.diagramElement().click();
  },

  isActivitySelected: function(activityName) {
    return this.diagramActivity(activityName)
      .getAttribute('class')
      .then(function(classes) {
        return classes.indexOf('highlight') !== -1;
      });
  },

  isActivitySuspended: function(activityName) {
    return element(
      by.css(
        '[data-container-id="' +
          activityName +
          '"] .badge[uib-tooltip="Suspended Job Definition"]'
      )
    )
      .getAttribute('class')
      .then(function(classes) {
        return classes.indexOf('ng-hide') === -1;
      });
  }
});
