package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.BenchChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenchChangeLogRepository extends JpaRepository<BenchChangeLog, Long> {

    List<BenchChangeLog> findByBenchIdOrderByChangedAtDesc(Long benchId);

    List<BenchChangeLog> findByOldNodeIdOrNewNodeId(Long oldNodeId, Long newNodeId);
}
