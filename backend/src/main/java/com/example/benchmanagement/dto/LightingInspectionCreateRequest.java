package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LightingInspectionCreateRequest {

    @NotNull(message = "点位不能为空")
    private Long pointId;

    @NotNull(message = "巡查时间不能为空")
    private LocalDateTime inspectedAt;

    /** 照明结论：1-完好，0-异常 */
    @NotNull(message = "照明结论不能为空")
    private Integer result;

    /** 灯具数量 */
    @NotNull(message = "灯具数量不能为空")
    private Integer lampCount;

    /** 异常类型：缺灯/损坏（异常时必填） */
    private String problemType;

    private String description;

    @NotBlank(message = "巡查人不能为空")
    private String inspector;
}
