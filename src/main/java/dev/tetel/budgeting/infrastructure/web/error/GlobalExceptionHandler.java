package dev.tetel.budgeting.infrastructure.web.error;

import dev.tetel.budgeting.infrastructure.audio.InvalidAudioUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Central place that turns every exception into the stable {@link ErrorResponse} shape.
 * This is what keeps stack traces, SQL, and internal class names out of HTTP responses
 * (see docs/security/security-controls.md, "safe error handling").
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = ErrorResponse.of("VALIDATION_ERROR", message.isBlank() ? "Invalid request." : message);
        log.warn("operation=validation outcome=rejected code={} requestId={}", error.code(), error.requestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.of("INVALID_ARGUMENT", "The request contains an invalid value.");
        log.warn("operation=domain-validation outcome=rejected code={} requestId={}", error.code(), error.requestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        ErrorResponse error = ErrorResponse.of("MISSING_PARAMETER", "A required request parameter is missing.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = ErrorResponse.of("INVALID_PARAMETER", "A request parameter has an invalid value.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        ErrorResponse error = ErrorResponse.of("MALFORMED_REQUEST", "The request body could not be read.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidAudioUploadException.class)
    ResponseEntity<ErrorResponse> handleInvalidAudioUpload(InvalidAudioUploadException ex) {
        ErrorResponse error = ErrorResponse.of("INVALID_AUDIO_UPLOAD", ex.getMessage());
        log.warn("operation=audio-upload outcome=rejected code={} requestId={}", error.code(), error.requestId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        ErrorResponse error = ErrorResponse.of("UPLOAD_TOO_LARGE", "The uploaded file exceeds the allowed size.");
        log.warn("operation=audio-upload outcome=rejected code={} requestId={}", error.code(), error.requestId());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = ErrorResponse.of("METHOD_NOT_ALLOWED", "This HTTP method is not supported for this endpoint.");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        ErrorResponse error = ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred.");
        log.error("operation=unexpected-error code={} requestId={}", error.code(), error.requestId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
