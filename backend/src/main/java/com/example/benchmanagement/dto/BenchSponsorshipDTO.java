package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchSponsorshipDTO {

    private Long id;

    private Long pointId;
    private String pointName;
    private Long sectionId;
    private String sectionName;
    private Long districtId;
    private String districtName;

    private String merchantName;
    private String sponsorshipText;

    private LocalDate startDate;
    private LocalDate endDate;

    /** 展示状态：1-待生效，2-生效中，3-已过期（按当天日期与起止日期动态推导） */
    private Integer effectiveStatus;

    private Boolean expired;

    private LocalDateTime createdAt;
}
