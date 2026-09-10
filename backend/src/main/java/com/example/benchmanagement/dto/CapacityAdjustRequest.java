package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityAdjustRequest {

    @NotNull(message = "容量不能为空")
    @PositiveOrZero(message = "容量必须为大于等于0的整数")
    private Integer capacity;

    @NotBlank(message = "调整原因不能为空")
    private String adjustReason;
}
