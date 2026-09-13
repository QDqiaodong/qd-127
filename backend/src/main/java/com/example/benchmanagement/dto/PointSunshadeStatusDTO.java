package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 点位遮阳棚状态：每个点位一行，附带最近一次遮阳棚巡查结论；未巡查的点位 inspected=false。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointSunshadeStatusDTO {

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    /** 是否巡查过（存在遮阳棚巡查记录） */
    private Boolean inspected;

    /** 最近一次遮阳棚结论：1-完好，0-异常，null-未巡查 */
    private Integer latestResult;

    /** 最近一次破损面积（平方米） */
    private BigDecimal damagedArea;

    /** 最近一次破损位置 */
    private String damagedLocation;

    private String inspector;

    private LocalDateTime latestInspectedAt;
}
