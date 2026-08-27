package com.zhumeiyuan.codingagent.agent.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ToolJson {

	private ToolJson() {
	}

	static Map<String, Object> deepCopyObject(Map<String, Object> source) {
		Map<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
		}
		return Collections.unmodifiableMap(copy);
	}

	private static Object deepCopyValue(Object value) {
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> nested = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (!(entry.getKey() instanceof String key)) {
					throw new IllegalArgumentException("Tool JSON object keys must be strings");
				}
				nested.put(key, deepCopyValue(entry.getValue()));
			}
			return Collections.unmodifiableMap(nested);
		}
		if (value instanceof List<?> list) {
			List<Object> nested = new ArrayList<>();
			for (Object item : list) {
				nested.add(deepCopyValue(item));
			}
			return Collections.unmodifiableList(nested);
		}
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof Object[] array) {
			List<Object> nested = new ArrayList<>();
			for (Object item : array) {
				nested.add(deepCopyValue(item));
			}
			return Collections.unmodifiableList(nested);
		}
		throw new IllegalArgumentException("Unsupported tool JSON value type: " + value.getClass().getName());
	}
}
