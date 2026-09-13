package com.example.benchmanagement.dto;

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
public class SunshadeInspectionDTO {

    private Long id;

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    private LocalDateTime inspectedAt;

    /** 遮阳棚结论：1-完好，0-异常 */
    private Integer result;

    /** 破损面积（平方米） */
    private BigDecimal damagedArea;

    /** 破损位置 */
    private String damagedLocation;

    /** 备注说明 */
    private String description;

    private String inspector;

    private LocalDateTime createdAt;
}
