package com.acme.payroll.common.exception;

import com.acme.payroll.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception,
                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception,
                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(BusinessValidationException exception,
                                                             HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (exception.getField() != null) {
            fieldErrors.put(exception.getField(), exception.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException exception,
                                                         HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                       HttpServletRequest request) {
        String message = "Parameter '" + exception.getName() + "' has an invalid value";
        return build(HttpStatus.BAD_REQUEST, message, request, Map.of(exception.getName(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                         HttpServletRequest request) {
        String message = "Request body is missing, malformed, or contains an unsupported value";
        return build(HttpStatus.BAD_REQUEST, message, request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception,
                                                        HttpServletRequest request) {
        log.warn("Database constraint rejected request: {}", exception.getMostSpecificCause().getMessage());
        String message = "The request conflicts with existing data. "
                + "Check the employee code, email, and salary effective date.";
        return build(HttpStatus.CONFLICT, message, request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request, Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String message,
                                           HttpServletRequest request,
                                           Map<String, String> fieldErrors) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(error);
    }
}
