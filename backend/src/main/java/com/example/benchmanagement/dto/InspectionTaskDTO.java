package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionTaskDTO {

    private Long id;

    private Long planId;

    private String planName;

    private Long scopeNodeId;

    private String scopeNodeName;

    /** 巡检范围完整路径 */
    private String scopePath;

    private LocalDate planDate;

    private String inspector;

    /** 任务状态：1-待执行，2-已执行 */
    private Integer status;

    /** 是否逾期（待执行且计划日期早于今天） */
    private Boolean overdue;

    private LocalDateTime executedAt;

    private LocalDateTime createdAt;
}
