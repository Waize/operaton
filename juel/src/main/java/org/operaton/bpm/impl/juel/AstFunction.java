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

import jakarta.el.ELContext;
import jakarta.el.ELException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class AstFunction extends AstRightValue implements FunctionNode {
	private final int index;
	private final String name;
	private final AstParameters params;
	private final boolean varargs;

	public AstFunction(String name, int index, AstParameters params) {
		this(name, index, params, false);
	}

	public AstFunction(String name, int index, AstParameters params, boolean varargs) {
		this.name = name;
		this.index = index;
		this.params = params;
		this.varargs = varargs;
	}

	/**
	 * Invoke method.
	 * @param bindings
	 * @param context
	 * @param base
	 * @param method
	 * @return method result
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	protected Object invoke(Bindings bindings, ELContext context, Object base, Method method)
		throws InvocationTargetException, IllegalAccessException {
		Class<?>[] types = method.getParameterTypes();
		Object[] invocationParams = null;
		if (types.length > 0) {
			invocationParams = new Object[types.length];
			if (varargs && method.isVarArgs()) {
				for (int i = 0; i < invocationParams.length - 1; i++) {
					Object param = getParam(i).eval(bindings, context);
					if (param != null || types[i].isPrimitive()) {
						invocationParams[i] = bindings.convert(param, types[i]);
					}
				}
				int varargIndex = types.length - 1;
				Class<?> varargType = types[varargIndex].getComponentType();
				int length = getParamCount() - varargIndex;
				Object array = null;
				if (length == 1) { // special: eventually use argument as is
					Object param = getParam(varargIndex).eval(bindings, context);
					if (param != null && param.getClass().isArray()) {
						if (types[varargIndex].isInstance(param)) {
							array = param;
						} else { // coerce array elements
							length = Array.getLength(param);
							array = Array.newInstance(varargType, length);
							for (int i = 0; i < length; i++) {
								Object elem = Array.get(param, i);
								if (elem != null || varargType.isPrimitive()) {
									Array.set(array, i, bindings.convert(elem, varargType));
								}
							}
						}
					} else { // single element array
						array = Array.newInstance(varargType, 1);
						if (param != null || varargType.isPrimitive()) {
							Array.set(array, 0, bindings.convert(param, varargType));
						}
					}
				} else {
					array = Array.newInstance(varargType, length);
					for (int i = 0; i < length; i++) {
						Object param = getParam(varargIndex + i).eval(bindings, context);
						if (param != null || varargType.isPrimitive()) {
							Array.set(array, i, bindings.convert(param, varargType));
						}
					}
				}
				invocationParams[varargIndex] = array;
			} else {
				for (int i = 0; i < invocationParams.length; i++) {
					Object param = getParam(i).eval(bindings, context);
					if (param != null || types[i].isPrimitive()) {
						invocationParams[i] = bindings.convert(param, types[i]);
					}
				}
			}
		}
		return method.invoke(base, invocationParams);
	}

	@Override
	public Object eval(Bindings bindings, ELContext context) {
		Method method = bindings.getFunction(index);
		try {
			return invoke(bindings, context, null, method);
		} catch (IllegalAccessException e) {
			throw new ELException(LocalMessages.get("error.function.access", name), e);
		} catch (InvocationTargetException e) {
			throw new ELException(LocalMessages.get("error.function.invocation", name), e.getCause());
		}
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void appendStructure(StringBuilder b, Bindings bindings) {
		b.append(bindings != null && bindings.isFunctionBound(index) ? "<fn>" : name);
		params.appendStructure(b, bindings);
	}

  @Override
  public int getIndex() {
		return index;
	}

  @Override
  public String getName() {
		return name;
	}

  @Override
  public boolean isVarArgs() {
		return varargs;
	}

  @Override
  public int getParamCount() {
		return params.getCardinality();
	}

	protected AstNode getParam(int i) {
		return params.getChild(i);
	}

  @Override
  public int getCardinality() {
		return 1;
	}

  @Override
  public AstNode getChild(int i) {
		return i == 0 ? params : null;
	}
}
