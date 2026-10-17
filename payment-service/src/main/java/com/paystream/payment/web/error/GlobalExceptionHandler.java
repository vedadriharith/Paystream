package com.paystream.payment.web.error;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.paystream.payment.exception.AccountNotFoundException;
import com.paystream.payment.exception.IdempotencyKeyReuseException;
import com.paystream.payment.exception.PaymentNotFoundException;
import com.paystream.payment.exception.PaymentValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Converts every exception into an RFC 9457 Problem Details response with a stable errorCode.
 * Extends ResponseEntityExceptionHandler so Spring's own exceptions (missing header, bad JSON,
 * type mismatch) also use the same format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** 422: the request is well-formed but breaks a business rule. */
    private static final HttpStatusCode UNPROCESSABLE = HttpStatusCode.valueOf(422);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setTitle("Invalid request");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", violations);

        log.warn("Request validation failed: {}", violations);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail handlePaymentNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Payment not found", "PAYMENT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        return problem(UNPROCESSABLE, "Account not found", "ACCOUNT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentValidationException.class)
    ProblemDetail handlePaymentValidation(PaymentValidationException ex, HttpServletRequest request) {
        return problem(UNPROCESSABLE, "Payment rejected", "PAYMENT_REJECTED", ex.getMessage(), request);
    }

    /** Temporary mapping until Phase 2 replaces duplicate-key handling with idempotent replay. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", "DATA_CONFLICT",
                "The request conflicts with existing data", request);
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

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    ProblemDetail handleIdempotencyKeyReuse(IdempotencyKeyReuseException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Idempotency-Key reused", "IDEMPOTENCY_KEY_REUSED",
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

    /** One rejected field in a request body. */
    record FieldViolation(String field, String message) {
    }
}