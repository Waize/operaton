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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.ValueReference;


public abstract class AstProperty extends AstNode {
	private static final String ERROR_PROPERTY_BASE_NULL = "error.property.base.null";
	private static final String ERROR_PROPERTY_METHOD_ACCESS = "error.property.method.access";
	private static final String ERROR_PROPERTY_METHOD_NOTFOUND = "error.property.method.notfound";
	private static final String ERROR_PROPERTY_METHOD_INVOCATION = "error.property.method.invocation";
	private static final String ERROR_PROPERTY_PROPERTY_NOTFOUND = "error.property.property.notfound";
	private static final String ERROR_VALUE_SET_RVALUE = "error.value.set.rvalue";

	protected final AstNode prefix;
	protected final boolean lvalue;
	protected final boolean strict; // allow null as property value?

	protected AstProperty(AstNode prefix, boolean lvalue, boolean strict) {
		this.prefix = prefix;
		this.lvalue = lvalue;
		this.strict = strict;
	}

	protected abstract Object getProperty(Bindings bindings, ELContext context) throws ELException;

	protected AstNode getPrefix() {
		return prefix;
	}

  @Override
  public ValueReference getValueReference(Bindings bindings, ELContext context) {
		return new ValueReference(prefix.eval(bindings, context), getProperty(bindings, context));
	}

	@Override
	public Object eval(Bindings bindings, ELContext context) {
		Object base = prefix.eval(bindings, context);
		if (base == null) {
			return null;
		}
		Object property = getProperty(bindings, context);
		if (property == null && strict) {
			return null;
		}
		context.setPropertyResolved(false);
		Object result = context.getELResolver().getValue(context, base, property);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, property, base));
		}
		return result;
	}

  @Override
  public final boolean isLiteralText() {
		return false;
	}

  @Override
  public final boolean isLeftValue() {
		return lvalue;
	}

  @Override
  public boolean isMethodInvocation() {
		return false;
	}

  @Override
  public Class<?> getType(Bindings bindings, ELContext context) {
		if (!lvalue) {
			return null;
		}
		Object base = prefix.eval(bindings, context);
		if (base == null) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_BASE_NULL, prefix));
		}
		Object property = getProperty(bindings, context);
		if (property == null && strict) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, "null", base));
		}
		context.setPropertyResolved(false);
		Class<?> result = context.getELResolver().getType(context, base, property);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, property, base));
		}
		return result;
	}

  @Override
  public boolean isReadOnly(Bindings bindings, ELContext context) throws ELException {
		if (!lvalue) {
			return true;
		}
		Object base = prefix.eval(bindings, context);
		if (base == null) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_BASE_NULL, prefix));
		}
		Object property = getProperty(bindings, context);
		if (property == null && strict) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, "null", base));
		}
		context.setPropertyResolved(false);
		boolean result = context.getELResolver().isReadOnly(context, base, property);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, property, base));
		}
		return result;
	}

  @Override
  public void setValue(Bindings bindings, ELContext context, Object value) throws ELException {
		if (!lvalue) {
			throw new ELException(LocalMessages.get(ERROR_VALUE_SET_RVALUE, getStructuralId(bindings)));
		}
		Object base = prefix.eval(bindings, context);
		if (base == null) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_BASE_NULL, prefix));
		}
		Object property = getProperty(bindings, context);
		if (property == null && strict) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, "null", base));
		}
		context.setPropertyResolved(false);
		context.getELResolver().setValue(context, base, property, value);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_PROPERTY_NOTFOUND, property, base));
		}
	}

	protected Method findMethod(String name, Class<?> clazz, Class<?> returnType, Class<?>[] paramTypes) {
		Method method = null;
		try {
			method = clazz.getMethod(name, paramTypes);
		} catch (NoSuchMethodException e) {
			throw new MethodNotFoundException(LocalMessages.get(ERROR_PROPERTY_METHOD_NOTFOUND, name, clazz));
		}
		if (returnType != null && !returnType.isAssignableFrom(method.getReturnType())) {
			throw new MethodNotFoundException(LocalMessages.get(ERROR_PROPERTY_METHOD_NOTFOUND, name, clazz));
		}
		return method;
	}

  @Override
  public MethodInfo getMethodInfo(Bindings bindings, ELContext context, Class<?> returnType, Class<?>[] paramTypes) {
		Object base = prefix.eval(bindings, context);
		if (base == null) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_BASE_NULL, prefix));
		}
		Object property = getProperty(bindings, context);
		if (property == null && strict) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_METHOD_NOTFOUND, "null", base));
		}
		String name = bindings.convert(property, String.class);
		Method method = findMethod(name, base.getClass(), returnType, paramTypes);
		return new MethodInfo(method.getName(), method.getReturnType(), paramTypes);
	}

  @Override
  public Object invoke(Bindings bindings, ELContext context, Class<?> returnType, Class<?>[] paramTypes, Object[] paramValues) {
		Object base = prefix.eval(bindings, context);
		if (base == null) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_BASE_NULL, prefix));
		}
		Object property = getProperty(bindings, context);
		if (property == null && strict) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_PROPERTY_METHOD_NOTFOUND, "null", base));
		}
		String name = bindings.convert(property, String.class);
		Method method = findMethod(name, base.getClass(), returnType, paramTypes);
		try {
			return method.invoke(base, paramValues);
		} catch (IllegalAccessException e) {
			throw new ELException(LocalMessages.get(ERROR_PROPERTY_METHOD_ACCESS, name, base.getClass()));
		} catch (IllegalArgumentException e) {
			throw new ELException(LocalMessages.get(ERROR_PROPERTY_METHOD_INVOCATION, name, base.getClass()), e);
		} catch (InvocationTargetException e) {
			throw new ELException(LocalMessages.get(ERROR_PROPERTY_METHOD_INVOCATION, name, base.getClass()), e.getCause());
		}
	}

  @Override
  public AstNode getChild(int i) {
		return i == 0 ? prefix : null;
	}
}
