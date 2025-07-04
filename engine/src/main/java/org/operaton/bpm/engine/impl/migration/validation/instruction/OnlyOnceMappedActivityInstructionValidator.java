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
package org.operaton.bpm.engine.impl.migration.validation.instruction;

import java.util.List;

import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.util.StringUtil;

public class OnlyOnceMappedActivityInstructionValidator implements MigrationInstructionValidator {

  @Override
  public void validate(ValidatingMigrationInstruction instruction, ValidatingMigrationInstructions instructions, MigrationInstructionValidationReportImpl report) {
    ActivityImpl sourceActivity = instruction.getSourceActivity();
    List<ValidatingMigrationInstruction> instructionsForSourceActivity = instructions.getInstructionsBySourceScope(sourceActivity);

    if (instructionsForSourceActivity.size() > 1) {
      addFailure(sourceActivity.getId(), instructionsForSourceActivity, report);
    }
  }

  protected void addFailure(String sourceActivityId, List<ValidatingMigrationInstruction> migrationInstructions, MigrationInstructionValidationReportImpl report) {
    report.addFailure("There are multiple mappings for source activity id '" + sourceActivityId +"': " +
      StringUtil.join(new StringUtil.StringIterator<ValidatingMigrationInstruction>(migrationInstructions.iterator()) {
        @Override
        public String next() {
          return iterator.next().toString();
        }
      }));
  }

}
