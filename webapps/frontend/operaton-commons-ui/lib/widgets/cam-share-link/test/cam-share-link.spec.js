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

/* jshint node: true, unused: false */
/* global __dirname: false, xdescribe: false, describe: false, before: false, it: false, browser: false,
          element: false, expect: false, by: false, protractor: false, xit: false,
          describe: false, after: false */
'use strict';
var path = require('path');
var projectRoot = path.resolve(__dirname, '../../../../');
var pkg = require(path.join(projectRoot, 'package.json'));
var pageUrl = 'http://localhost:' + pkg.gruntConfig.connectPort +
              '/lib/widgets/cam-share-link/test/cam-share-link.spec.html';

var page = require('./cam-widget-clipboard.page.js');

describe('cam-share-link widget', function() {
  it('can copy values to the clipboard', function() {
    browser.go(pageUrl);
    browser.sleep(2000);

    element(by.css('a.glyphicon-link')).click();

    var target = element(by.css('#target'));

    target.sendKeys(protractor.Key.chord(browser.controlKey, 'v'));

    expect(
      target.getAttribute('value')
    ).to.eventually.eql(browser.getCurrentUrl());
  });
});
