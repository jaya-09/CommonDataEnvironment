package com.cde.qlm.repository;
import com.cde.qlm.entity.DocumentControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface DocumentControlRepository extends JpaRepository<DocumentControl, UUID> {
    List<DocumentControl> findByApprovalStatus(DocumentControl.DocStatus status);
    List<DocumentControl> findByDocNumber(String docNumber);
}
