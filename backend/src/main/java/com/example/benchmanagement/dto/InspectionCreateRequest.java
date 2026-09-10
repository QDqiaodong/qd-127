package com.example.benchmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 按街区/路段/点位发起巡检的请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionCreateRequest {

    /** 巡检范围节点（街区 level=1 / 路段 level=2 / 点位 level=3） */
    @NotNull(message = "巡检范围不能为空")
    private Long scopeNodeId;

    /** 检查人 */
    private String inspector;

    @NotEmpty(message = "巡检记录不能为空")
    @Valid
    private List<InspectionItemRequest> items;
}
