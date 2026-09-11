package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.Bench;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BenchRepository extends JpaRepository<Bench, Long> {

    Optional<Bench> findByCode(String code);

    List<Bench> findByNodeId(Long nodeId);

    @Query("SELECT b FROM Bench b WHERE b.nodeId IN :nodeIds")
    List<Bench> findByNodeIds(@org.springframework.data.repository.query.Param("nodeIds") Collection<Long> nodeIds);

    @Query("SELECT COUNT(b) FROM Bench b WHERE b.nodeId = :nodeId")
    long countByNodeId(Long nodeId);

    /** 仅统计状态为在用(1)的长凳，用于点位占用数/剩余容量计算。 */
    @Query("SELECT COUNT(b) FROM Bench b WHERE b.nodeId = :nodeId AND b.status = 1")
    long countActiveByNodeId(Long nodeId);

    @Query("SELECT b.nodeId AS nodeId, COUNT(b) AS cnt FROM Bench b WHERE b.nodeId IN :nodeIds GROUP BY b.nodeId")
    List<Object[]> countByNodeIds(@org.springframework.data.repository.query.Param("nodeIds") List<Long> nodeIds);

    /** 按点位分组统计在用(1)长凳数量，用于批量填充占用数/剩余容量。 */
    @Query("SELECT b.nodeId AS nodeId, COUNT(b) AS cnt FROM Bench b WHERE b.nodeId IN :nodeIds AND b.status = 1 GROUP BY b.nodeId")
    List<Object[]> countActiveByNodeIds(@org.springframework.data.repository.query.Param("nodeIds") List<Long> nodeIds);

    @Query("SELECT b FROM Bench b WHERE b.material LIKE %:keyword% OR b.code LIKE %:keyword%")
    List<Bench> searchByKeyword(String keyword);

    boolean existsByCode(String code);
}
