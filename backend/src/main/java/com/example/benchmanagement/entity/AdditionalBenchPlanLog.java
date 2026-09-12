package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 加凳投放核销记录：每次核销数量、操作人和备注均留痕。
 */
@Entity
@Table(name = "additional_bench_plan_log", indexes = {
        @Index(name = "idx_plan_id", columnList = "plan_id"),
        @Index(name = "idx_verified_at", columnList = "verified_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalBenchPlanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "verified_count", nullable = false)
    private Integer verifiedCount;

    @Column(name = "before_count", nullable = false)
    private Integer beforeCount;

    @Column(name = "after_count", nullable = false)
    private Integer afterCount;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;
}
