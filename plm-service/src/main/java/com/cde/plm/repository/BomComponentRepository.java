package com.cde.plm.repository;
import com.cde.plm.entity.BomComponent;
import com.cde.plm.entity.ProductVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface BomComponentRepository extends JpaRepository<BomComponent, UUID> {
    List<BomComponent> findByVersionAndParentIsNull(ProductVersion version);
    List<BomComponent> findByParent(BomComponent parent);
    List<BomComponent> findByVersion(ProductVersion version);
}
