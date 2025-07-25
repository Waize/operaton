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
package org.operaton.spin.impl.json.jackson.format;

import org.operaton.spin.spi.TypeDetector;

import java.util.Set;

import static org.operaton.spin.impl.json.jackson.format.TypeHelper.constructType;

/**
 * Detects erased types of Set classes.
 * <p>To use it, make sure to call {@link JacksonJsonDataFormat#addTypeDetector(TypeDetector)}
 * to activate it.</p>
 */
public class SetJacksonJsonTypeDetector extends AbstractJacksonJsonTypeDetector {

    /**
     * Object instance to use.
     */
    public static final SetJacksonJsonTypeDetector INSTANCE = new SetJacksonJsonTypeDetector();

    @Override
    public boolean canHandle(Object value) {
        return value instanceof Set<?>;
    }

    @Override
    public String detectType(Object value) {
        return constructType(value).toCanonical();
    }
}
