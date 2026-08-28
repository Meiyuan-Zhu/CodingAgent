package com.zhumeiyuan.codingagent.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ToolArgumentReaderTests {

	@Test
	void rejectsUnexpectedArguments() {
		ToolArgumentReader reader = new ToolArgumentReader("demo_tool", Map.of("path", "README.md", "extra", true));

		assertToolError(() -> reader.rejectUnexpected(Set.of("path")), ToolExecutionErrorCode.INVALID_ARGUMENTS);
	}

	@Test
	void rejectsMissingOrWrongStringArguments() {
		ToolArgumentReader missing = new ToolArgumentReader("demo_tool", Map.of());
		ToolArgumentReader wrongType = new ToolArgumentReader("demo_tool", Map.of("path", 123));

		assertToolError(() -> missing.requiredString("path"), ToolExecutionErrorCode.INVALID_ARGUMENTS);
		assertToolError(() -> wrongType.requiredString("path"), ToolExecutionErrorCode.INVALID_ARGUMENTS);
	}

	@Test
	void requiredTextAllowsEmptyStringButRejectsNonString() {
		ToolArgumentReader empty = new ToolArgumentReader("demo_tool", Map.of("content", ""));
		ToolArgumentReader wrongType = new ToolArgumentReader("demo_tool", Map.of("content", 123));

		assertThatCode(() -> empty.requiredText("content")).doesNotThrowAnyException();
		assertToolError(() -> wrongType.requiredText("content"), ToolExecutionErrorCode.INVALID_ARGUMENTS);
	}

	@Test
	void requiredStringListCopiesValidStringsAndRejectsBadItems() {
		ToolArgumentReader valid = new ToolArgumentReader("demo_tool", Map.of("command", List.of("mvn", "test")));
		ToolArgumentReader blank = new ToolArgumentReader("demo_tool", Map.of("command", List.of("mvn", " ")));
		ToolArgumentReader wrong = new ToolArgumentReader("demo_tool", Map.of("command", "mvn test"));

		assertThat(valid.requiredStringList("command", 4)).containsExactly("mvn", "test");
		assertToolError(() -> blank.requiredStringList("command", 4), ToolExecutionErrorCode.INVALID_ARGUMENTS);
		assertToolError(() -> wrong.requiredStringList("command", 4), ToolExecutionErrorCode.INVALID_ARGUMENTS);
	}

	@Test
	void optionalBooleanRejectsNonBoolean() {
		ToolArgumentReader reader = new ToolArgumentReader("demo_tool", Map.of("overwrite", "true"));

		assertToolError(() -> reader.optionalBoolean("overwrite", false), ToolExecutionErrorCode.INVALID_ARGUMENTS);
	}

	@Test
	void rejectsNonIntegerAndOutOfRangeNumbers() {
		ToolArgumentReader decimal = new ToolArgumentReader("demo_tool", Map.of("max", 1.5));
		ToolArgumentReader tooLarge = new ToolArgumentReader("demo_tool", Map.of("max", 101));

		assertToolError(() -> decimal.optionalPositiveInt("max", 10, 100), ToolExecutionErrorCode.INVALID_ARGUMENTS);
		assertToolError(() -> tooLarge.optionalPositiveInt("max", 10, 100), ToolExecutionErrorCode.INVALID_ARGUMENTS);
	}

	private void assertToolError(Runnable action, ToolExecutionErrorCode code) {
		assertThatThrownBy(action::run)
				.isInstanceOf(ToolExecutionException.class)
				.extracting(ex -> ((ToolExecutionException) ex).code())
				.isEqualTo(code);
	}
}
