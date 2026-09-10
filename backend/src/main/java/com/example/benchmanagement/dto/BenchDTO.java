package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchDTO {

    private Long id;

    @NotBlank(message = "长凳编号不能为空")
    private String code;

    private String material;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    @NotNull(message = "所属点位不能为空")
    private Long nodeId;

    private String specsJson;

    private Integer status;

    private String nodeName;

    private String districtName;

    private String sectionName;

    /** 归属点位变更原因（仅编辑长凳并调整点位时使用） */
    private String changeReason;

    /** 最近一次巡检时间 */
    private java.time.LocalDateTime latestInspectionAt;

    /** 最近一次巡检结果：1-正常，0-异常；null-从未巡检 */
    private Integer latestInspectionResult;

    /** 最近一次巡检严重程度：1-低，2-中，3-高 */
    private Integer latestInspectionSeverity;

    /** 最近一次巡检问题类型 */
    private String latestProblemType;

    /** 当前未完成（待处理/维修中）工单数 */
    private Long openOrderCount;
}
