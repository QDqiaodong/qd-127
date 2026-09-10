package com.example.benchmanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 巡检发起请求中的单张长凳巡检项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionItemRequest {

    @NotNull(message = "巡检长凳不能为空")
    private Long benchId;

    @NotNull(message = "检查时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inspectedAt;

    /** 巡检结果：1-正常，0-异常 */
    @NotNull(message = "巡检结果不能为空")
    private Integer result;

    /** 问题类型（异常时必填） */
    private String problemType;

    /** 严重程度：1-低，2-中，3-高（异常时必填） */
    private Integer severity;

    /** 问题描述（异常时必填） */
    private String description;

    /** 处理建议 */
    private String suggestion;

    /** 异常时是否同步生成维修工单 */
    private Boolean createRepairOrder;
}
