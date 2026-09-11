package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bench")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bench {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "material")
    private String material;

    @Column(name = "length")
    private BigDecimal length;

    @Column(name = "width")
    private BigDecimal width;

    @Column(name = "height")
    private BigDecimal height;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "specs_json", columnDefinition = "TEXT")
    private String specsJson;

    @Column(name = "status")
    private Integer status;

    /** 退役状态：1-已退役(报废)，0-在用 */
    @Column(name = "retired")
    private Integer retired;

    /** 退役记录ID */
    @Column(name = "retirement_id")
    private Long retirementId;

    /** 报废退役原因 */
    @Column(name = "retired_reason")
    private String retiredReason;

    /** 退役时间 */
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    /** 退役经办人 */
    @Column(name = "retired_by")
    private String retiredBy;

    /** 替换新凳ID（退役后在原点位登记的替换长凳） */
    @Column(name = "replaced_by_bench_id")
    private Long replacedByBenchId;

    /** 被替换的退役长凳ID（仅替换新凳有值） */
    @Column(name = "replaces_bench_id")
    private Long replacesBenchId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DISABLED = 0;
    public static final int RETIRED_YES = 1;
    public static final int RETIRED_NO = 0;

    public boolean isRetiredBench() {
        return Integer.valueOf(RETIRED_YES).equals(this.retired);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = 1;
        }
        if (this.retired == null) {
            this.retired = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
