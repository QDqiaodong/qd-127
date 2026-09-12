package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 路段容量告警详情：剩余容量加总、生效阈值及已满/将满点位明细。
 * 与街区树路段告警标记同源（同一份点位占用数据、同一组阈值常量）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionCapacityAlarmDTO {

    private Long sectionId;

    private String sectionName;

    /** 告警阈值：剩余容量加总低于该值触发告警 */
    private Integer threshold;

    /** 路段下各点位剩余容量加总（没有点位时为 null） */
    private Long remainingSum;

    /** 是否告警（没有点位的路段恒为 false） */
    private Boolean alarm;

    /** 已满点位数 */
    private Integer fullCount;

    /** 将满点位数 */
    private Integer nearlyFullCount;

    /** 已满/将满点位明细（按剩余升序） */
    private List<TreeNodeDTO> points;
}
