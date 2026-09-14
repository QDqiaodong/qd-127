package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 防滑垫领出登记请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiSlipMatIssueRequest {

    @NotNull(message = "点位不能为空")
    private Long pointId;

    /** 本次领出数量（必须大于0） */
    @NotNull(message = "领出数量不能为空")
    @Positive(message = "领出数量必须大于0")
    private Integer quantity;

    @NotNull(message = "领出时间不能为空")
    private LocalDateTime operatedAt;

    private String operator;

    private String remark;
}
