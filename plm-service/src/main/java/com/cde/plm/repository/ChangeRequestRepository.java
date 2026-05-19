package com.cde.plm.repository;
import com.cde.plm.entity.ChangeRequest;
import com.cde.plm.entity.ProductVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {
    List<ChangeRequest> findByVersion(ProductVersion version);
    List<ChangeRequest> findByStatus(ChangeRequest.CrStatus status);
    Optional<ChangeRequest> findByCrNumber(String crNumber);
}
