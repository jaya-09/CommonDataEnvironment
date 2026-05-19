package com.cde.qlm.repository;
import com.cde.qlm.entity.Capa;
import com.cde.qlm.entity.NonConformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface CapaRepository extends JpaRepository<Capa, UUID> {
    List<Capa> findByNcr(NonConformance ncr);
    List<Capa> findByStatus(Capa.CapaStatus status);
    List<Capa> findByOwnerUserId(UUID ownerId);
}
