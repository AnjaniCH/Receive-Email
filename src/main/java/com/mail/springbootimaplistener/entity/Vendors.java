package com.mail.springbootimaplistener.entity;


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
@Table(name = "Vendors")
public class Vendors {

    public Vendors(String coupaVendorId, String psapVendorId, String legalEntity, String vendorName, String status, String registrationStatus, Date registrationDate, String location, String invited, String address, String city, String email, String category) {
        super();
        this.coupaVendorId = coupaVendorId;
        this.psapVendorId = psapVendorId;
        this.legalEntity = legalEntity;
        this.vendorName = vendorName;
        this.status = status;
        this.registrationStatus = registrationStatus;
        this.registrationDate = registrationDate;
        this.location = location;
        this.invited = invited;
        this.address = address;
        this.city = city;
        this.email = email;
        this.category = category;
    }

    public Vendors() {
        
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendorId")
    private String vendorId;
    
    @Column(name = "coupaVendorId")
    private String coupaVendorId;

    @Column(name = "psapVendorId")
    private String psapVendorId;
    
    @Column(name = "legalEntity")
    private String legalEntity;
    
    @Column(name = "vendorName")
    private String vendorName;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "registrationStatus")
    private String registrationStatus;
    
    @Column(name = "registrationDate")
    private Date registrationDate;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "invited")
    private String invited;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "category")
    private String category;

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getCoupaVendorId() {
        return coupaVendorId;
    }

    public void setCoupaVendorId(String coupaVendorId) {
        this.coupaVendorId = coupaVendorId;
    }

    public String getPsapVendorId() {
        return psapVendorId;
    }

    public void setPsapVendorId(String psapVendorId) {
        this.psapVendorId = psapVendorId;
    }

    public String getLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(String legalEntity) {
        this.legalEntity = legalEntity;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInvited() {
        return invited;
    }

    public void setInvited(String invited) {
        this.invited = invited;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
}
