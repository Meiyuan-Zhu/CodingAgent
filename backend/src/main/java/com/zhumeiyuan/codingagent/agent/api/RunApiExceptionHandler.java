package com.zhumeiyuan.codingagent.agent.api;

import com.zhumeiyuan.codingagent.agent.execution.AgentRunNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class RunApiExceptionHandler {

	@ExceptionHandler(AgentRunNotFoundException.class)
	ResponseEntity<ErrorResponse> notFound(AgentRunNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("run_not_found", ex.getMessage()));
	}

	@ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class })
	ResponseEntity<ErrorResponse> badRequest(Exception ex) {
		return ResponseEntity.badRequest()
				.body(new ErrorResponse("bad_request", message(ex)));
	}

	private String message(Exception ex) {
		if (ex instanceof MethodArgumentNotValidException validationException) {
			return validationException.getBindingResult().getFieldErrors().stream()
					.findFirst()
					.map(error -> error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage())
					.orElse("Invalid request");
		}
		return ex.getMessage() == null ? "Invalid request" : ex.getMessage();
	}
}
