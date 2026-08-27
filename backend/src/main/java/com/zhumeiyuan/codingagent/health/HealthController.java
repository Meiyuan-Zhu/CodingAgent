package com.zhumeiyuan.codingagent.health;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class HealthController {

	@GetMapping("/health")
	HealthResponse health() {
		return new HealthResponse("ok", "coding-agent-backend", Runtime.version().feature(), Instant.now());
	}

	record HealthResponse(String status, String service, int javaVersion, Instant serverTime) {
	}
}
