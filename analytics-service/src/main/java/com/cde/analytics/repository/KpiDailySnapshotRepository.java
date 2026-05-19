package com.cde.analytics.repository;

import com.cde.analytics.entity.KpiDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KpiDailySnapshotRepository extends JpaRepository<KpiDailySnapshot, UUID> {
    List<KpiDailySnapshot> findTop30ByOrderBySnapshotDateDesc();
    Optional<KpiDailySnapshot> findBySnapshotDate(LocalDate date);
}
