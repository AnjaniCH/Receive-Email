package com.mail.springbootimaplistener.repository;

import org.springframework.stereotype.Repository;

import com.mail.springbootimaplistener.entity.IncomingEmails;
import java.sql.Timestamp;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface IncomingEmailRepository extends JpaRepository<IncomingEmails, Long> {
    @Modifying
    @Query(value = "insert into IncomingEmails(sender,recipients,cc,subject,body,receivedTime) VALUES (:sender,:recipients,:cc,:subject,:body,:receivedTime)", nativeQuery = true)
    @Transactional
    void insertIncomingEmail(@Param("sender") String sender, @Param("recipients") String recipients, @Param("cc") String cc, @Param("subject") String subject,  @Param("body")String body, @Param("receivedTime")Timestamp receivedTime);
    
}
