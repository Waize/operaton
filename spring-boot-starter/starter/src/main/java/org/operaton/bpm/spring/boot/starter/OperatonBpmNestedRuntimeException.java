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
package org.operaton.bpm.spring.boot.starter;

import org.springframework.core.NestedRuntimeException;

/**
 * Custom runtime exception that wraps a checked exception.
 * <p>
 * This class can be used when it is necessary to avoid the explicit throwing of checked exceptions.
 * </p>
 */
public class OperatonBpmNestedRuntimeException extends NestedRuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception with the specified detail message and nested exception.
   *
   * @param msg the exception message
   */
  public OperatonBpmNestedRuntimeException(final String msg) {
    super(msg);
  }

  /**
   * Construct an exception with the specified detail message and nested exception.
   *
   * @param msg the exception message
   * @param cause the nested exception
   */
  public OperatonBpmNestedRuntimeException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

}
