package com.mail.springbootimaplistener.entity;


import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DocumentTypes")
public class DocumentTypes {

    public DocumentTypes(String name, String fileNameFormat, String mandatoryStatus, String keywords) {
        super();
        this.name = name;
        this.fileNameFormat = fileNameFormat;
        this.mandatoryStatus = mandatoryStatus;
        this.keywords = keywords;
    }

    public DocumentTypes() {
        
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documentTypeId")
    private Long documentTypeId;
    
    @Column(name = "name")
    private String name;

    @Column(name = "fileNameFormat")
    private String fileNameFormat;
    
    @Column(name = "mandatoryStatus")
    private String mandatoryStatus;
    
    @Column(name = "keywords")
    private String keywords;

    public Long getDocumentTypeId() {
        return documentTypeId;
    }

    public void setDocumentTypeId(Long documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileNameFormat() {
        return fileNameFormat;
    }

    public void setFileNameFormat(String fileNameFormat) {
        this.fileNameFormat = fileNameFormat;
    }

    public String getMandatoryStatus() {
        return mandatoryStatus;
    }

    public void setMandatoryStatus(String mandatoryStatus) {
        this.mandatoryStatus = mandatoryStatus;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    
}
