package com.kartik.finance_tracker.common.exception;

import com.kartik.finance_tracker.common.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleResourceNotFoundException(
            ResourceNotFoundException exception
    ) {
        // Resources outside the authenticated user's scope are treated as not found.
        return new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        // Business-rule violations are exposed to API clients as bad requests.
        return new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        // Return the validation message from the first invalid request field.
        String message = exception.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage();

        return new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        // Malformed JSON or invalid JSON values cannot be converted into the request DTO.
        // Return a clean API error instead of exposing Spring's internal error response.
        return new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request body"
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpectedException(Exception exception) {
        // Keep unexpected internal failures hidden from API clients.
        return new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred"
        );
    }
    
}
