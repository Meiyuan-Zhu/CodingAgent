package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RunBudgetTests {

	@Test
	void rejectsNonPositiveRoundLimit() {
		assertThatThrownBy(() -> new RunBudget(0, 1, 2)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNonPositiveToolLimit() {
		assertThatThrownBy(() -> new RunBudget(1, 0, 2)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void contextWindowMustHaveRoomForAtLeastTwoMessages() {
		assertThatThrownBy(() -> new RunBudget(1, 1, 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNonPositiveToolTimeout() {
		assertThatThrownBy(() -> new RunBudget(1, 1, 2, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
	}
}
