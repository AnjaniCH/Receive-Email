package com.mail.springbootimaplistener.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "IncomingEmails")
public class IncomingEmails {

    public IncomingEmails(String sender, String recipients, String cc, String subject, String body, Timestamp receivedTime) {
        super();
        this.sender = sender;
        this.recipients =  recipients;
        this.cc = cc;
        this.subject = subject;
        this.body = body;
        this.receivedTime = receivedTime;
    }

    public IncomingEmails() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incomingEmailId")
    private Long incomingEmailId;

    @Column(name = "sender")
    private String sender;
    
    @Column(name = "recipients")
    private String recipients;
    
    @Column(name = "cc")
    private String cc;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body")
    private String body;
    
    @Column(name = "receivedTime")
    private Timestamp receivedTime;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "incomingEmailId", referencedColumnName = "incomingEmailId")
    List<IncomingEmailAttachments> attachments = new ArrayList<>();

    public Long getIncomingEmailId() {
        return incomingEmailId;
    }

    public void setIncomingEmailId(Long incomingEmailId) {
        this.incomingEmailId = incomingEmailId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Timestamp getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Timestamp receivedTime) {
        this.receivedTime = receivedTime;
    }

    public List<IncomingEmailAttachments> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<IncomingEmailAttachments> attachments) {
        this.attachments = attachments;
    }
    
}
