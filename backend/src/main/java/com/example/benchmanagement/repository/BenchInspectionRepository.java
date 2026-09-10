package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.BenchInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenchInspectionRepository extends JpaRepository<BenchInspection, Long> {

    List<BenchInspection> findByBenchIdOrderByInspectedAtDescIdDesc(Long benchId);

    List<BenchInspection> findByBenchIdInOrderByInspectedAtDescIdDesc(List<Long> benchIds);

    List<BenchInspection> findAllByOrderByInspectedAtDescIdDesc();

    long countByBenchIdAndResult(Long benchId, Integer result);
}
