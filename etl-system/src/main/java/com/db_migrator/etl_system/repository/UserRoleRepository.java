package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.user.UserRole;
import com.db_migrator.etl_system.model.enums.UserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    Optional<UserRole> findByRole(UserRoleEnum role);
}
