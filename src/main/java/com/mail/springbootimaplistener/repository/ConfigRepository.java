package com.mail.springbootimaplistener.repository;

import org.springframework.stereotype.Repository;

import com.mail.springbootimaplistener.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ConfigRepository extends JpaRepository<Config, String> {

}
