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
    //Exception mã định danh sách bị trùng
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateIsbnException die) {
        Map<String, Object> errorDetails = Map.of(
                "code", "DUPLICATE_ISBN",
                "message", die.getMessage()
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "error", errorDetails
        );
        return new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
    }

    //Exception sách không tồn tại
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<Map<String,Object>> handleBookNotFoundException(BookNotFoundException bnfe){
        Map<String, Object> errorDetails = Map.of(
                "code", "BOOK_NOT_FOUND",
                "message", bnfe.getMessage()
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "error", errorDetails
        );
        return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);
    }
}
