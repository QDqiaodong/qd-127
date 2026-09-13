package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunshadeInspectionCreateRequest {

    @NotNull(message = "点位不能为空")
    private Long pointId;

    @NotNull(message = "巡查时间不能为空")
    private LocalDateTime inspectedAt;

    /** 遮阳棚结论：1-完好，0-异常（破损） */
    @NotNull(message = "巡查结论不能为空")
    private Integer result;

    /** 破损面积（平方米），完好时填 0 */
    @NotNull(message = "破损面积不能为空")
    private BigDecimal damagedArea;

    /** 破损位置（异常时必填，必须写清破损位置） */
    private String damagedLocation;

    /** 备注说明 */
    private String description;

    @NotBlank(message = "巡查人不能为空")
    private String inspector;
}
