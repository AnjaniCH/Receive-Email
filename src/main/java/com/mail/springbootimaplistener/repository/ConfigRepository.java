package com.mail.springbootimaplistener.repository;

import org.springframework.stereotype.Repository;

import com.mail.springbootimaplistener.entity.IncomingEmail;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface IncomingEmailRepository extends JpaRepository<IncomingEmail, Long> {

}
