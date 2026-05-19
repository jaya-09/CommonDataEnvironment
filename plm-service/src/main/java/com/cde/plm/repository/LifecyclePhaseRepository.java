package com.cde.plm.repository;
import com.cde.plm.entity.LifecyclePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface LifecyclePhaseRepository extends JpaRepository<LifecyclePhase, UUID> {
    Optional<LifecyclePhase> findByPhaseName(String phaseName);
    List<LifecyclePhase> findAllByOrderBySequenceOrderAsc();
    Optional<LifecyclePhase> findBySequenceOrder(int order);
}
