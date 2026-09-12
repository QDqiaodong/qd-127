package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点位找平状态：每个点位一行，口径与街区树点位找平标记同源。
 * pending=true 表示该点位当前挂着“待整改”（存在未写回执的不合格验收）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLevelingStatusDTO {

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    /** 是否登记过找平验收 */
    private Boolean accepted;

    /** 最近一次验收结论：1-合格，0-不合格，null-未验收 */
    private Integer latestResult;

    /** 是否待整改（不合格且未写回执） */
    private Boolean pending;

    /** 待整改对应的不合格验收记录ID（写回执用，无待整改时为 null） */
    private Long openAcceptanceId;

    private String acceptor;

    private LocalDateTime latestAcceptedAt;

    private String description;
}
