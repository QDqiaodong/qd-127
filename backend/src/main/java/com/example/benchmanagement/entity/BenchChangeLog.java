package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bench_change_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bench_id", nullable = false)
    private Long benchId;

    @Column(name = "old_node_id", nullable = false)
    private Long oldNodeId;

    @Column(name = "new_node_id", nullable = false)
    private Long newNodeId;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private String changedBy;

    @PrePersist
    public void prePersist() {
        this.changedAt = LocalDateTime.now();
        if (this.changedBy == null) {
            this.changedBy = "system";
        }
    }
}
