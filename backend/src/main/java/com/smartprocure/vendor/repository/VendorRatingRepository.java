package com.smartprocure.vendor.repository;

import com.smartprocure.vendor.entity.VendorRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRatingRepository extends JpaRepository<VendorRating, Long> {

    Page<VendorRating> findByVendorId(Long vendorId, Pageable pageable);

    Optional<VendorRating> findByIdAndVendorId(Long id, Long vendorId);

    @Query("SELECT AVG(r.score) FROM VendorRating r WHERE r.vendor.id = :vendorId")
    Optional<Double> findAverageScoreByVendorId(@Param("vendorId") Long vendorId);

    boolean existsByVendorIdAndRatedById(Long vendorId, Long ratedById);
}
