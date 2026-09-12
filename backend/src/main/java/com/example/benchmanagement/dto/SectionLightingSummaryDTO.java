package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 路段夜间照明异常汇总：按路段统计最近一次巡查结论为异常的点位数，并拆分缺灯/损坏。
 * 只包含有异常点位的路段；没有异常的路段不出汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionLightingSummaryDTO {

    private Long sectionId;

    private String sectionName;

    private Long districtId;

    private String districtName;

    /** 异常点数：该路段下最近一次照明结论为异常的点位数量 */
    private Integer abnormalPointCount;

    /** 其中异常类型为缺灯的点位数 */
    private Integer missingLampCount;

    /** 其中异常类型为损坏的点位数 */
    private Integer damagedCount;

    /** 该路段异常点位中最近一次巡查时间 */
    private LocalDateTime latestInspectedAt;
}
