package com.cde.plm.repository;
import com.cde.plm.entity.Product;
import com.cde.plm.entity.ProductVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface ProductVersionRepository extends JpaRepository<ProductVersion, UUID> {
    List<ProductVersion> findByProduct(Product product);
    List<ProductVersion> findByStatus(ProductVersion.VersionStatus status);
    boolean existsByProductAndVersionNumber(Product product, String versionNumber);
    @Query("SELECT v FROM ProductVersion v WHERE v.product.productId = :productId ORDER BY v.createdAt DESC")
    List<ProductVersion> findByProductId(@Param("productId") UUID productId);
}
