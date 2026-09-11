package com.example.benchmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 执行巡检任务的请求：逐张长凳提交巡检结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecuteRequest {

    /** 检查人（为空则使用任务生成时的检查人） */
    private String inspector;

    @NotEmpty(message = "巡检记录不能为空")
    @Valid
    private List<InspectionItemRequest> items;
}
