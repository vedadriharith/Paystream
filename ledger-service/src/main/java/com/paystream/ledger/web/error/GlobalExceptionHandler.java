package com.paystream.ledger.web.error;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.paystream.ledger.exception.LedgerAccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converts every exception into an RFC 9457 Problem Details response with a stable errorCode,
 * matching payment-service's error format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(LedgerAccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(LedgerAccountNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Ledger account not found", "LEDGER_ACCOUNT_NOT_FOUND",
                ex.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<String> violations = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setTitle("Invalid request");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", violations);

        log.warn("Request parameter validation failed: {}", violations);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error {} on {} {}", errorId, request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "INTERNAL_ERROR",
                "An unexpected error occurred. Quote error id " + errorId + " when contacting support.", request);
        problem.setProperty("errorId", errorId);
        return problem;
    }

    private ProblemDetail problem(HttpStatusCode status,
                                  String title,
                                  String errorCode,
                                  String detail,
                                  HttpServletRequest request) {
        if (status.is4xxClientError()) {
            log.warn("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(), status.value(), errorCode, detail);
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", errorCode);
        return problem;
    }
}