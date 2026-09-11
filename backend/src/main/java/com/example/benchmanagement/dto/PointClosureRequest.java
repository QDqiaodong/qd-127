package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointClosureRequest {

    @NotNull(message = "封闭开始时间不能为空")
    private LocalDateTime startAt;

    @NotNull(message = "封闭结束时间不能为空")
    private LocalDateTime endAt;

    @NotBlank(message = "封闭原因不能为空")
    private String reason;
}
