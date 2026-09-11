package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 巡检计划详情：计划信息（含已执行/待执行/逾期统计）及其任务列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionPlanDetailDTO {

    private InspectionPlanDTO plan;

    private List<InspectionTaskDTO> tasks;
}
