package com.cde.llm.repository;

import com.cde.llm.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByEmployeeId(String employeeId);

    List<UserProfile> findByRoleAndActiveTrue(UserProfile.Role role);

    List<UserProfile> findByDepartmentAndActiveTrue(String department);

    @Query("SELECT u FROM UserProfile u WHERE u.active = true AND u.role = :role AND u.department = :dept")
    List<UserProfile> findActiveByRoleAndDepartment(
            @Param("role") UserProfile.Role role,
            @Param("dept") String department);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);
}
