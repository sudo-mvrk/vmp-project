package com.volunnear.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {
    @EntityGraph(attributePaths = {"roles"})
    Optional<AppUser> findAppUserByUsername(String username);

    @EntityGraph(attributePaths = {"roles"})
    Optional<AppUser> findAppUserByEmail(String email);
    @EntityGraph(attributePaths = {"roles"})
    Optional<AppUser> findAppUserById(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
