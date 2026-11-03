package com.smartprocure.user.repository;

import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Login lookup — accepts both username and email. */
    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /** Admin list with optional server-side filters. */
    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
            AND   (:department IS NULL
                   OR LOWER(u.department) LIKE LOWER(CONCAT('%', :department, '%')))
            """)
    Page<User> findAllFiltered(
            @Param("status")     UserStatus status,
            @Param("department") String department,
            Pageable pageable
    );
}
