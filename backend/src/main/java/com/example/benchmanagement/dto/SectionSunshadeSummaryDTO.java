package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 路段遮阳棚异常汇总：按路段统计最近一次巡查结论为异常（破损）的点位数及破损面积加总。
 * 只包含有异常点位的路段；没有异常的路段不出汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionSunshadeSummaryDTO {

    private Long sectionId;

    private String sectionName;

    private Long districtId;

    private String districtName;

    /** 异常点数：该路段下最近一次遮阳棚结论为异常的点位数量 */
    private Integer abnormalPointCount;

    /** 该路段异常点位最近一次破损面积加总（平方米） */
    private BigDecimal damagedAreaSum;

    /** 该路段异常点位中最近一次巡查时间 */
    private LocalDateTime latestInspectedAt;
}
