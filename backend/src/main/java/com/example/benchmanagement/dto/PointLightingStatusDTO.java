package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点位照明状态：每个点位一行，附带最近一次照明巡查结论；未巡查的点位 inspected=false。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLightingStatusDTO {

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    /** 是否已巡查过（存在照明巡查记录） */
    private Boolean inspected;

    /** 最近一次照明结论：1-完好，0-异常，null-未巡查 */
    private Integer latestResult;

    private Integer lampCount;

    /** 最近一次异常类型：缺灯/损坏 */
    private String problemType;

    private String inspector;

    private LocalDateTime latestInspectedAt;
}
