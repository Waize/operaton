/*
 * Based on JUEL 2.2.1 code, 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.impl.juel;

import jakarta.el.ELException;
import java.io.Serializable;

/**
 * Tree builder interface.
 * A tree builder can be used to create arbitrary many trees. Furthermore, a tree builder
 * implementation must be thread-safe.
 *
 * @author Christoph Beck
 */
public interface TreeBuilder extends Serializable {
  /**
   * Parse the given expression and create an abstract syntax tree for it.
   *
   * @param expression expression string
   * @return tree corresponding to the given expression
   * @throws ELException on parse error
   */
  Tree build(String expression) throws TreeBuilderException;
}
