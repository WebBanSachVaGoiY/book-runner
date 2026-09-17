package com.example.bookrunner.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //Exception xử lý field unique bị trùng
    @ExceptionHandler(DuplicateUniqueFieldException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateUniqueFieldException die) {
        Map<String, Object> errorDetails = Map.of(
                "code", "DUPLICATE_FIELD",
                "message", die.getMessage()
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "error", errorDetails
        );
        return new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
    }

    //Exception xử lý dữ liệu không tồn tại
    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<Map<String,Object>> handleBookNotFoundException(ItemNotFoundException bnfe){
        Map<String, Object> errorDetails = Map.of(
                "code", "ITEM_NOT_FOUND",
                "message", bnfe.getMessage()
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "error", errorDetails
        );
        return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);
    }

    //Exception xử lý nhập thiếu field
    @ExceptionHandler(FieldRequiredException.class)
    public ResponseEntity<Map<String,Object>> handleFieldRequiredException(FieldRequiredException fre){
        Map<String, Object> errorDetails = Map.of(
                "code", "FIELD_REQUIRED",
                "message", fre.getMessage()
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "error", errorDetails
        );
        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleException(Exception e){
        Map<String, Object> errorDetails = Map.of(
                "code", "UNIDENTIFIED_ERROR",
                "message", e.getMessage()
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "error", errorDetails
        );
        return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
