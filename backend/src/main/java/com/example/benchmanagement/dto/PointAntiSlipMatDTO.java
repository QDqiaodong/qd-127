package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点位防滑垫领用台账：每个点位一行，汇总领出、完好归还、破损、未还数量。
 * 未还 = 领出合计 - 归还合计（归还合计含破损）；未还 &gt; 0 即为未还清，需要在台账和街区树标出来。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointAntiSlipMatDTO {

    private Long pointId;

    private String pointName;

    private Long sectionId;

    private String sectionName;

    private Long districtId;

    private String districtName;

    /** 领出合计 */
    private Integer issuedCount;

    /** 归还合计（完好归还 + 破损） */
    private Integer returnedCount;

    /** 其中完好归还合计 */
    private Integer intactCount;

    /** 破损合计（归还时登记的破损数量） */
    private Integer damagedCount;

    /** 未还数量 = 领出 - 归还 */
    private Integer outstandingCount;

    /** 是否未还清（未还数量 &gt; 0） */
    private Boolean outstanding;

    /** 是否有过领用记录（从未领用的点位仍返回一行，便于按点位登记与核对） */
    private Boolean hasRecord;

    private String lastOperator;

    private LocalDateTime lastOperatedAt;
}
