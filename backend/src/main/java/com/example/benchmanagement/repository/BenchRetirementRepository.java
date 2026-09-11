package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.BenchRetirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenchRetirementRepository extends JpaRepository<BenchRetirement, Long> {

    List<BenchRetirement> findByBenchIdOrderByRetiredAtDescIdDesc(Long benchId);

    List<BenchRetirement> findAllByOrderByRetiredAtDescIdDesc();

    boolean existsByReplacementBenchId(Long replacementBenchId);
}
