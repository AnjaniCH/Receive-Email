package com.mail.springbootimaplistener.repository;

import org.springframework.stereotype.Repository;

import com.mail.springbootimaplistener.entity.IncomingEmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface IncomingEmailAttachmentRepository extends JpaRepository<IncomingEmailAttachment, Long> {

}
