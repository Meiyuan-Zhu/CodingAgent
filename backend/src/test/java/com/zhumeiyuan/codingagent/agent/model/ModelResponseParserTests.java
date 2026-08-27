package com.zhumeiyuan.codingagent.agent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class ModelResponseParserTests {

	private final ModelResponseParser parser = new ModelResponseParser(new ObjectMapper());

	@Test
	void parsesFinalMessageWithoutToolCalls() {
		ModelResponse response = this.parser.parse("""
				{
				  "message": "Done",
				  "finish_reason": "stop"
				}
				""");

		assertThat(response.message()).isEqualTo("Done");
		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.STOP);
		assertThat(response.toolCalls()).isEmpty();
	}

	@Test
	void parsesToolCallsWithObjectArguments() {
		ModelResponse response = this.parser.parse("""
				{
				  "message": "Need to inspect files",
				  "finish_reason": "tool_calls",
				  "tool_calls": [
				    {
				      "id": "call-1",
				      "name": "list_files",
				      "arguments": {
				        "path": ".",
				        "max_entries": 10
				      }
				    }
				  ]
				}
				""");

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.id()).isEqualTo("call-1");
			assertThat(call.name()).isEqualTo("list_files");
			assertThat(call.arguments()).containsEntry("path", ".").containsEntry("max_entries", 10);
		});
	}

	@Test
	void rejectsMalformedJsonAndNonObjectRoot() {
		assertParseError(() -> this.parser.parse("{bad json"));
		assertParseError(() -> this.parser.parse("[1,2,3]"));
	}

	@Test
	void rejectsUnknownFinishReason() {
		assertParseError(() -> this.parser.parse("""
				{"message":"x","finish_reason":"strange"}
				"""));
	}

	@Test
	void rejectsToolCallsWhenFinishReasonIsStop() {
		assertParseError(() -> this.parser.parse("""
				{
				  "message": "Done",
				  "finish_reason": "stop",
				  "tool_calls": [
				    {"id":"call-1","name":"list_files","arguments":{"path":"."}}
				  ]
				}
				"""));
	}

	@Test
	void rejectsToolCallsMissingArgumentsObject() {
		assertParseError(() -> this.parser.parse("""
				{
				  "message": "Need tool",
				  "finish_reason": "tool_calls",
				  "tool_calls": [
				    {"id":"call-1","name":"list_files","arguments":"bad"}
				  ]
				}
				"""));
	}

	@Test
	void rejectsDuplicateToolCallIds() {
		assertParseError(() -> this.parser.parse("""
				{
				  "message": "Need tools",
				  "finish_reason": "tool_calls",
				  "tool_calls": [
				    {"id":"call-1","name":"list_files","arguments":{"path":"."}},
				    {"id":"call-1","name":"read_file","arguments":{"path":"README.md"}}
				  ]
				}
				"""));
	}

	@Test
	void rejectsTooManyToolCalls() {
		StringBuilder calls = new StringBuilder();
		for (int index = 0; index < 9; index++) {
			if (index > 0) {
				calls.append(',');
			}
			calls.append("""
					{"id":"call-%d","name":"list_files","arguments":{"path":"."}}
					""".formatted(index));
		}

		assertParseError(() -> this.parser.parse("""
				{"message":"Need tools","finish_reason":"tool_calls","tool_calls":[%s]}
				""".formatted(calls)));
	}

	private void assertParseError(Runnable action) {
		assertThatThrownBy(action::run).isInstanceOf(ModelParseException.class);
	}
}
