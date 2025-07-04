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
package org.operaton.bpm.engine.test.bpmn.scripttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.ScriptEvaluationException;
import org.operaton.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptEngineResolver;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;

import com.oracle.truffle.js.scriptengine.GraalJSEngineFactory;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

@Parameterized
public class ScriptTaskGraalJsTest extends AbstractScriptTaskTest {

  private static final String GRAALJS = "graal.js";

  protected ScriptEngineResolver defaultScriptEngineResolver;
  protected boolean spinEnabled = false;

  @BeforeEach
  void setup() {
    spinEnabled = processEngineConfiguration.getEnvScriptResolvers().stream()
                    .anyMatch(resolver -> resolver.getClass().getSimpleName().equals("SpinScriptEnvResolver"));
    defaultScriptEngineResolver = processEngineConfiguration.getScriptEngineResolver();
    processEngineConfiguration.setConfigureScriptEngineHostAccess(configureHostAccess);
    processEngineConfiguration.setEnableScriptEngineLoadExternalResources(enableExternalResources);
    processEngineConfiguration.setEnableScriptEngineNashornCompatibility(enableNashornCompat);
    // create custom script engine lookup to receive a fresh GraalVM JavaScript engine
    processEngineConfiguration.setScriptEngineResolver(new TestScriptEngineResolver(
        processEngineConfiguration.getScriptEngineResolver().getScriptEngineManager()));
  }

  @AfterEach
  void resetConfiguration() {
    processEngineConfiguration.setConfigureScriptEngineHostAccess(true);
    processEngineConfiguration.setEnableScriptEngineNashornCompatibility(false);
    processEngineConfiguration.setEnableScriptEngineLoadExternalResources(false);
    processEngineConfiguration.setScriptEngineResolver(defaultScriptEngineResolver);
  }

  @Parameters
  public static Collection<Object[]> setups() {
    return Arrays.asList(new Object[][] {
      {false, false, false},
      {true, false, false},
      {false, true, false},
      {false, false, true},
      {true, true, false},
      {true, false, true},
      {false, true, true},
      {true, true, true},
    });
  }

  @Parameter(0)
  public boolean configureHostAccess;

  @Parameter(1)
  public boolean enableExternalResources;

  @Parameter(2)
  public boolean enableNashornCompat;

  @TestTemplate
  void testJavascriptProcessVarVisibility() {

    deployProcess(GRAALJS,

        // GIVEN
        // an execution variable 'foo'
        "execution.setVariable('foo', 'a');"

        // THEN
        // there should be a script variable defined
      + "if (typeof foo !== 'undefined') { "
      + "  throw 'Variable foo should be defined as script variable.';"
      + "}"

        // GIVEN
        // a script variable with the same name
      + "var foo = 'b';"

        // THEN
        // it should not change the value of the execution variable
      + "if(execution.getVariable('foo') != 'a') {"
      + "  throw 'Execution should contain variable foo';"
      + "}"

        // AND
        // it should override the visibility of the execution variable
      + "if(foo != 'b') {"
      + "  throw 'Script variable must override the visibiltity of the execution variable.';"
      + "}"

    );

    if (enableNashornCompat || configureHostAccess) {
      // WHEN
      // we start an instance of this process
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

      // THEN
      // the script task can be executed without exceptions
      // the execution variable is stored and has the correct value
      Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
      assertThat(variableValue).isEqualTo("a");
    } else {
      // WHEN
      // we start an instance of this process
      assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      // THEN
      // this is not allowed in the JS ScriptEngine
        .isInstanceOf(ScriptEvaluationException.class)
        .hasMessageContaining(spinEnabled ? "ReferenceError" : "TypeError");
    }
  }

