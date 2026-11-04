package com.smartprocure.vendor.repository;

import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.entity.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByEmail(String email);
    boolean existsByTaxId(String taxId);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByTaxIdAndIdNot(String taxId, Long id);

    Optional<Vendor> findByEmail(String email);

    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);

    Page<Vendor> findByActiveTrue(Pageable pageable);

    @Query("""
            SELECT v FROM Vendor v
            WHERE (:status IS NULL OR v.status = :status)
              AND (:active IS NULL OR v.active = :active)
              AND (:search IS NULL
                   OR LOWER(v.name)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(v.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Vendor> search(
            @Param("status") VendorStatus status,
            @Param("active") Boolean      active,
            @Param("search") String       search,
            Pageable pageable);
}
