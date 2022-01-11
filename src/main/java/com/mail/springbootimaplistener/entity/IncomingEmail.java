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
@Table(name = "incoming_email")
public class IncomingEmail implements Serializable {

    public IncomingEmail(String sender, String subject, String body, Timestamp received_date) {
        super();
        this.sender = sender;
        this.subject = subject;
        this.body = body;
        this.received_date = received_date;
    }

    public IncomingEmail() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender")
    private String sender;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body")
    private String body;
    
    @Column(name = "received_date")
    private Timestamp received_date;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "incoming_email_id", referencedColumnName = "id")
    List<IncomingEmailAttachment> attachments = new ArrayList<>();

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<IncomingEmailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<IncomingEmailAttachment> attachments) {
        this.attachments = attachments;
    }

    public Timestamp getReceived_date() {
        return received_date;
    }

    public void setReceived_date(Timestamp received_date) {
        this.received_date = received_date;
    }

    
    
    
}
