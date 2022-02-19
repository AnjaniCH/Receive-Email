package com.mail.springbootimaplistener.repository;

import org.springframework.stereotype.Repository;

import com.mail.springbootimaplistener.entity.IncomingEmailAttachments;
import java.sql.Timestamp;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface IncomingEmailAttachmentRepository extends JpaRepository<IncomingEmailAttachments, Long> {
    @Modifying
    @Query(value = "insert into IncomingEmailAttachments(incomingEmailId,fileName) VALUES ((SELECT incomingEmailId FROM IncomingEmails WHERE receivedTime = :receivedTime ORDER BY incomingEmailId DESC LIMIT 1),:fileName)", nativeQuery = true)
    @Transactional
    void insertIncomingEmailAttachment(@Param("receivedTime") Timestamp receivedTime, @Param("fileName") String fileName);
    
}
