package com.mail.springbootimaplistener.repository;

import org.springframework.stereotype.Repository;

import com.mail.springbootimaplistener.entity.Vendors;
import java.sql.Timestamp;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VendorRepository extends JpaRepository<Vendors, String> {
    @Query(value = "SELECT vendorId from Vendors WHERE vendorId=:vendorId", nativeQuery = true)
    String findVendorId(@Param("vendorId")String vendorId);
}
