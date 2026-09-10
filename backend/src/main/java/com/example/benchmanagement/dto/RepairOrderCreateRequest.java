package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 手动为异常长凳创建维修工单的请求（无关联巡检记录时使用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairOrderCreateRequest {

    @NotNull(message = "维修长凳不能为空")
    private Long benchId;

    private String problemType;

    /** 严重程度：1-低，2-中，3-高 */
    private Integer severity;

    private String description;

    private String suggestion;
}
