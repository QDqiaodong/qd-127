package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.NodeCapacityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeCapacityLogRepository extends JpaRepository<NodeCapacityLog, Long> {

    List<NodeCapacityLog> findByNodeIdOrderByAdjustedAtDesc(Long nodeId);
}
