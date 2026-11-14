package com.smartbackup.exception;

import com.smartbackup.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation échouée", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation échouée", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "Corps de requête invalide", null, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST,
                "Paramètre invalide : " + exception.getName(), null, request);
    }

    @ExceptionHandler(BackupInProgressException.class)
    public ResponseEntity<ApiErrorResponse> handleBackupInProgress(
            BackupInProgressException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null, request);
    }

    @ExceptionHandler(RestoreInProgressException.class)
    public ResponseEntity<ApiErrorResponse> handleRestoreInProgress(
            RestoreInProgressException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null, request);
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleConflict(
            ScheduleConflictException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null, request);
    }

    @ExceptionHandler(BackupIntegrityException.class)
    public ResponseEntity<ApiErrorResponse> handleBackupIntegrity(
            BackupIntegrityException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, "Identifiants incorrects", null, request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            InvalidTokenException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), null, request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(
            DisabledException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "Compte désactivé", null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Accès refusé"
                : exception.getMessage();
        return build(HttpStatus.FORBIDDEN, message, null, request);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyUsed(
            EmailAlreadyUsedException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), null, request);
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ApiErrorResponse> handleFileTooLarge(
            FileTooLargeException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage(), null, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux", null, request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "Aucun fichier fourni", null, request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(
            MultipartException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "Requête multipart invalide", null, request);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFileType(
            InvalidFileTypeException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), null, request);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleQuotaExceeded(
            QuotaExceededException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), null, request);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorage(
            StorageException exception,
            HttpServletRequest request
    ) {
        log.error("Erreur de stockage sur {} {}", request.getMethod(), request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur", null, request);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            List<String> details,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
