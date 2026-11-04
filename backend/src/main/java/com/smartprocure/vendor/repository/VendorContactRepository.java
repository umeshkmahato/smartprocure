package com.smartprocure.vendor.repository;

import com.smartprocure.vendor.entity.VendorContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorContactRepository extends JpaRepository<VendorContact, Long> {

    List<VendorContact> findByVendorId(Long vendorId);

    Optional<VendorContact> findByIdAndVendorId(Long id, Long vendorId);

    boolean existsByVendorIdAndPrimaryTrue(Long vendorId);

    @Modifying
    @Query("UPDATE VendorContact c SET c.primary = false WHERE c.vendor.id = :vendorId")
    void clearPrimaryForVendor(@Param("vendorId") Long vendorId);
}
