package io.transatron.transaction.manager.controller.advice;

import io.transatron.transaction.manager.controller.dto.ErrorResponse;
import io.transatron.transaction.manager.domain.exception.BadRequestException;
import io.transatron.transaction.manager.domain.exception.ErrorsTable;
import io.transatron.transaction.manager.domain.exception.ForbiddenException;
import io.transatron.transaction.manager.domain.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;

import static io.transatron.transaction.manager.domain.exception.ErrorsTable.RESOURCE_NOT_FOUND;
import static io.transatron.transaction.manager.domain.exception.ErrorsTable.VALIDATION_FAILED;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlingControllerAdvice {

    private static final String COLON_SEPARATOR = ": ";
    private static final String SEMICOLON_SEPARATOR = "; ";

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        var errorMessage = ex.getConstraintViolations().stream()
                             .map(violation -> violation.getMessage() + COLON_SEPARATOR + violation.getInvalidValue())
                             .sorted()
                             .collect(joining(SEMICOLON_SEPARATOR));
        log.debug("ConstraintViolationException happened: {}", errorMessage);
        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        var errors = new ArrayList<String>();
        for (var error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (var error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }

        errors.sort(String::compareToIgnoreCase);

        var errorMessage = String.join(", ", errors);
        log.debug("MethodArgumentNotValidException happened. Details: {}", errorMessage);

        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleConversionFailedException(ConversionFailedException ex) {
        log.debug("ConversionFailedException happened. Details: {}", ex.getMessage());
        var errorMessage = "Unable to convert value. Please, make sure you sent a valid request.";
        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.debug("MethodArgumentTypeMismatchException happened. Details: {}", ex.getMessage());
        var errorMessage = "Unable to convert value. Please, make sure you sent a valid request.";
        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.debug("HttpMessageNotReadableException happened. Details: {}", ex.getMessage());
        var errorMessage = "Unable to read the request. Please, make sure you sent a valid request.";
        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.debug("MissingRequestHeaderException happened. Details: {}", ex.getMessage());
        var errorMessage = "You have to provide API key in request";
        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.debug("IllegalArgumentException happened. Details: {}", ex.getMessage());
        var errorMessage = "Unable to process request. Please, make sure you sent a valid request.";
        return ErrorResponse.of(errorMessage, VALIDATION_FAILED, BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException ex) {
        log.debug("BadRequestException happened. Details: {}", ex.getMessage());
        return ErrorResponse.of(ex, BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(FORBIDDEN)
    public ErrorResponse handleForbiddenException(ForbiddenException ex) {
        log.debug("ForbiddenException happened. Details: {}", ex.getMessage());
        return ErrorResponse.of(ex, FORBIDDEN);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.debug("HttpRequestMethodNotSupportedException happened. Details: {}", ex.getMessage());
        var errorMessage = "Unable to process request. Please, make sure you sent a valid request.";
        return ErrorResponse.of(errorMessage, RESOURCE_NOT_FOUND, NOT_FOUND);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.debug("ResourceNotFoundException happened. Details: {}", ex.getMessage());
        return ErrorResponse.of(ex, NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneralException(final Exception ex) {
        log.error("Unexpected exception happened", ex);
        return ErrorResponse.of(ex.getMessage(), ErrorsTable.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);
    }

}
