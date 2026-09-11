package com.example.benchmanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 巡检计划创建/编辑请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionPlanRequest {

    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称不能超过100个字符")
    private String name;

    /** 巡检范围节点（街区 level=1 / 路段 level=2 / 点位 level=3） */
    @NotNull(message = "巡检范围不能为空")
    private Long scopeNodeId;

    /** 巡检周期：1-每天，2-每周，3-每月 */
    @NotNull(message = "巡检周期不能为空")
    private Integer cycleType;

    /** 首次计划日期 */
    @NotNull(message = "计划日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    @NotBlank(message = "检查人不能为空")
    @Size(max = 50, message = "检查人不能超过50个字符")
    private String inspector;

    /** 启用状态，默认启用 */
    private Boolean enabled;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
