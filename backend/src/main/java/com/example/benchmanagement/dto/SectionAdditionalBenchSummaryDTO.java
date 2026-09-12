package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionAdditionalBenchSummaryDTO {

    /** 路段下待投放预案的未投放数量（含逾期未投放），与街区树标记同源 */
    private Long pendingCount;

    /** 路段下存在逾期且仍未完全投放的预案数 */
    private Long overduePlanCount;
}
