package com.example.benchmanagement.util;

import com.example.benchmanagement.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("业务状态冲突: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("数据完整性冲突: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "存在关联的巡检或维修历史记录，无法删除该长凳"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("数据校验失败: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                .code(400)
                .message("数据校验失败")
                .data(errors)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("系统异常: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "系统内部错误"));
    }
}
