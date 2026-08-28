package com.zhumeiyuan.codingagent.agent.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ToolArgumentReader {

	private final String toolName;
	private final Map<String, Object> arguments;

	public ToolArgumentReader(String toolName, Map<String, Object> arguments) {
		if (toolName == null || toolName.isBlank()) {
			throw new IllegalArgumentException("toolName must not be blank");
		}
		this.toolName = toolName;
		this.arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
	}

	public void rejectUnexpected(Set<String> allowedNames) {
		Objects.requireNonNull(allowedNames, "allowedNames");
		for (String name : this.arguments.keySet()) {
			if (!allowedNames.contains(name)) {
				throw invalid("Unexpected argument: " + name);
			}
		}
	}

	public String requiredString(String name) {
		Object value = this.arguments.get(name);
		if (!(value instanceof String stringValue) || stringValue.isBlank()) {
			throw invalid("Argument '" + name + "' must be a non-blank string");
		}
		return stringValue;
	}

	public String requiredText(String name) {
		Object value = this.arguments.get(name);
		if (!(value instanceof String stringValue)) {
			throw invalid("Argument '" + name + "' must be a string");
		}
		return stringValue;
	}

	public String optionalString(String name, String defaultValue) {
		Object value = this.arguments.get(name);
		if (value == null) {
			return defaultValue;
		}
		if (!(value instanceof String stringValue) || stringValue.isBlank()) {
			throw invalid("Argument '" + name + "' must be a non-blank string");
		}
		return stringValue;
	}

	public List<String> requiredStringList(String name, int maxSize) {
		Object value = this.arguments.get(name);
		if (!(value instanceof List<?> listValue) || listValue.isEmpty()) {
			throw invalid("Argument '" + name + "' must be a non-empty string array");
		}
		if (listValue.size() > maxSize) {
			throw invalid("Argument '" + name + "' must contain at most " + maxSize + " items");
		}
		List<String> strings = new ArrayList<>();
		for (Object item : listValue) {
			if (!(item instanceof String stringValue) || stringValue.isBlank()) {
				throw invalid("Argument '" + name + "' must contain only non-blank strings");
			}
			strings.add(stringValue);
		}
		return List.copyOf(strings);
	}

	public boolean optionalBoolean(String name, boolean defaultValue) {
		Object value = this.arguments.get(name);
		if (value == null) {
			return defaultValue;
		}
		if (!(value instanceof Boolean booleanValue)) {
			throw invalid("Argument '" + name + "' must be a boolean");
		}
		return booleanValue;
	}

	public int optionalPositiveInt(String name, int defaultValue, int maxValue) {
		Object value = this.arguments.get(name);
		if (value == null) {
			return defaultValue;
		}
		int intValue = toInteger(name, value);
		if (intValue <= 0 || intValue > maxValue) {
			throw invalid("Argument '" + name + "' must be between 1 and " + maxValue);
		}
		return intValue;
	}

	private int toInteger(String name, Object value) {
		if (value instanceof Integer intValue) {
			return intValue;
		}
		if (value instanceof Long longValue && longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
			return longValue.intValue();
		}
		throw invalid("Argument '" + name + "' must be an integer");
	}

	private ToolExecutionException invalid(String message) {
		return new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, this.toolName, message);
	}
}
