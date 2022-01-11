package com.mail.springbootimaplistener.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "incoming_email_attachment")
public class IncomingEmailAttachment implements Serializable {

    public IncomingEmailAttachment(String file_name, String file_path) {
        super();
        this.file_name = file_name;
        this.file_path = file_path;
    }

    public IncomingEmailAttachment() {
        
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @Column(name = "incoming_email_id")
    public Long incoming_email_id;

    @Column(name = "file_name")
    public String file_name;
    
    @Column(name = "file_path")
    public String file_path;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIncoming_email_id() {
        return incoming_email_id;
    }

    public void setIncoming_email_id(Long incoming_email_id) {
        this.incoming_email_id = incoming_email_id;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }
    
    
}
