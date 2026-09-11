package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 长凳报废退役台账。
 */
@Entity
@Table(name = "bench_retirement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchRetirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 退役长凳ID */
    @Column(name = "bench_id", nullable = false)
    private Long benchId;

    /** 退役长凳编号（台账快照） */
    @Column(name = "bench_code", nullable = false)
    private String benchCode;

    /** 退役时所在点位ID（原点位） */
    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    /** 报废退役原因 */
    @Column(name = "retired_reason", nullable = false)
    private String retiredReason;

    /** 退役时间 */
    @Column(name = "retired_at", nullable = false)
    private LocalDateTime retiredAt;

    /** 退役经办人 */
    @Column(name = "retired_by", nullable = false)
    private String retiredBy;

    /** 原点位登记的替换新凳ID */
    @Column(name = "replacement_bench_id")
    private Long replacementBenchId;

    /** 替换新凳编号 */
    @Column(name = "replacement_bench_code")
    private String replacementBenchCode;

    /** 替换登记时间 */
    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
