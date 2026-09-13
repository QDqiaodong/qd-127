package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商户长凳冠名台账：按点位（level=3）登记商户名称、冠名文案和冠名起止日期。
 * 生效状态（待生效/生效中/已过期）全部由起止日期动态推导，不单独落库。
 */
@Entity
@Table(name = "bench_sponsorship", indexes = {
        @Index(name = "idx_point_date", columnList = "point_id,start_date,end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchSponsorship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Column(name = "merchant_name", nullable = false, length = 100)
    private String merchantName;

    @Column(name = "sponsorship_text", nullable = false, length = 200)
    private String sponsorshipText;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
