package com.cde.qlm.repository;
import com.cde.qlm.entity.UserShadow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public interface UserShadowRepository extends JpaRepository<UserShadow, UUID> {}
