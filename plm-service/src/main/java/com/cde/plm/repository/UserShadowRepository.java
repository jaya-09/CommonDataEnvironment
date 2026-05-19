package com.cde.plm.repository;
import com.cde.plm.entity.UserShadow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public interface UserShadowRepository extends JpaRepository<UserShadow, UUID> {}
