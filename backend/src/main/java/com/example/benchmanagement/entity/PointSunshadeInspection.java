package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_sunshade_inspection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointSunshadeInspection {

    /** 遮阳棚结论：完好 */
    public static final int RESULT_INTACT = 1;
    /** 遮阳棚结论：异常（破损） */
    public static final int RESULT_ABNORMAL = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Column(name = "inspected_at", nullable = false)
    private LocalDateTime inspectedAt;

    @Column(name = "result", nullable = false)
    private Integer result;

    /** 破损面积（平方米），完好时为 0 */
    @Column(name = "damaged_area", precision = 10, scale = 2, nullable = false)
    private BigDecimal damagedArea;

    /** 破损位置（异常时必填，必须写清破损位置） */
    @Column(name = "damaged_location", length = 200)
    private String damagedLocation;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "inspector", length = 50, nullable = false)
    private String inspector;

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
