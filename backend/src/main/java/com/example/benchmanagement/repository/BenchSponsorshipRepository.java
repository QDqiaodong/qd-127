package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.BenchSponsorship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BenchSponsorshipRepository extends JpaRepository<BenchSponsorship, Long> {

    List<BenchSponsorship> findAllByOrderByCreatedAtDescIdDesc();

    /** 查询指定点位中已经生效、尚未过期，且在结束日临期窗口内的冠名。 */
    @Query("""
            select s from BenchSponsorship s
            where s.pointId in :pointIds
              and s.startDate <= :today
              and s.endDate >= :today
              and s.endDate <= :expiringBefore
            order by s.endDate asc, s.id asc
            """)
    List<BenchSponsorship> findExpiringSoonByPointIds(
            @Param("pointIds") List<Long> pointIds,
            @Param("today") LocalDate today,
            @Param("expiringBefore") LocalDate expiringBefore);

    boolean existsByPointId(Long pointId);
}
