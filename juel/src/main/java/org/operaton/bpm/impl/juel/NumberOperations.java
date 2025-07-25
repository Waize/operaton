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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Arithmetic Operations as specified in chapter 1.7.
 *
 * @author Christoph Beck
 */
public class NumberOperations {
	private static final Long LONG_ZERO = 0L;

	private NumberOperations() {
	}

	private static boolean isDotEe(String value) {
		int length = value.length();
		for (int i = 0; i < length; i++) {
			switch (value.charAt(i)) {
				case '.', 'E', 'e': return true;
			}
		}
		return false;
	}

	private static boolean isDotEe(Object value) {
		return value instanceof String stringValue && isDotEe(stringValue);
	}

	private static boolean isFloatOrDouble(Object value) {
		return value instanceof Float || value instanceof Double;
	}

	private static boolean isFloatOrDoubleOrDotEe(Object value) {
		return isFloatOrDouble(value) || isDotEe(value);
	}

	private static boolean isBigDecimalOrBigInteger(Object value) {
		return value instanceof BigDecimal || value instanceof BigInteger;
	}

	private static boolean isBigDecimalOrFloatOrDoubleOrDotEe(Object value) {
		return value instanceof BigDecimal || isFloatOrDoubleOrDotEe(value);
	}

	public static final Number add(TypeConverter converter, Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return LONG_ZERO;
		}
		if (o1 instanceof BigDecimal || o2 instanceof BigDecimal) {
			return converter.convert(o1, BigDecimal.class).add(converter.convert(o2, BigDecimal.class));
		}
		if (isFloatOrDoubleOrDotEe(o1) || isFloatOrDoubleOrDotEe(o2)) {
			if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
				return converter.convert(o1, BigDecimal.class).add(converter.convert(o2, BigDecimal.class));
			}
			return converter.convert(o1, Double.class) + converter.convert(o2, Double.class);
		}
		if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
			return converter.convert(o1, BigInteger.class).add(converter.convert(o2, BigInteger.class));
		}
		return converter.convert(o1, Long.class) + converter.convert(o2, Long.class);
	}

	public static final Number sub(TypeConverter converter, Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return LONG_ZERO;
		}
		if (o1 instanceof BigDecimal || o2 instanceof BigDecimal) {
			return converter.convert(o1, BigDecimal.class).subtract(converter.convert(o2, BigDecimal.class));
		}
		if (isFloatOrDoubleOrDotEe(o1) || isFloatOrDoubleOrDotEe(o2)) {
			if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
				return converter.convert(o1, BigDecimal.class).subtract(converter.convert(o2, BigDecimal.class));
			}
			return converter.convert(o1, Double.class) - converter.convert(o2, Double.class);
		}
		if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
			return converter.convert(o1, BigInteger.class).subtract(converter.convert(o2, BigInteger.class));
		}
		return converter.convert(o1, Long.class) - converter.convert(o2, Long.class);
	}

	public static final Number mul(TypeConverter converter, Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return LONG_ZERO;
		}
		if (o1 instanceof BigDecimal || o2 instanceof BigDecimal) {
			return converter.convert(o1, BigDecimal.class).multiply(converter.convert(o2, BigDecimal.class));
		}
		if (isFloatOrDoubleOrDotEe(o1) || isFloatOrDoubleOrDotEe(o2)) {
			if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
				return converter.convert(o1, BigDecimal.class).multiply(converter.convert(o2, BigDecimal.class));
			}
			return converter.convert(o1, Double.class) * converter.convert(o2, Double.class);
		}
		if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
			return converter.convert(o1, BigInteger.class).multiply(converter.convert(o2, BigInteger.class));
		}
		return converter.convert(o1, Long.class) * converter.convert(o2, Long.class);
	}

	public static final Number div(TypeConverter converter, Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return LONG_ZERO;
		}
		if (isBigDecimalOrBigInteger(o1) || isBigDecimalOrBigInteger(o2)) {
			return converter.convert(o1, BigDecimal.class).divide(converter.convert(o2, BigDecimal.class), RoundingMode.HALF_UP);
		}
		return converter.convert(o1, Double.class) / converter.convert(o2, Double.class);
	}

	public static final Number mod(TypeConverter converter, Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return LONG_ZERO;
		}
		if (isBigDecimalOrFloatOrDoubleOrDotEe(o1) || isBigDecimalOrFloatOrDoubleOrDotEe(o2)) {
			return converter.convert(o1, Double.class) % converter.convert(o2, Double.class);
		}
		if (o1 instanceof BigInteger || o2 instanceof BigInteger) {
			return converter.convert(o1, BigInteger.class).remainder(converter.convert(o2, BigInteger.class));
		}
		return converter.convert(o1, Long.class) % converter.convert(o2, Long.class);
	}

	public static final Number neg(TypeConverter converter, Object value) {
		if (value == null) {
			return LONG_ZERO;
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal.negate();
		}
		if (value instanceof BigInteger bigInteger) {
			return bigInteger.negate();
		}
		if (value instanceof Double doubleValue) {
			return -doubleValue.doubleValue();
		}
		if (value instanceof Float floatValue) {
			return -floatValue.floatValue();
		}
		if (value instanceof String stringValue) {
			if (isDotEe(stringValue)) {
				return -converter.convert(value, Double.class).doubleValue();
			}
			return -converter.convert(value, Long.class).longValue();
		}
		if (value instanceof Long longValue) {
			return -longValue.longValue();
		}
		if (value instanceof Integer integerValue) {
			return -integerValue.intValue();
		}
		if (value instanceof Short shortValue) {
			return (short) -shortValue.shortValue();
		}
		if (value instanceof Byte byteValue) {
			return (byte) -byteValue.byteValue();
		}
		throw new ELException(LocalMessages.get("error.negate", value.getClass()));
	}
}
