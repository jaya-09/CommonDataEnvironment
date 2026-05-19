package com.cde.qlm.repository;
import com.cde.qlm.entity.RiskRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface RiskRegisterRepository extends JpaRepository<RiskRegister, UUID> {
    List<RiskRegister> findByProductVersionId(UUID versionId);
    List<RiskRegister> findByStatus(RiskRegister.RiskStatus status);
    List<RiskRegister> findAllByOrderByRiskScoreDesc();
}
