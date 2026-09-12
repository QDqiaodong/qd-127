package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点位地面找平验收记录。
 * 验收不合格的点位挂“待整改”，只有整改完成并填写回执后才拿掉标记。
 */
@Entity
@Table(name = "point_leveling_acceptance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLevelingAcceptance {

    /** 验收结论：合格 */
    public static final int RESULT_QUALIFIED = 1;
    /** 验收结论：不合格 */
    public static final int RESULT_UNQUALIFIED = 0;

    /** 整改状态：待整改（未写回执） */
    public static final int RECTIFY_PENDING = 0;
    /** 整改状态：已整改（已写回执） */
    public static final int RECTIFY_DONE = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    /** 验收日期 */
    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    /** 验收结论：1-合格，0-不合格 */
    @Column(name = "result", nullable = false)
    private Integer result;

    /** 验收人 */
    @Column(name = "acceptor", length = 50, nullable = false)
    private String acceptor;

    /** 验收说明（地面不平情况等） */
    @Column(name = "description", length = 1000)
    private String description;

    /** 整改状态：0-待整改，1-已整改（仅不合格记录有意义，合格记录置为已整改） */
    @Column(name = "rectify_status", nullable = false)
    private Integer rectifyStatus;

    /** 整改回执内容 */
    @Column(name = "rectify_note", length = 1000)
    private String rectifyNote;

    /** 回执提交人 */
    @Column(name = "rectify_by", length = 50)
    private String rectifyBy;

    /** 回执提交时间 */
    @Column(name = "rectified_at")
    private LocalDateTime rectifiedAt;

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
