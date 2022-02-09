package com.mail.springbootimaplistener.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "IncomingEmailAttachments")
public class IncomingEmailAttachments {

    public IncomingEmailAttachments(String fileName) {
        super();
        this.fileName = fileName;
    }

    public IncomingEmailAttachments() {
        
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachmentId")
    public Long attachmentId;
    
    @Column(name = "incomingEmailId")
    public Long incomingEmailId;

    @Column(name = "fileName")
    public String fileName;

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public Long getIncomingEmailId() {
        return incomingEmailId;
    }

    public void setIncomingEmailId(Long incomingEmailId) {
        this.incomingEmailId = incomingEmailId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
}
