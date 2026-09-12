package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日临时加凳预案：按街区下的路段记录计划日期、加凳数量和投放状态。
 */
@Entity
@Table(name = "additional_bench_plan", indexes = {
        @Index(name = "idx_section_plan_date", columnList = "section_id,plan_date"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalBenchPlan {

    /** 预案状态：待投放（逾期由计划日期动态推导，不单独落库） */
    public static final int STATUS_PENDING = 1;
    /** 预案状态：已投放（计划数量全部核销后自动置为已投放） */
    public static final int STATUS_DEPLOYED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "bench_count", nullable = false)
    private Integer benchCount;

    @Column(name = "verified_count", nullable = false)
    private Integer verifiedCount;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "last_verified_by", length = 50)
    private String lastVerifiedBy;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.verifiedCount == null) {
            this.verifiedCount = 0;
        }
        if (this.status == null) {
            this.status = STATUS_PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
