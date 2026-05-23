package com.cde.plm.repository;

import com.cde.plm.entity.VersionPhaseInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VersionPhaseInstanceRepository extends JpaRepository<VersionPhaseInstance, UUID> {

    /** All phase instances for a given version, ordered by phase sequence. */
    List<VersionPhaseInstance> findByVersion_VersionIdOrderByPhase_SequenceOrderAsc(UUID versionId);
}
