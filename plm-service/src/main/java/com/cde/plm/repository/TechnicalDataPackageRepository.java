package com.cde.plm.repository;
import com.cde.plm.entity.TechnicalDataPackage;
import com.cde.plm.entity.ProductVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface TechnicalDataPackageRepository extends JpaRepository<TechnicalDataPackage, UUID> {
    List<TechnicalDataPackage> findByVersion(ProductVersion version);
}
