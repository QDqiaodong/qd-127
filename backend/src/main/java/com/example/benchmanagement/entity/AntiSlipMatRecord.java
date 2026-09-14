package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 防滑垫领用/归还流水：雨天把防滑垫发到各点位，按点位登记领出、归还、破损数量。
 * 同一点位可多次领出、分批归还，点位台账汇总数量 = 领出合计 - 完好归还合计 - 破损合计。
 */
@Entity
@Table(name = "anti_slip_mat_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiSlipMatRecord {

    /** 动作类型：领出 */
    public static final int ACTION_ISSUE = 1;
    /** 动作类型：归还（归还数量中再区分完好归还与破损） */
    public static final int ACTION_RETURN = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    /** 动作类型：1-领出，2-归还 */
    @Column(name = "action_type", nullable = false)
    private Integer actionType;

    /** 本次数量：领出时为领出数量；归还时为本次归还总数（完好归还 + 破损） */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 本次破损数量（归还时填写，不能大于本次归还数量；领出时为 0） */
    @Column(name = "damaged_quantity", nullable = false)
    private Integer damagedQuantity;

    /** 操作时间 */
    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    /** 经办人 */
    @Column(name = "operator", length = 50, nullable = false)
    private String operator;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;

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
