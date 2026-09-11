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
public class InspectionPlanDTO {

    private Long id;

    private String name;

    private Long scopeNodeId;

    private String scopeNodeName;

    /** 巡检范围完整路径，如：商业步行街A区 / A区主干道 / 广场前 */
    private String scopePath;

    /** 巡检周期：1-每天，2-每周，3-每月 */
    private Integer cycleType;

    private LocalDate planDate;

    private String inspector;

    private Boolean enabled;

    private String remark;

    /** 已执行任务数 */
    private Long executedCount;

    /** 待执行任务数（不含逾期） */
    private Long pendingCount;

    /** 逾期任务数 */
    private Long overdueCount;

    private LocalDateTime createdAt;
}