  @TestTemplate
  void testJavascriptFunctionInvocation() {

    deployProcess(GRAALJS,

        // GIVEN
        // a function named sum
        "function sum(a,b){"
      + "  return a+b;"
      + "};"

        // THEN
        // i can call the function
      + "var result = sum(1,2);"

      + "execution.setVariable('foo', result);"

    );

    if (enableNashornCompat || configureHostAccess) {
      // WHEN
      // we start an instance of this process
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

      // THEN
      // the variable is defined
      Object variable = runtimeService.getVariable(pi.getId(), "foo");
      assertThat(variable).isIn(3, 3.0);
    } else {
      // WHEN
      // we start an instance of this process
      assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      // THEN
      // this is not allowed in the JS ScriptEngine
        .isInstanceOf(ScriptEvaluationException.class)
        .hasMessageContaining(spinEnabled ? "ReferenceError" : "TypeError");
    }

  }

  @TestTemplate
  void testJsVariable() {

    String scriptText = "var foo = 1;";

    deployProcess(GRAALJS, scriptText);

    if (spinEnabled && !enableNashornCompat && !configureHostAccess) {
      // WHEN
      // we start an instance of this process
      assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      // THEN
      // this Java access is not allowed for Spin Environment Script
        .isInstanceOf(ScriptEvaluationException.class)
        .hasMessageContaining("ReferenceError");
    } else {
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");
      Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
      assertThat(variableValue).isNull();
    }

  }

  @TestTemplate
  void testJavascriptVariableSerialization() {
    deployProcess(GRAALJS,
        // GIVEN
        // setting Java classes as variables
        "execution.setVariable('date', new java.util.Date(0));"
      + "execution.setVariable('myVar', new org.operaton.bpm.engine.test.bpmn.scripttask.MySerializable('test'));");

    if (enableNashornCompat || configureHostAccess) {
      // WHEN
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

      // THEN
      Date date = (Date) runtimeService.getVariable(pi.getId(), "date");
      assertThat(date.getTime()).isZero();
      MySerializable myVar = (MySerializable) runtimeService.getVariable(pi.getId(), "myVar");
      assertThat(myVar.getName()).isEqualTo("test");
    } else {
      // WHEN
      // we start an instance of this process
      assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      // THEN
      // this is not allowed in the JS ScriptEngine
        .isInstanceOf(ScriptEvaluationException.class)
        .hasMessageContaining("ReferenceError");
    }
  }

  @TestTemplate
  void shouldLoadExternalScript() {
      // GIVEN
      // an external JS file with a function
      deployProcess(GRAALJS,
          // WHEN
          // we load a function from an external file
          "load(\"" + getNormalizedResourcePath("/org/operaton/bpm/engine/test/bpmn/scripttask/sum.js") + "\");"
          // THEN
          // we can use that function
        + "execution.setVariable('foo', sum(3, 4));"
      );

      if (enableNashornCompat || (enableExternalResources && configureHostAccess)) {
        // WHEN
        // we start an instance of this process
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

        // THEN
        // the script task can be executed without exceptions
        // the execution variable is stored and has the correct value
        Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
        assertThat(variableValue).isEqualTo(7);
      } else {
        // WHEN
        // we start an instance of this process
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
        // THEN
        // this is not allowed in the JS ScriptEngine
          .isInstanceOf(ScriptEvaluationException.class)
          .hasMessageContaining(
              (spinEnabled && !configureHostAccess) ? "ReferenceError" :
              (enableExternalResources && !configureHostAccess) ? "TypeError" :
              "Operation is not allowed");
      }
  }

  protected static class TestScriptEngineResolver extends DefaultScriptEngineResolver {

    public TestScriptEngineResolver(ScriptEngineManager scriptEngineManager) {
      super(scriptEngineManager);
    }

    @Override
    protected ScriptEngine getScriptEngine(String language) {
      if (GRAALJS.equalsIgnoreCase(language)) {
        GraalJSScriptEngine scriptEngine = new GraalJSEngineFactory().getScriptEngine();
        configureScriptEngines(language, scriptEngine);
        return scriptEngine;
      }
      return super.getScriptEngine(language);
    }
  }

}
