package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.BenchSponsorship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenchSponsorshipRepository extends JpaRepository<BenchSponsorship, Long> {

    List<BenchSponsorship> findAllByOrderByCreatedAtDescIdDesc();

    boolean existsByPointId(Long pointId);
}
