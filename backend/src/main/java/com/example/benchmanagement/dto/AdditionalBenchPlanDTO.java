package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalBenchPlanDTO {

    private Long id;
    private Long districtId;
    private String districtName;
    private Long sectionId;
    private String sectionName;
    private LocalDate planDate;
    private Integer benchCount;
    private Integer verifiedCount;
    private Integer pendingCount;

    /** 落库状态：1-待投放，2-已投放 */
    private Integer status;

    /** 展示状态：1-待投放，2-已投放，3-逾期未投放/未完全投放 */
    private Integer effectiveStatus;

    private Boolean overdue;
    private String remark;
    private String lastVerifiedBy;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private List<AdditionalBenchPlanLogDTO> logs;
}
