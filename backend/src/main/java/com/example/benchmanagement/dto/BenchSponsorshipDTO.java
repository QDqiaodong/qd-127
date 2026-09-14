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

    /** 展示状态：1-待生效，2-生效中，3-已过期，4-即将到期（按当天日期与起止日期动态推导） */
    private Integer effectiveStatus;

    /** 是否处于临期提醒窗口（生效中且距离结束日期不超过阈值） */
    private Boolean expiringSoon;

    /** 距离结束日期的剩余天数（结束日期当天为0，结束日期过后仍按自然日差返回） */
    private Long daysRemaining;

    /** 临期提醒阈值：结束日前3天开始提示 */
    private Integer expiringSoonDays;

    private Boolean expired;

    private LocalDateTime createdAt;
}
