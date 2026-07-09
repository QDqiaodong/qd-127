package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.Bench;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BenchRepository extends JpaRepository<Bench, Long> {

    Optional<Bench> findByCode(String code);

    List<Bench> findByNodeId(Long nodeId);

    @Query("SELECT b FROM Bench b WHERE b.nodeId IN :nodeIds")
    List<Bench> findByNodeIds(List<Long> nodeIds);

    @Query("SELECT COUNT(b) FROM Bench b WHERE b.nodeId = :nodeId")
    long countByNodeId(Long nodeId);

    @Query("SELECT b FROM Bench b WHERE b.material LIKE %:keyword% OR b.code LIKE %:keyword%")
    List<Bench> searchByKeyword(String keyword);

    boolean existsByCode(String code);
}
