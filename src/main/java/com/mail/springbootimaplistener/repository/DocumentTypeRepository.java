package com.mail.springbootimaplistener.repository;

import com.mail.springbootimaplistener.entity.DocumentTypes;
import java.util.List;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentTypes, Long> {
    @Query(value = "SELECT fileNameFormat from DocumentTypes WHERE keywords LIKE %:keywords%", nativeQuery = true)
    String findFileNameFormat(@Param("keywords")String keywords);
    
    @Query(value = "SELECT fileNameFormat from DocumentTypes", nativeQuery = true)
    List<String> getFileNameFormat();
    
    @Query(value = "SELECT keywords from DocumentTypes GROUP BY documentTypeId", nativeQuery = true)
    List<String> findKeywords();
    
    @Query(value = "SELECT documentTypeId from DocumentTypes where keywords = :keywords", nativeQuery = true)
    int getId(@Param("keywords")String keywords);
}
