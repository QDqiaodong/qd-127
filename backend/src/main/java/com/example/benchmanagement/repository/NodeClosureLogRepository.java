package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.NodeClosureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeClosureLogRepository extends JpaRepository<NodeClosureLog, Long> {

    List<NodeClosureLog> findByNodeIdOrderByOperatedAtDescIdDesc(Long nodeId);

    List<NodeClosureLog> findAllByOrderByOperatedAtDescIdDesc();
}
